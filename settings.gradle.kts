pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
    }
}

plugins {
    id("com.lagradost.cloudstream3.gradle") version "0.1.1" apply false
}

rootProject.name = "CloudStreamRepo"

include(":core")
include(":extractors")
include(":TurkishProviders")
include(":AsianAnimeProviders")
include(":GlobalProviders")
include(":LiveTvProviders")
