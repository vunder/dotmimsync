import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    `maven-publish`
}

kotlin {
    androidTarget {
        publishLibraryVariants("release")
        withSourcesJar(publish = true)

        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_18)
                }
            }
        }
    }
    
//    listOf(
//        iosX64(),
//        iosArm64(),
//        iosSimulatorArm64()
//    ).forEach {
//        it.binaries.framework {
//            baseName = "shared"
//            isStatic = true
//        }
//    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.jetbrains.serialization.core)
            implementation(libs.jetbrains.serialization.json)
//            implementation(libs.jetbrains.serialization.core.jvm)
            implementation(libs.kotlinx.datetime)
            implementation(libs.androidx.sqlite.bundled)

            implementation(libs.bundles.ktor)

            implementation(libs.gradle.simple)
        }
        androidMain.dependencies {
//            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
//            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.mimetis.dotmimsync"
    compileSdk = 34
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_18
        targetCompatibility = JavaVersion.VERSION_18
    }

    buildTypes {
        debug {  }
        release {  }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

publishing.publications
    .withType<MavenPublication>()
    .configureEach {
        groupId = "com.github.vunder"
        version = "1.1.0"

        pom {
            name = "dotmimsync"
            description = "Android (Kotlin) port for Dotmim.Sync C# library (https://dotmimsync.readthedocs.io/)"
            url = "https://github.com/vunder/dotmimsync"

            developers {
                developer {
                    id = "vunder"
                    name = "Aleksei Starchikov"
                    email = "wp7apps@mail.ru"
                }
            }

            issueManagement {
                system = "GitHub"
                url = "https://github.com/vunder/dotmimsync/issues"
            }

            scm {
                url = "https://github.com/vunder/dotmimsync"
            }
        }
    }

publishing {
    repositories {
        mavenLocal()

        maven {
            name = "BuildDir"
            url = uri(project.layout.buildDirectory.dir("maven-repo"))
        }

        maven {
            name = "JitPack"
        }

        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/vunder/dotmimsync")
            credentials {
                username = ""
                password = ""
            }
        }
    }
}