# Dotmim.Sync for Android

[![Release](https://jitpack.io/v/vunder/dotmimsync.svg)](https://jitpack.io/vunder/dotmimsync)
[![](https://jitci.com/gh/vunder/dotmimsync/svg)](https://jitci.com/gh/vunder/dotmimsync)

Kotlin Multiplatform port for [Dotmim.Sync](https://github.com/Mimetis/Dotmim.Sync) C# library. Original library documentation can be found [here](https://dotmimsync.readthedocs.io/)


## Adding dependencies
To use library in your app add link to JitPack repo

```gradle
    allprojects {
        repositories {
            ...
            mavenCentral()
        }
    }
```
and add library dependency to your app build.gradle
```gradle
    dependencies {
        implementation 'io.github.vunder.dotmimsync:dotmimsync-core:VERSION'
    }
```

```kotlin
    BundledSQLiteDriver().open(databaseFullPath).use { sqliteConnection ->
        val syncSetup = SyncSetup() // fill sync tables info
        val serverOrchestrator = WebClientOrchestrator(
            "http://your-server-api",
            httpClient, // injected on created Ktor HttpClient instance
            authHeader // server authentication header, e.g. "Bearer <token>"
        )
        val clientProvider = SqliteSyncProvider(sqliteConnection)
        val syncOptions = SyncOptions(useVerboseErrors = true)
        val agent = SyncAgent(clientProvider, serverOrchestrator, syncOptions, syncSetup)
        val progress = object : Progress<ProgressArgs> {
            override fun report(value: ProgressArgs) {
                Log.d(
                    "sync-progress",
                    "LOCAL[${value.eventId}]. ${value.context.syncStage}: ${value.message} ${value.progressPercentage * 100}% (${value.hint})(${value.context.sessionId})"
                )
            }
        }
        val syncResult = agent.synchronize(progress = progress)
    }
```


## Library usage
General use-cases you can find in original library documentation


## Dotmim.Sync version match table
Here is a version match table. Left column represent current library, right column - Dotmim.Sync library

| Library version |Dotmim.Sync version|
|----------------|-|
| 1.0-1.1.0      |0.9.1 or lower|
| 1.1.0          |0.9.1 or lower|