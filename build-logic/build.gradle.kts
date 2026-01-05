plugins {
  `kotlin-dsl`
}

repositories {
  google()
  mavenCentral()
  gradlePluginPortal()
}

gradlePlugin {
  plugins {
    create("AndroidBaseLibs") {
      id = "android-base-libs"
      implementationClass = "AndroidBasePlugin"
    }
  }
}