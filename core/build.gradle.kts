import com.lagradost.cloudstream3.gradle.CloudstreamExtension

cloudstream {
    setPlugin(
        name = "Core",
        description = "Core utilities and network helpers",
        authors = listOf("Hexated"),
        version = 1,
        language = "all"
    )
}
