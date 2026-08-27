import com.lagradost.cloudstream3.gradle.CloudstreamExtension

cloudstream {
    setPlugin(
        name = "AsianAnimeProviders",
        description = "Turkish Anime and Asian Drama (K-Drama) Providers",
        authors = listOf("Hexated"),
        version = 1,
        language = "tr"
    )
}

dependencies {
    implementation(project(":core"))
    implementation(project(":extractors"))
}
