import com.lagradost.cloudstream3.gradle.CloudstreamExtension

cloudstream {
    setPlugin(
        name = "TurkishProviders",
        description = "Turkish Movies & Series Providers for CloudStream",
        authors = listOf("Korhan"),
        version = 1,
        language = "tr"
    )
}
