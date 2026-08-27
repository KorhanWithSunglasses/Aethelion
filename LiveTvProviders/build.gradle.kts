import com.lagradost.cloudstream3.gradle.CloudstreamExtension

cloudstream {
    setPlugin(
        name = "LiveTvProviders",
        description = "Live TV Channels and Sports Streams",
        authors = listOf("Korhan"),
        version = 1,
        language = "tr"
    )
}
