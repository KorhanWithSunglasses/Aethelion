pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
    }
}

rootProject.name = "Aethelion"

include(":TurkishProviders")
include(":AsianAnimeProviders")
include(":GlobalProviders")
include(":LiveTvProviders")
