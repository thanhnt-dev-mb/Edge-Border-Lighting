// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.jetbrainsKotlinAndroid) apply false
    alias(libs.plugins.androidLibrary) apply false

    id("org.jlleitschuh.gradle.ktlint") version "10.2.0"
    id("com.google.devtools.ksp") version "2.0.21-1.0.27" apply false
}

//TODO: AppLovin
buildscript {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
//        maven { url = uri("https://artifacts.applovin.com/android") }
        maven { url = uri("https://jitpack.io") }
        maven(url = "https://jitpack.io")
        maven { setUrl("https://jitpack.io") }

        maven(url = "https://artifact.bytedance.com/repository/pangle/")
    }
    dependencies {
        classpath(libs.androidx.navigation.safe.args.gradle.plugin)
        classpath(libs.hilt.android.gradle.plugin)
        classpath(libs.google.services)
        classpath(libs.firebase.crashlytics.gradle)

        //noinspection GradleDynamicVersion
//        classpath ("com.applovin.quality:AppLovinQualityServiceGradlePlugin:+")
        classpath(
            kotlin(
                "gradle-plugin",
                version = "1.9.21"
            )
        )  //TODO: Need check for upgrading version to -> kotlin = "2.0.21"
    }
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint") // To apply ktLint to all included modules
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}