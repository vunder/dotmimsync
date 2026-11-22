# Client SQLite metadata: 0.9.1 → 1.3.0 and how C# upgrades it

Answers #21 (map #17). Sources: C# repo at commit f4378baa (version 1.3.0 per Directory.Build.props, no 1.3.0 tag) and this Kotlin port (0.9.1).

## Answer

- **Changed:** only the scope tables. The single 0.9.1 `scope_info` (one row per scope, holding client state too) is split into `scope_info` (setup/schema/version) and a new `scope_info_client` (client id, parameter hash, timestamps).
- **Unchanged:** tracking tables keep the same columns and index, and triggers look the same apart from a small difference in the update trigger. User tables are not touched.
- **Upgrade:** on the first sync, `SyncAgent` calls `NeedsToUpgradeAsync` and then `InternalUpgradeAsync`. For a version below 0.9.6, which includes 0.9.1, this runs `UpgdrateFromNoWhereTo098Async`. In one transaction it moves the scope rows into the new tables, drops the triggers and recreates them. It never drops or rewrites the tracking tables, so rows not yet synced survive.
- **Limit:** if any old scope has filters (`fils` in its setup JSON), the automatic upgrade throws, and the client must be migrated by hand with `ManualUpgradeWithFiltersParameterAsync`.

## 1. scope_info

0.9.1 (Kotlin `dotmimsync-core/.../sqlite/SqliteScopeBuilder.kt` `createScopeInfoTable`):
`sync_scope_id blob PK, sync_scope_name, sync_scope_schema, sync_scope_setup, sync_scope_version, scope_last_server_sync_timestamp, scope_last_sync_timestamp, scope_last_sync_duration, scope_last_sync`

1.3.0 (Dotmim.Sync@f4378baa `Projects/Dotmim.Sync.Sqlite/Builders/SqliteScopeBuilder.cs` ~L135-164):
- `scope_info`: `sync_scope_name text PK, sync_scope_schema, sync_scope_setup, sync_scope_version, sync_scope_last_clean_timestamp, sync_scope_properties`
- `scope_info_client`: `sync_scope_id blob, sync_scope_name, sync_scope_hash, sync_scope_parameters, scope_last_sync_timestamp, scope_last_server_sync_timestamp, scope_last_sync_duration, scope_last_sync, sync_scope_errors, sync_scope_properties`, with the primary key `(sync_scope_id, sync_scope_name, sync_scope_hash)`

## 2. Tracking tables and triggers

- Tracking columns are the same in both: `update_scope_id, timestamp, sync_row_is_tombstone, last_change_datetime`, plus the `<tracking>_timestamp_index` index (Dotmim.Sync@f4378baa `Projects/Dotmim.Sync.Sqlite/Builders/SqliteTableBuilder.cs` L192-216; Kotlin `SqliteTableBuilder.kt` ~L285-305).
- One type difference: C# declares `update_scope_id [text]`, and has done so since SQLite support was added (git log -S, commit f95a4567, 2020). The Kotlin port declares `[blob]`. This comes from the port, not from a version change. The upgrade does not rebuild tracking tables, so an upgraded port DB would keep `blob`. Check how the id is bound and compared before relying on this.
- Triggers: insert, update and delete triggers have the same shape. In 1.3.0 the update trigger updates the tracking row and then does `INSERT OR IGNORE`, with no "AND (column changed …)" condition on that UPDATE. The Kotlin 0.9.1 trigger has that condition. The upgrade drops and recreates every trigger anyway.

## 3. Detection (Dotmim.Sync@f4378baa `Projects/Dotmim.Sync.Core/Orchestrators/Upgrade/LocalOrchestrator.Upgrade.cs` `NeedsToUpgradeAsync` ~L323)

An upgrade is needed if `scope_info` exists **and** one of these holds:
- its columns do not match the expected schema (`IsScopeInfoSchemaValidAsync`), which is true for 0.9.1;
- `sync_scope_version` in the first row is not `SyncVersion.Current`;
- the `scope_info_client` columns do not match.

`SyncAgent.cs` ~L200-212 runs this once per agent, for the local and the remote side, before the sync.

## 4. 0.9.1 path: `UpgdrateFromNoWhereTo098Async` (Dotmim.Sync@f4378baa `Projects/Dotmim.Sync.Core/Orchestrators/Upgrade/LocalOrchestrator.Upgrade.cs` ~L512-715)

`InternalUpgradeAsync` (~L393) chooses the path:
- 0.9.6 and later: auto path;
- 0.9.0-0.9.5 or 0.8.x and earlier: `UpgdrateFromNoWhereTo098Async`;
- anything else: deprovision and reprovision.

Steps of `UpgdrateFromNoWhereTo098Async`, all in one transaction (`SyncMode.WithTransaction`, `CommitAsync` at the end):
0. Read each old row's `sync_scope_setup` JSON. If it has `fils` (filters), throw and send the user to the manual upgrade.
1. `MigrateScopeInfoTableAsync` and `MigrateScopeInfoClientTableAsync` (`BaseOrchestrator.Upgrade.cs` L79-200): rename `scope_info` to `tmpscope_info`, rename `scope_info_client` to `tmpscope_info_client` if it exists, create the new tables, and return the old rows.
2. For each old row, save a `ScopeInfo` with the same name, setup and schema, and Version = Current. Also save a `ScopeInfoClient` with:
   - Id = the first row's `sync_scope_id`, reused for every scope;
   - Hash = `SyncParameters.DefaultScopeHash`;
   - `scope_last_sync_timestamp`, `scope_last_server_sync_timestamp`, `scope_last_sync_duration` and `scope_last_sync` copied over.
3. Drop the tmp tables.
4. Deprovision **only** `StoredProcedures | Triggers` for each scope, plus a fake "DMS Generated" scope that covers every user table not ending in `_tracking`, so that orphan triggers are also removed.
5. Reprovision triggers with the 1.3.0 builders.

Why rows not yet synced survive:
- Tracking tables and their rows are never dropped (step 4 leaves out `SyncProvision.TrackingTable`).
- `scope_last_sync_timestamp` is kept, so the next `GetChanges` still selects rows where `timestamp > last_sync_timestamp` and `update_scope_id IS NULL`.
- The client id is kept, so the server still knows this client.
- The whole upgrade is one transaction, so a failure rolls back.

## 5. Manual path (filters)

`ManualUpgradeWithFiltersParameterAsync(IList<ScopeInfoClientUpgrade> entries, …, evaluateOnly)` (Dotmim.Sync@f4378baa `Projects/Dotmim.Sync.Core/Orchestrators/Upgrade/LocalOrchestrator.Upgrade.cs` L49+) runs the same steps, but the caller gives the parameters and hash for each scope.

## Implications for the port
- Run the same steps on the first sync after upgrading: rename to tmp, create the 2 tables, copy rows, drop tmp, then drop and recreate triggers only.
- Fail loudly if any scope has filters.
- Decide what to do about the `update_scope_id` blob/text difference.
