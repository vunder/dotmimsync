plugins {
    //trick: for the same plugin versions in all sub-modules
    alias(libs.plugins.kotlinMultiplatform).apply(false)
    alias(libs.plugins.androidKmpLibrary).apply(false)
    alias(libs.plugins.axion.release)
}

scmVersion {
    tag { prefix.set("v") }   // tags look like v1.2.3
}

allprojects {
    version = rootProject.scmVersion.version
}