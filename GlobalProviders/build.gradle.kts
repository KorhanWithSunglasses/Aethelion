import com.lagradost.cloudstream3.gradle.CloudstreamExtension

cloudstream {
    setPlugin(
        name = "GlobalProviders",
        description = "Global Movies, Series and Anime Providers",
        authors = listOf("Korhan"),
        version = 1,
        language = "en"
    )
}
