buildscript {
    configurations.classpath {
        resolutionStrategy {
            force("org.jetbrains:annotations:23.0.0")
        }
    }
}
plugins {
    kotlin("android") version "2.4.0" apply false
    id("com.android.application") version "9.3.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10" apply false
    id("com.google.gms.google-services") version "4.5.0" apply false
    id("com.mikepenz.aboutlibraries.plugin") version "15.0.4" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
