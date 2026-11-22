# Client wire protocol diff: Dotmim.Sync 0.9.1 -> 1.3.0

Answers #20 (map #17). Scope: HTTP client <-> `Dotmim.Sync.Web.Server`, JSON serializer only.

## Sources

All claims come from the C# repo `Mimetis/Dotmim.Sync`:

- **0.9.1** = commit `eea490e1` ("tag 0.9.1", 2021-11-14).
- **1.3.0** = commit `bcc5148c` ("version 1.3.0", 2024-09-25). Line references are to `HEAD` `f4378baa` (2025-08-28). `git diff bcc5148c HEAD` over Web.Client, Web.Server, Core/Serialization, Set, Batch, Scopes and SyncContext shows only refactors and a netstandard `DateTime` fallback, so the wire is the same as 1.3.0.
- The Kotlin side is `dotmimsync-core/.../web/client/` (`DotmimServiceImpl.kt`, `HttpStep.kt`).

Abbreviations: `WRO` = `Projects/Dotmim.Sync.Web.Client/Orchestrators/WebRemoteOrchestrator*.cs`, `WSA` = `Projects/Dotmim.Sync.Web.Server/WebServerAgent.cs`, `HM` = `Projects/Dotmim.Sync.Web.Client/HttpMessage.cs`.

## TL;DR: what the Kotlin client must change

1. **Upload must send `scopeclient` + `clst`.** `SendChangesInProgress` / `GetEstimatedChangesCount` requests need `scopeclient` (a `ScopeInfoClient`: `id,n,h,lst,lsst,in,p`) and `clst` (client last sync timestamp). The old `scope` field is still declared (`OldScopeInfo`, "backward compat") but **nothing on the server reads it**. `RemoteOrchestrator.ApplyChanges.cs` calls `cScopeInfoClient.Id` unguarded, so a 0.9.x request with only `scope` gets a NullReference on the server. **The current Kotlin client cannot sync against an unmodified 1.3.0 server.** This affects the rollout requirement in #17 (see #23).
2. **One table per upload message.** Each `SendChangesInProgress` carries a `ContainerSet` with exactly one `ContainerTable`. The server reads only `Changes.Tables[0]` (`WSA` `ApplyThenGetChangesAsync2`). The client sends one message per `BatchPartInfo` with `bi`, `bc`, `islb` (`WRO.ApplyChanges.cs` L74-110). If there is nothing to upload, it sends one empty message.
3. **New headers:** `dotmim-sync-scope-id` (= `SyncContext.ClientId`) and `dotmim-sync-version` (client DMS version), plus optional `dotmim-sync-identifier` (`WRO.cs` `SendAsync`). `dotmim-sync-hash` (base64 SHA-256 of the body) is now sent on **every** request. The server checks it only when present (`WSA` `HandleRequestAsync`).
4. **Scope model split.** The old `ScopeInfo` {n,id,in,v,lst,lsst} became server-side `ScopeInfo` {n, **sch** (SyncSet), s (SyncSetup), v, lst=LastCleanupTimestamp} and client-side `ScopeInfoClient` {id,n,h,lst,lsst,in,p}. `EnsureScopes` returns `serverscope` as the new `ScopeInfo`, which now **includes the schema**. The old `ServerScopeInfo` {n,s,v,lst} is a subset, so old parsers still read it.
5. **New steps** (enum values append at the end, existing numbers unchanged): `GetRemoteClientTimestamp=12`, `GetOperation=13`, `EndSession=14` (`HttpStep.cs`). The 1.3.0 client flow is: EnsureScopes -> (GetOperation) -> (GetSummary for snapshot) -> SendChangesInProgress* -> GetMoreChanges* (all but the last in parallel, then the last) -> SendEndDownloadChanges -> EndSession. `EnsureSchema=1` is still served, as "pre v 0.9.6" compat. The 1.3.0 client no longer uses `EnsureSchema` or `GetSnapshot`.
6. **`SyncStage` renumbered.** `SchemaReading` (old 7) and `MetadataCleaning` were removed, so Provisioning=7, Deprovisioning=8, ChangesSelecting=9, ChangesApplying=10, Migrating=11. This is used in `sc.stage` and `WebSyncException`. The Kotlin `SyncStage` enum still has the old numbers.
7. **Batch format.** `BatchPartInfo` lost `tables` (BatchPartTableInfo[]) and inline `changes`, and gained flat `tn` (table name), `ts` (schema name) and `state` (SyncRowState). So each part is one table. `BatchInfo` lost `schema` (SanitizedSchema). `ContainerTable` gained optional `c` (columns: `n`,`t`,`p`), and rows `r` moved to Order 4 (same name). `BatchPartTableInfo` no longer exists.
8. **`SyncContext.csid`** is now nullable and means `ClientId` (was `ClientScopeId`). It also feeds the new scope-id header.

## Headers

