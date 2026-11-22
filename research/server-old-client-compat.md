# Does an unmodified Dotmim.Sync 1.3.0 Web.Server accept 0.9.x clients?

Ticket: #19 (map #17). Sources: C# repo `Mimetis/Dotmim.Sync` at local HEAD (Directory.Build.props = 1.3.0), compared with commit `eea490e1` ("tag 0.9.1"). Kotlin client: `dotmimsync-core` on `origin/master`.

## Answer

**No, not unmodified.** The handshake works, but the first upload/download round trip (`SendChangesInProgress`) fails for every 0.9.x client. The server-side fix is small and can be done in the server's own code (an interceptor or a `WebServerAgent` subclass), with no change to the Dotmim.Sync library.

## What still works (wire-compatible)

- **Headers.** The names are the same: `dotmim-sync-session-id`, `-scope-name`, `-step`, `-serialization-format`, `-converter`, `-hash`. `dotmim-sync-version` is optional on the server (`WebServerAgent.cs` L267-280). The Kotlin client never sends it (`DotmimServiceImpl.kt`).
- **`HttpStep` numbering is the same.** `git diff v0.9.6 HEAD -- HttpStep.cs` only adds `EndSession` at the end.
- **Legacy `EnsureSchema` is still handled.** It is marked `// pre v 0.9.6` and returns `HttpMessageEnsureSchemaResponse` with the same keys `sc`/`schema`/`ssi` (`WebServerAgent.cs` L354-357, L414-416; `HttpMessage.cs` L195-226). `EnsureScopes`, `GetSnapshot`, `GetSummary` and `SendEndDownloadChanges` are all still dispatched.
- **Most DTO key names did not change** (`DataMember` names in `HttpMessage.cs`, `SyncContext`, `SyncSet`, `SyncTable`, `SyncColumn`, `SyncFilter`, `SyncSetup`, `ContainerSet`). Old ServerScopeInfo `{n,s,v,lst}` is a subset of the new ScopeInfo `{n,sch,s,v,lst}`.
- **A session-less client is fine.** The server skips the hash check when no hash is sent, and the session is created on `EnsureSchema`/`EnsureScopes` (L301-327). An old client never calls `EndSession`, which does no harm.

## What breaks

1. **Upload request: the scope moved (blocking).**
   - In 0.9.x, `HttpMessageSendChangesRequest` carries the client scope as `"scope"` (ScopeInfo `{n,id,in,v,lst,lsst}`). The Kotlin `HttpMessageSendChangesRequest.kt` sends the same.
   - In 1.3.0, the server reads `"scopeclient"` (ScopeInfoClient `{id,n,h,lst,lsst,in,p}`) plus `"clst"`. Both are `IsRequired=false`, and the old `"scope"` is deserialized into `OldScopeInfo` "to preserve backward compat" (`HttpMessage.cs` L160-189).
   - **No code reads `OldScopeInfo`.** `grep -rn OldScopeInfo Projects` finds only its declaration.
   - So `ScopeInfoClient` is null when `InternalApplyThenGetChangesAsync(httpMessage.ScopeInfoClient, …)` runs (`WebServerAgent.cs` L929-933). That method dereferences it straight away (`cScopeInfoClient.IsNewScope`, `.LastServerSyncTimestamp`, `.Id`; `RemoteOrchestrator.ApplyChanges.cs` L90, L222-257). The result is a NullReferenceException, returned as a `dotmim-sync-error` response.
   - `ClientLastSyncTimestamp` also defaults to 0.
2. **Response shape drift (needs testing with the 0.9.x Kotlin client).**
   - `BatchPartInfo` no longer has `"tables"`. It was required in 0.9.1; it is now `"tn"`/`"ts"`/`"state"`. The Kotlin `BatchPartInfo.tables` is nullable, so it deserializes, but any code that reads `tables` sees null.
   - `ContainerTable` gains `"c"` (columns).
   - `ScopeInfo` gains `"sch"`.
   - `BatchInfo` drops `"schema"`.
   - These are safe only if the Kotlin HTTP JSON decoder ignores unknown keys. `ignoreUnknownKeys` is set for SQLite and batch files, but I did not find it in the web client code. Check this before relying on it.
3. **Server database.** Upgrading the server to 1.x also migrates its scope tables (server concern, map #17 "Not yet specified"). The server then has to keep per-client `scope_info_client` rows, which 0.9.x clients never send.

## Options

| Option | Effort | Notes |
|---|---|---|
| **A. Server shim (recommended).** In `HttpGettingRequestArgs` (raised after deserialize, before dispatch, L406-407), or in a `WebServerAgent` subclass: if `ScopeInfoClient == null && OldScopeInfo != null`, build `ScopeInfoClient{Id=id, Name=n, IsNewScope=in, LastSyncTimestamp=lst, LastServerSyncTimestamp=lsst, Parameters=SyncContext.pp, Hash=<computed from parameters>}` and set `ClientLastSyncTimestamp=lst`. | Small, server-only | Old clients can be detected by the missing `dotmim-sync-version` header (added to C# clients in 0.9.6, commit `4d72d1ab`). Confirm the hash rule against `ScopeInfoClient` in 1.3.0. Then run an integration test with a real 0.9.1 Kotlin client (check item 2). |
| **B. Side-by-side servers** (0.9.0 on the old URL, 1.3.0 on a new one, same DB) | Medium, risky | 0.9 and 1.x provision different scope tables and tracking metadata on the same DB. Not verified as safe, so do not assume it. |
| **C. Legacy endpoint in the same app** (a separate controller with a 0.9 package) | Not feasible | You cannot load two versions of the same assembly side by side in one process without isolating them. |
| **D. Force-upgrade clients** (min-version gate) | Product decision | This goes against the map goal "old 0.9.x clients keep working during rollout". |

## Not verified

- No live run against a real 1.3.0 server. These findings come from reading the source.
- Whether the Kotlin 0.9.x HTTP decoder tolerates the new unknown keys.
- The exact `ScopeInfoClient.Hash` derivation for option A.
