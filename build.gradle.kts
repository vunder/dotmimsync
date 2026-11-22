plugins {
    //trick: for the same plugin versions in all sub-modules
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    alias(libs.plugins.androidKmpLibrary).apply(false)
    alias(libs.plugins.axion.release)
}

// commits since last tag, the "4" in git describe's 1.2.3-4-gabc1234
val commitsSinceTagProvider = providers.exec {
    commandLine("git", "describe", "--tags", "--long")
    isIgnoreExitValue = true // no tags yet
}.standardOutput.asText.get().trim().split("-").let { it.getOrNull(it.size - 2) ?: "0" }

scmVersion {
    tag {
        prefix.set("v")   // tags look like v1.2.3
        initialVersion { _, _ -> "1.1.1"}
    }
    // after release 1.0.10: 1.0.11-132-SNAPSHOT
    val commitsSinceTag = commitsSinceTagProvider // local copy: lambda must not capture the script (config cache)
    snapshotCreator { _, _ -> "-beta$commitsSinceTag" }
}

allprojects {
    version = rootProject.scmVersion.version
}