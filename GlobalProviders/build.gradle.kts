import com.lagradost.cloudstream3.gradle.CloudstreamExtension

cloudstream {
    setPlugin(
        name = "GlobalProviders",
        description = "Global Movies, Series and Anime Providers (SFlix, Vidsrc, LookMovie, etc.)",
        authors = listOf("Hexated"),
        version = 1,
        language = "en"
    )
}

dependencies {
    implementation(project(":core"))
    implementation(project(":extractors"))
}
