import com.vanniktech.maven.publish.SonatypeHost
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    `maven-publish`
    alias(libs.plugins.vanniktech.maven.publish)
}

kotlin {
//    jvm()

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

            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)

            implementation(libs.gradle.simple)
        }
        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.mimetis.dotmimsync"
    compileSdk = 35
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
        groupId = "io.github.vunder.dotmimsync"
        version = "1.1.0"

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
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL, automaticRelease = false)
    signAllPublications()
}