# Dotmim.Sync for Android

[![Release](https://jitpack.io/v/vunder/dotmimsync.svg)](https://jitpack.io/vunder/dotmimsync)
[![](https://jitci.com/gh/vunder/dotmimsync/svg)](https://jitci.com/gh/vunder/dotmimsync)

Android (Kotlin) port for [Dotmim.Sync](https://github.com/Mimetis/Dotmim.Sync) C# library. Original library documentation can be found [here](https://dotmimsync.readthedocs.io/)


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
            implementation 'io.github.vunder:dotmimsync:VERSION'
    }
```


## Library usage
General use-cases you can find in original library documentation


## Dotmim.Sync version match table
Here is a version match table. Left column represent current library, right column - Dotmim.Sync library

|Library version|Dotmim.Sync version|
|-|-|
|1.0-1.0.10|0.9.1 or lower|
|1.1.0|0.9.1 or lower|