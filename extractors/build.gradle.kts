import com.lagradost.cloudstream3.gradle.CloudstreamExtension

cloudstream {
    setPlugin(
        name = "Extractors",
        description = "Shared video host extractors",
        authors = listOf("Hexated"),
        version = 1,
        language = "all"
    )
}

dependencies {
    implementation(project(":core"))
}
