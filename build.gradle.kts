buildscript {
    dependencies {
        // AGP 9.x has built-in Kotlin support. Pin the compiler toolchain used by
        // the Compose compiler plugin so CI and Android Studio resolve identically.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21")
    }
}

plugins {
    id("com.android.application") version "9.4.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
}
