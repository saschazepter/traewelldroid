plugins {
    kotlin("android") version "2.2.0" apply false
    id("com.android.application") version "8.13.2" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.0" apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
    id("com.mikepenz.aboutlibraries.plugin.android") version "13.2.1" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
