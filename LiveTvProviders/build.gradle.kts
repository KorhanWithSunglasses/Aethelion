import com.lagradost.cloudstream3.gradle.CloudstreamExtension

cloudstream {
    setPlugin(
        name = "LiveTvProviders",
        description = "Live TV, Sports and IPTV Channels",
        authors = listOf("Hexated"),
        version = 1,
        language = "tr"
    )
}

dependencies {
    implementation(project(":core"))
    implementation(project(":extractors"))
}
