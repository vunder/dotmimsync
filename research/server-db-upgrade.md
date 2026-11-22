# Server DB upgrade 0.9.0 -> 1.3.0: does client sync state survive?

Issue: #26. Sources: Dotmim.Sync source at `8569980` (1.3.0 line) and `eea490e1` (tag 0.9.1). Paths below are relative to the Dotmim.Sync repo.

## Short answer

Yes, client timestamps and scope ids stay valid, **if** the server tables are actually migrated. But the 1.3 auto-upgrade does **not** fire on a pure 0.9.0 server, so we have to run the migration ourselves (or replicate it).

## 1. The auto-upgrade does not trigger on a 0.9.0 server

- 0.9.x server keeps its state in `scope_info_server` and `scope_info_history` (`DbScopeType.Server` / `ServerHistory`, `Projects/Dotmim.Sync.SqlServer/Builders/SqlScopeBuilder.cs` lines ~192-210 at `eea490e1`). A plain server has no `scope_info` table.
- 1.3 `RemoteOrchestrator.NeedsToUpgradeAsync` (`Projects/Dotmim.Sync.Core/Orchestrators/Upgrade/RemoteOrchestrator.Upgrade.cs` L21-60) checks only `scope_info`: "no scopes info table. Just a new sync" -> `return false`.
- Callers gate on it: `SyncAgent.cs` L207-210, `WebServerAgent.cs` L1074-1083. `InternalUpgradeAsync` itself also reads `scope_info` and returns early if empty (L104-109).
- So, against a 0.9.0 server DB, `UpgradeOldestVersions` (the code written exactly for this case) is never reached. 1.3 would create a fresh empty `scope_info`, leave `scope_info_server` orphaned and try to provision with `overwrite=false`, which keeps the old 0.9 stored procedures and triggers. That is the risky path.

## 2. What the real migration (`UpgradeOldestVersions`, L220-436) does

Picked for version 0.x where `Minor <= 9 && Build <= 5` (L121-126), so 0.9.0 qualifies.

1. Drops `scope_info_history` (L244-250). The server's per-client history is thrown away.
2. Creates new `scope_info` and `scope_info_client` (L253-282).
3. Copies each `scope_info_server` row into `scope_info`: setup, schema, name (empty -> `DefaultScope`), **`LastCleanupTimestamp` kept** (L289-324).
4. Drops `scope_info_server` (L326-333).
5. Deprovisions only `StoredProcedures | Triggers` and provisions them again (L335-420). **Tracking tables and data tables are not touched.**

## 3. Are client timestamps still valid?

Yes.
- Tracking tables keep their rows. Column shape is the same in both versions (`update_scope_id`, `timestamp` rowversion, `timestamp_bigint` computed, `sync_row_is_tombstone`; `SqlBuilderTrackingTable.cs` in both trees). A SQL Server rowversion only grows, so a client's last server timestamp still means "changes after this point".
- The only server-side check against it is outdated detection: `isOutdated = cScopeInfoClient.LastServerSyncTimestamp < sScopeInfo.LastCleanupTimestamp` (`BaseOrchestrator.cs` L303). The migration keeps `LastCleanupTimestamp`, so a client is only outdated if it would have been outdated already.
- In 1.x the client sends its own timestamp with `ScopeInfoClient`. The dropped `scope_info_history` is not needed to serve changes. `scope_info_client` rows on the server get recreated on the next sync.

## 4. Are client scope ids still valid?

Yes, as long as the id is kept. `update_scope_id` in the tracking tables still holds the old client GUIDs. The server uses it to skip sending a client's own changes back to it.
- **Old 0.9.x clients via the shim**: map `OldScopeInfo.sync_scope_id` -> `ScopeInfoClient.Id` and `last_server_sync_timestamp` -> `LastServerSyncTimestamp`. Same GUID means no echo, and no reinit.
- **New clients after the SQLite migration**: the client migration must keep `sync_scope_id` and the last server timestamp. If it generates a new id, the client gets its own pending uploads echoed back once (conflicts/duplicate apply), but no data is lost.

## Recommendation

- Do not count on the agent auto-upgrade. Run the `UpgradeOldestVersions` steps on the server DB ourselves, in one transaction, before any 1.3 client connects. It is `internal`, so either call it through a subclass/reflection or port the steps as SQL.
- Back up `scope_info_history` first (it is dropped) in case we need old per-client data.
- No client needs to reinitialize if the steps above hold.

## Not checked

- Filtered scopes / multiple scopes beyond the default.
- Behaviour when 1.3 runs against an un-migrated 0.9 server was read from code, not tested.
