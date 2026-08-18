pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
        gradlePluginPortal()
    }
}

rootProject.name = "Träwelldroid"
include(":app")

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
