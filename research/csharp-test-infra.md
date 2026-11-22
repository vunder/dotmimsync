# C# Dotmim.Sync 1.3.0 test infrastructure and reuse for a Kotlin harness

Question: issue #25 (blocks #23). Source: local clone of Mimetis/Dotmim.Sync at commit `8569980` ("split pipelines"). All paths below are relative to that repo root.

## 1. Test structure

- One xUnit project: `Tests/Dotmim.Sync.Tests/Dotmim.Sync.Tests.csproj`, multi-targeted net6 / net8 / netcoreapp3.1; xunit 2.8.1, EF Core (SqlServer, Sqlite, Pomelo MySql, Npgsql) per TFM.
- Folders: `UnitTests/` (no DB), `StandAlone/` (serializers), `TcpTests/` (in-process `RemoteOrchestrator`: `TcpTests`, `TcpFilterTests`, `TcpConflictsTests`, scenarios), `HttpTests/HttpTests.cs` (2582 lines, client over HTTP to an in-process Kestrel server).
- Pattern: abstract base test classes; `Setup.cs` holds the concrete per-provider subclasses (e.g. `SqlServerHttpTests : HttpTests`, `SqlServerChangeTrackingHttpFilterTests`, `PostgresConflictTests`, `MariaDBHttpTests`), each overriding `ServerProviderType` and `GetClientProviders()` (Sqlite + Sql clients).
- `Fixtures/DatabaseServerFixture.cs` is only a stopwatch; real work is in `Misc/DatabaseTest.cs` (`CreateDatabasesAsync`, line ~197) and `Misc/HelperDatabase.cs` (random DB names, `CreateDatabaseAsync`, connection strings; hard-coded password `Password12!` when `AZUREDEV=true`).
- Schema + seed data: EF `Models/AdventureWorksContext.cs` (AdventureWorks-LT subset plus `Posts`, `Tags`, `PostTag`, `PriceList*`, `Log`, schema-qualified tables), seeding in `OnSeeding` (line ~823). Also raw scripts `CreateAdventureWorks*.sql` at repo root.
- `Core/KestrelTestServer.cs`: builds an `IWebHost` with `UseKestrel()` on a random port; `AddSyncServer(provider, setup, options, webServerOptions, scopeName, identifier)` wraps `services.AddSyncServer(...)`; `Run()` returns the service URI. `HttpTests` ctor: `Kestrel.AddSyncServer(serverProvider, setup, new SyncOptions{DisableConstraintsOnApplyChanges=true})`.
- HTTP coverage relevant to us: RowsCount, MultiScopes (scopes `v1`/`v2` on identifier `db1`, default scope on `db2`), insert/update/delete both sides, null updates, byte arrays, Reinitialize(+WithUpload), UploadOnly, DownloadOnly, Snapshots (`RemoteOrchestrator.CreateSnapshotAsync(setup)` with `SnapshotsDirectory`, `BatchSize=3000`), SnapshotsThenReinitialize, converter errors, additional HTTP headers. Conflicts and filters are TCP-only test classes (`TcpConflictsTests`, `TcpFilterTests`) but the same setup objects work over HTTP.
- Each test run creates fresh random-named databases; server and client are the same process.

## 2. How CI runs them

- No GitHub Actions and no Dockerfile/compose in the repo (`git ls-files` shows none: .github/FUNDING.yml). CI is Azure DevOps: `azure-pipelines-mysql.yml`, `azure-pipelines-nuget.yml`, `pipelines/azure-pipelines-sql.yml`, all instantiating `pipelines/azure-pipelines-template.yml`.
- Template job (ubuntu-latest): NuGet cache, `UseDotNet` 3.x/6.x/8.x, then one `docker run` per `docker` parameter:
  - `mcr.microsoft.com/mssql/server` with `ACCEPT_EULA=Y SA_PASSWORD=Password12!` on 1433
  - `mysql` on 3307, `mariadb:latest` on 3308, `postgres:latest` on 5432 (same password)
- Then `dotnet test Tests/Dotmim.Sync.Tests/Dotmim.Sync.Tests.csproj --filter <ClassName> -f <tfm>` with env `AZUREDEV=true` (switches HelperDatabase from localdb/`appsettings.json` to the container credentials). One job per test class, e.g. `Dotmim.Sync.Tests.SqlServerHttp`, `SqlServerChangeTrackingHttp`, `SqlServerConflictTests`, `SqlServerTcpFilterTests`, `MariaDBHttpTests`.
- Locally, `appsettings.json` defaults: SQL Server `(localdb)\\mssqllocaldb` (Windows only), MySQL 127.0.0.1:3307, MariaDB :3308, Postgres localhost.

