# Scopes, filters/parameters and conflict resolution: client-side changes 0.9.1 -> 1.3.0

Ticket: #22 (map #17). Sources: the C# repo at the 1.3.0 working tree (`Directory.Build.props` `<Version>1.3.0</Version>`, set in commit bcc5148c, "version 1.3.0", 2024-09-25), and the Kotlin port at origin/master (it is our 0.9.1 baseline). C# paths are relative to `Projects/`. Kotlin paths are relative to `dotmimsync-core/src/commonMain/kotlin/com/mimetis/dotmim/sync/`.

## Answer

1. **Scopes:** the single client `scope_info` row (0.9.1) is split into two tables. `scope_info` holds one row per scope name (schema, setup, version). `scope_info_client` holds one row per (client id, scope name, parameter hash) with the timestamps. This is the change that matters most: one scope with different parameter values now has separate sync state per parameter set.
2. **Filters/parameters:** the wire names of `SyncFilter` and `SyncParameter` did not change. Parameters are now part of the client scope's identity (a hash) and are saved on the client (`sync_scope_parameters`). In 1.3.0 `SynchronizeAsync` takes scopeName, setup, syncType and parameters per call.
3. **Conflicts:** `ConflictResolutionPolicy` still has only ServerWins and ClientWins, and the client still uses the reverse of the server's policy. In `ConflictResolution`, `Rollback` was renamed to `Throw`. There is a new row-level error policy, `ErrorResolution` / `ApplyChangesErrorOccured`, with RetryOnNextSync (failed rows are kept in `sync_scope_errors`).

## 1. Scope model

| | 0.9.1 (Kotlin today) | 1.3.0 |
|---|---|---|
| Client scope object | `ScopeInfo` {n, id, in, v, lst, lsst, schema, setup} (`scopes/ScopeInfo.kt`) | `ScopeInfo` {n, sch, s, v, lst=LastCleanupTimestamp} plus `ScopeInfoClient` {id, n, h, lst, lsst, in, p} (`Dotmim.Sync.Core/Scopes/ScopeInfo.cs`, `ScopeInfoClient.cs`) |
| SQLite tables | `scope_info` with PK sync_scope_id (`sqlite/SqliteScopeBuilder.kt:36`) | `scope_info` with PK sync_scope_name; `scope_info_client` with PK (sync_scope_id, sync_scope_name, sync_scope_hash) and the columns sync_scope_parameters, sync_scope_errors, sync_scope_properties (`Dotmim.Sync.Sqlite/Builders/SqliteScopeBuilder.cs:134-164`) |
| Server scope | `ServerScopeInfo` {n, s, v, lst} | merged into `ScopeInfo` (the same class on both sides) |

- The JSON key `lst` means different things: client LastSyncTimestamp in 0.9.1 `ScopeInfo` and in 1.3.0 `ScopeInfoClient`, but LastCleanupTimestamp in 1.3.0 `ScopeInfo`. Serializers must not mix the two classes up.
- Changing the setup of a scope uses `ScopeInfoClient.ShadowScope`, which copies the timestamps from the old client scope (`ScopeInfoClient.cs`).
- C# has an upgrade path from the old single table: `Dotmim.Sync.Core/Orchestrators/Upgrade/LocalOrchestrator.Upgrade.cs` (~l.170-215). It renames to a temporary table, writes the new scope_info and scope_info_client rows (Hash = parameters?.GetHash() ?? DefaultScopeHash, keeping id, lst, lsst, last_sync and duration), then drops the temporary table. The Kotlin in-place migration should copy this logic.
- Kotlin API gap: `SyncAgent.kt:22` hardcodes `scopeName = DefaultScopeName`, and `parameters` is one property on the agent. In 1.3.0 both are per call: `SynchronizeAsync(string scopeName, SyncSetup setup, SyncType syncType, SyncParameters parameters, ...)` (`Dotmim.Sync.Core/SyncAgent.cs:141`). Multiple scopes in production need scope name and parameters per call.

## 2. Filters and parameters

- `SyncFilter` wire keys are the same: t, s, p, w, j, cw (`Dotmim.Sync.Core/Set/SyncFilter.cs` matches `set/SyncFilter.kt`). `SyncParameter` keys are the same: pn, v.
- New: the scope hash. See `Dotmim.Sync.Core/Parameter/SyncParameters.cs:97-120`:
  - `GetHash()` = Base64(SHA256(UTF8(concat over parameters ordered by Name of "{Name}.{Value}"))).
  - `DefaultScopeHash` = Base64(SHA256(UTF8("DefaultScope"))). This is used when there are no parameters (`BaseOrchestrator.ScopeInfoClients.cs:127`).
  - Kotlin must produce exactly the same hash. Name ordering uses .NET `OrderBy`, which is culture-sensitive for strings, and `Value` goes through .NET `ToString()`. Test this against C# output with mixed-case names, numbers, Guids and dates.
- `SyncContext` gets its hash from `Parameters.GetHash()` (`SyncContext.cs:108`).
- The upgrade code throws if the given parameters are not in the setup filters (`LocalOrchestrator.Upgrade.cs`, about l.168).

## 3. Conflict resolution

- `ConflictResolutionPolicy` {ServerWins, ClientWins} is unchanged. The default is still ServerWins (`SyncOptions.cs:120`).
- In HTTP mode the server's policy wins. The server sends it in the summary/apply responses (`Dotmim.Sync.Web.Client/HttpMessage.cs:93, 453`; `Web.Server/WebServerAgent.cs:689, 781`). The client applies the reverse of it (`SyncAgent.cs:387-403`). This is the same as 0.9.1 (`orchestrators/BaseOrchestrator.kt:1534`).
- `ConflictResolution` in 1.3.0 is {ServerWins, ClientWins, MergeRow, Throw}. Kotlin has `Rollback` where 1.3.0 has `Throw` (`enumerations/ConflictResolution.kt` vs `Enumerations/ConflictResolution.cs`; handled in `BaseOrchestrator.Conflicts.cs:317`). `RollbackException` still exists.
- The hook is `OnApplyChangesConflictOccured` (`SyncAgent.cs:119-130`). It works like the Kotlin conflict interceptor (MergeRow uses the final row).
- **New in 1.x:** `SyncOptions.ErrorResolutionPolicy` (`SyncOptions.cs:85`), `ErrorResolution` {Throw, ContinueOnError, RetryOneMoreTimeAndThrowOnError, RetryOneMoreTimeAndContinueOnError, RetryOnNextSync, Resolved} and `ErrorAction` (`Enumerations/ErrorResolution.cs`). These are handled per failed row in `Orchestrators/BaseOrchestrator.Errors.cs` through `ApplyChangesErrorOccuredArgs`. This applies to errors that are not conflicts (constraint failures and similar). Rows marked RetryOnNextSync are kept and applied again on the next sync. `sync_scope_errors` in scope_info_client stores them. The default, Throw, behaves like 0.9.1, so this only has to be ported if we want that behaviour. It is not a wire change.

## Implications for the port

- Required: the two-table scope model and an in-place migration based on C# Upgrade, a parameter hash identical to C#, scope name and parameters per call, Rollback renamed to Throw.
- Optional: ErrorResolution (Throw is the default and equals today's behaviour).
- No change: filter definitions on the wire, and the server-authoritative conflict policy.
