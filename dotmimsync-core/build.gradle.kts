import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    `maven-publish`
    alias(libs.plugins.vanniktech.maven.publish)
}

kotlin {
    jvm()

    // Android target configuration is now inside kotlin block
    android {
        namespace = "com.mimetis.dotmimsync"
        compileSdk = 36
        minSdk = 26

        withSourcesJar(publish = true)

        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_18)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.jetbrains.serialization.core)
            implementation(libs.jetbrains.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.androidx.sqlite.bundled)

            implementation(libs.ktor.client.core)

            implementation(libs.kotlin.bignum)
            implementation(libs.kotlin.bignum.serialization)
            implementation(kotlincrypto.hash.sha2)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

val libVersion = "1.1.1-beta21"
val versionSuffix = project.findProperty("versionSuffix") as String

publishing.publications
    .withType<MavenPublication>()
    .configureEach {
        groupId = "io.github.vunder.dotmimsync"
        version = libVersion
        if (versionSuffix.isNotBlank()) {
            version += "-$versionSuffix"
        }

        pom {
            name = "dotmimsync"
            description = "Android (Kotlin) port for Dotmim.Sync C# library (https://dotmimsync.readthedocs.io/)"
            url = "https://github.com/vunder/dotmimsync"

            licenses {
                license {
                    name = "MIT License"
                    url = "https://mit-license.org/"
                    distribution = "https://mit-license.org/"
                }
            }

            developers {
                developer {
                    id = "vunder"
                    name = "Aleksei Starchikov"
                    email = "aleksei.starchikov@outlook.com"
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
//        mavenLocal()

        maven {
            name = "BuildDir"
            url = uri(rootProject.layout.buildDirectory.dir("maven-repo"))
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

mavenPublishing {
    coordinates("io.github.vunder.dotmimsync", "dotmimsync-core", libVersion)

    publishToMavenCentral(automaticRelease = false)
    signAllPublications()
}