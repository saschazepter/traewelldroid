plugins {
    kotlin("android") version "2.2.0" apply false
    //noinspection AndroidGradlePluginVersion -- F-Droid fails
    id("com.android.application") version "8.11.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.20" apply false
    id("com.google.gms.google-services") version "4.4.3" apply false
    id("com.mikepenz.aboutlibraries.plugin") version "12.2.4" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
