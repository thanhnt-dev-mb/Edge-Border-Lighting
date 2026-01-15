import java.text.SimpleDateFormat
import java.util.Date

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsKotlinAndroid)
    alias(libs.plugins.kotlinKsp)
    
    id("androidx.navigation.safeargs.kotlin")
    id("dagger.hilt.android.plugin")
    id("com.google.gms.google-services")
    id("kotlin-parcelize")
    id("com.google.firebase.crashlytics")
    id("kotlin-kapt")

    id("android-base-libs") // Custom plugin for base dependencies management

    //TODO: AppLovin
//    id("applovin-quality-service")
}

//TODO: AppLovin
//applovin {
//    apiKey =
//        "x5nodsO_qeATUUt7uw_s7e0oNDXl2tJFE3hnG4ks1J5l8anPmAGiONd7t--f28bXK6SfOUpcJfEmUJE5AhpkXC"
//}

android {
    namespace = "com.merryblue.baseapplication"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.merryblue.baseapp.baseapp"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.1"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

//        javaCompileOptions {
//            annotationProcessorOptions {
//                arguments += mapOf("room.schemaLocation" to "$projectDir/schemas")
//            }
//        }
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
        }

        missingDimensionStrategy("platform", "admob", "applovin")
    }

    buildTypes {
        debug {
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "BASE_URL", "\"assets://data\"")
        }

        release {
            manifestPlaceholders += mapOf()
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            manifestPlaceholders["admobAppId"] = "ca-app-pub-6445739239297382~1968340520"
            buildConfigField("String", "BASE_URL", "\"assets://data\"")
        }
    }

    applicationVariants.all {
        val variant = this
        variant.outputs
            .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
            .forEach { output ->
                val outputFileName = "${rootProject.name}_${
                    SimpleDateFormat(
                        "MM-dd"
                    ).format(Date())
                }.apk"
                output.outputFileName = outputFileName
            }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        buildConfig = true
    }
    sourceSets {
        getByName("main") {
            res {
                srcDirs("src/main/res", "src/main/res-common")
            }
        }
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(files("libs/core.aar"))

//    implementation(project(":core"))

    //TODO: AppLovin
    //noinspection GradleDynamicVersion
    implementation(libs.play.services.ads)
    implementation(libs.play.services.ads)
    implementation(libs.vungle)
    implementation(libs.pangle)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.androidx.appcompat)
}