| Header | 0.9.1 client | 1.3.0 client | 1.3.0 server |
|---|---|---|---|
| dotmim-sync-session-id | yes | yes | required |
| dotmim-sync-scope-name | yes | yes | required, must equal server scope |
| dotmim-sync-step | yes (int) | yes (int) | required |
| dotmim-sync-serialization-format | `{"f":"json","s":N}` (Newtonsoft anon obj) | `SerializerInfo` via JsonObjectSerializer (camelCase `serializerKey`,`clientBatchSize`) | parsed with `JsonSerializer.Deserialize<SerializerInfo>` default options (PascalCase, case-sensitive). An unknown key falls back to JSON (`??=`). Batch size is read from it. |
| dotmim-sync-converter | optional | optional | must match a configured converter |
| dotmim-sync-hash | yes | yes | verified if present |
| dotmim-sync-scope-id | - | yes | read (`GetClientScopeId`) |
| dotmim-sync-version | - | yes | read, informational |
| dotmim-sync-identifier | - | optional | - |
| dotmim-sync-bi / -bc / -islb / -bi-tables / -rows-count / -context | used by 0.9.1 (old batch headers) | removed | - |
| dotmim-sync-error (response) | yes | yes | same, body = `WebSyncException` |

Serialization-format caveat: going by the source, both `{"f":"json","s":0}` (Kotlin today) and the 1.3.0 client's camelCase form deserialize to `SerializerKey=null`/`ClientBatchSize=0` on the server. So the server falls back to JSON with batch size 0 either way. **Check this in the integration harness (#23/#24)** rather than relying on it. Sending PascalCase `{"SerializerKey":"json","ClientBatchSize":N}` is the safe choice.

## Message fields (DataMember names)

| Message | 0.9.1 | 1.3.0 |
|---|---|---|
| EnsureScopesRequest | sc | sc |
| EnsureScopesResponse | sc, serverscope(ServerScopeInfo) | sc, serverscope(ScopeInfo incl. `sch`) |
| EnsureSchemaResponse | sc, schema, ssi | sc, schema, ssi (compat only) |
| SendChangesRequest | sc, scope, bi, bc, islb, changes | sc, **scopeclient**, bi, bc, islb, changes, **clst**, scope (ignored) |
| SendChangesResponse | ss, sc, bi, bc, islb, rct, changes, scs, cca, policy | unchanged |
| SummaryResponse | sc, bi, rct, step, changes, scs, cca, crp | unchanged names; `bi.parts` now flat per-table parts |
| GetMoreChangesRequest | sc, bireq | unchanged |
| RemoteTimestampRequest/Response | - | sc / sc, rct |
| OperationRequest/Response | - | sc, scope(ScopeInfo), scopeclient / sc, so (SyncOperation: Normal 0, Reinitialize 1, ReinitializeWithUpload 2, DropAllAndSync 4, DropAllAndExit 8, DeprovisionAndSync 16, AbortSync 32) |
| EndSessionRequest/Response | - | sc, st, ct, cas, cac, scac, ccs, scs, exc / sc |

## Serializer and type changes

- Newtonsoft became System.Text.Json with a `DataContractResolver` (still DataMember names, ordered by `Order`), camelCase naming policy, `PropertyNameCaseInsensitive`, `ArrayJsonConverter` + `ObjectToInferredTypesConverter` for row values (`Core/Serialization/JsonObjectSerializer.cs`, `DataContractResolver.cs`).
- Many `EmitDefaultValue=false` flags were dropped (SyncColumn flags, TableChangesSelected d/u, BatchInfo ts, SyncContext pp, WebSyncException n). The server now **emits** defaults. The Kotlin parser must tolerate the extra keys (`ignoreUnknownKeys`), and every default must decode.
- `SyncColumn`: `seed`/`step` changed from int to **long**. `PrecisionSpecified`/`ScaleSpecified` were renamed in C# but keep the same wire names `ps`/`ss`. New optional `ext1` (ExtraProperty1).
- `SyncSet` t/r/f Order changed (8,9,10 -> 1,2,3). Names are unchanged, so JSON is unaffected.
- `SyncTable` dropped `sd` (SyncDirection) from the wire.
- `TableChangesApplied.st` changed from `DataRowState` to `SyncRowState` (None=2, Deleted=8, Modified=16, RetryDeletedOnNextSync=32, RetryModifiedOnNextSync=64, ApplyDeletedFailed=128, ApplyModifiedFailed=256). Values 2/8/16 are the same as DataRowState Unchanged/Deleted/Modified. The first element of each row is this state.
- `WebSyncException` dropped `s` (Side). `SyncStage` is renumbered (TL;DR 6).
- Unchanged: SyncType, SyncWay, ConflictResolutionPolicy, SyncDirection values.

## Snapshots

0.9.1 and 1.3.0 both start with `GetSummary` (request = SendChangesRequest, response = SummaryResponse). In 1.3.0 the parts are then fetched with `GetMoreChanges` (`WRO.Snapshots.cs` -> `DownladBatchInfoAsync`). The request is built with `scopeclient = null` (`new HttpMessageSendChangesRequest(context, null)`). The parts are flat per-table `BatchPartInfo`, as in TL;DR 7.

## Open (for other tickets)

- 0.9.x clients against a 1.3.0 server fail at upload (TL;DR 1). Keeping old Kotlin clients working during rollout needs either a server-side shim (map `scope`+context to a `ScopeInfoClient`) or a staged rollout. That decision belongs to #17/#23, not here.
- The serialization-format header parsing (above) should be verified empirically.