## 3. What is reusable for a standalone server (via git submodule)

- Reuse directly: `Projects/Dotmim.Sync.Core`, `Dotmim.Sync.SqlServer` (+`.ChangeTracking`), `Dotmim.Sync.Web.Server` (targets netstandard2.0/net6/net8) as ProjectReferences, pinned by the submodule at the 1.3.0 commit. This guarantees wire-exact 1.3.0 behaviour.
- Template to copy: `Samples/HelloWebSync/HelloWebSyncServer` (net8 web project, ProjectReferences to Web.Server/SqlServer; `services.AddSyncServer(provider, tables, options)`; `SyncController` POST -> `webServerAgent.HandleRequestAsync(HttpContext)`; session + distributed memory cache). Other samples for setups: `HelloMultiScopesSync`, `MutliOrchestratorsWebSync`, `FilterWebSync`, `SimpleFilter`, `ComplexFilter`, `ConflictHandling`, `HelloWebAuthSync`.
- Reusable as schema/data: `AdventureWorksContext` + `OnSeeding` (would need the test project's EF packages; simpler to run `CreateAdventureWorks.sql`).
- Not reusable as-is: the xUnit classes and `KestrelTestServer` (random port, in-process, per-test DBs, tied to xUnit fixtures). The *assertions* are a good catalogue of scenarios for the Kotlin harness to port.
- Caveat: Directory.Build.props / packages.lock.json in the submodule apply when building inside it; build our own server project outside the submodule tree, or pass `--no-restore`-compatible lock settings.

## 4. Docker image: feasibility and sketch

Feasible and small. Nothing in 1.3.0 requires Windows; CI already runs SQL Server on Linux.

```
docker-compose.yml
  mssql:  mcr.microsoft.com/mssql/server:2022-latest   (ACCEPT_EULA=Y, MSSQL_SA_PASSWORD=..., healthcheck sqlcmd)
  sync:   build ./sync-server  -> depends_on mssql healthy, port 8080
```

`sync-server/Dockerfile`:
```dockerfile
FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build
WORKDIR /src
COPY Dotmim.Sync/ Dotmim.Sync/          # git submodule @ 8569980
COPY TestServer/ TestServer/
RUN dotnet publish TestServer/TestServer.csproj -c Release -o /out
FROM mcr.microsoft.com/dotnet/aspnet:8.0
COPY --from=build /out /app
WORKDIR /app
ENV ASPNETCORE_URLS=http://+:8080
ENTRYPOINT ["dotnet","TestServer.dll"]
```

`TestServer` (copy of HelloWebSyncServer, minimal API, no HTTPS redirect) on startup:
1. Wait for SQL Server; create DB and run `CreateAdventureWorks.sql` (or our own small schema).
2. Register several endpoints with `AddSyncServer(provider, setup, options, webServerOptions, scopeName, identifier)` and map each to its own route, mirroring `MultiScopes` test:
   - `/api/sync` default scope (all tables),
   - `/api/sync/v1`, `/v2` named scopes on the same DB (multi-scope),
   - `/api/filter` with `SetupFilter` (e.g. `Customer` by `CustomerID` parameter, joins as in `FilterWebSync`),
   - conflict policy endpoints: `SyncOptions.ConflictResolutionPolicy = ServerWins|ClientWins`, plus one with `OnApplyChangesConflictOccured` interceptor for merge;
   - snapshots: `SnapshotsDirectory=/snapshots`, call `RemoteOrchestrator.CreateSnapshotAsync(setup)` at boot (or via an admin endpoint) so clients get snapshot batches.
3. Optional test-only admin endpoints (`POST /admin/reset`, `/admin/sql`, `/admin/snapshot`) so Kotlin tests can seed server rows / create conflicts without a JDBC driver. Keep them behind an env flag.

Kotlin side: Testcontainers `ComposeContainer` (or two `GenericContainer`s on a network) starts it once per test suite; tests talk to `http://host:mappedPort/api/...` with a fresh client DB per test, and use admin endpoints to mutate server state.

Alternative cheaper DB: `SqliteSyncProvider` cannot be a server hub? It can (any CoreProvider can be the server), which would drop the SQL Server container, but real deployments are SQL Server, and SQL-Server-specific type handling would then be untested. Recommend SQL Server (or SQL Server Change Tracking provider if production uses it).

Risks: SQL Server image is ~1.5 GB and needs ~2 GB RAM, slow first start (~20-30 s); ARM Macs need Rosetta/`--platform linux/amd64` for mssql (or use `azure-sql-edge`, which is retired, so prefer amd64 emulation).
