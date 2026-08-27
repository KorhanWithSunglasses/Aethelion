import com.lagradost.cloudstream3.gradle.CloudstreamExtension

cloudstream {
    setPlugin(
        name = "TurkishProviders",
        description = "Turkish Movies & Series Providers for CloudStream",
        authors = listOf("Hexated"),
        version = 1,
        language = "tr"
    )
}

dependencies {
    implementation(project(":core"))
    implementation(project(":extractors"))
}
