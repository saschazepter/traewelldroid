plugins {
    kotlin("android") version "2.2.0" apply false
    id("com.android.application") version "8.9.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.0" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("com.mikepenz.aboutlibraries.plugin") version "11.1.3" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
