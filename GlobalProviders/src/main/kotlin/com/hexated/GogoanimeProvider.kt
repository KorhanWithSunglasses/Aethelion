package com.hexated

import com.hexated.core.DynamicDomainHelper
import com.hexated.core.NetworkHelper
import com.hexated.extractors.CloseLoad
import com.hexated.extractors.Streamwish
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class GogoanimeProvider : MainAPI() {
    override var name = "Gogoanime"
    override var mainUrl = "https://anitaku.pe"
    override var lang = "en"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Anime, TvType.AnimeMovie)

    private val fallbackDomains = listOf(
        "https://anitaku.pe",
        "https://gogoanime3.co",
        "https://anitaku.so"
    )

    private suspend fun getUrl(): String {
        return DynamicDomainHelper.getActiveDomain("gogoanime", fallbackDomains)
    }

    override val mainPage = mainPageOf(
        "" to "Recent Releases",
        "popular.html" to "Popular Anime",
        "anime-movies.html" to "Anime Movies"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val domain = getUrl()
        val url = if (page <= 1) {
            "$domain/${request.data}"
        } else {
            "$domain/${request.data}?page=$page"
        }

        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document
        val home = doc.select("ul.items li, div.last_episodes ul li").mapNotNull {
            it.toSearchResult()
        }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("p.name a, a")?.text()?.trim() ?: return null
        val href = this.selectFirst("a")?.attr("href") ?: return null
        val posterUrl = this.selectFirst("img")?.let {
            it.attr("src")
        }

        return newAnimeSearchResponse(title, href, TvType.Anime) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val domain = getUrl()
        val searchUrl = "$domain/search.html?keyword=$query"
        val doc = app.get(searchUrl, headers = NetworkHelper.defaultHeaders).document

        return doc.select("ul.items li").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document

        val title = doc.selectFirst("div.anime_info_body_bg h1")?.text()?.trim() ?: "Unknown Anime"
        val poster = doc.selectFirst("div.anime_info_body_bg img")?.attr("src")
        val description = doc.selectFirst("div.anime_info_body_bg p.type:contains(Plot Summary)")?.text()?.substringAfter("Plot Summary:")?.trim()
        val year = doc.selectFirst("div.anime_info_body_bg p.type:contains(Released)")?.text()?.filter { it.isDigit() }?.toIntOrNull()
        val tags = doc.select("div.anime_info_body_bg p.type:contains(Genre) a").map { it.text().trim() }

        val episodes = mutableListOf<Episode>()
        doc.select("ul#episode_related li a").forEachIndexed { index, ep ->
            val epHref = ep.attr("href")
            val epName = ep.selectFirst("div.name")?.text()?.trim() ?: "${index + 1}. Episode"
            val epNum = Regex("""EP\s*(\d+)""", RegexOption.IGNORE_CASE).find(epName)?.groupValues?.get(1)?.toIntOrNull() ?: (index + 1)

            episodes.add(
                newEpisode(epHref) {
                    this.name = epName
                    this.episode = epNum
                }
            )
        }

        return newAnimeLoadResponse(title, url, TvType.Anime) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.episodes = mutableMapOf(DubStatus.Subbed to episodes)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data, headers = NetworkHelper.defaultHeaders).document

        val iframes = doc.select("div.play-video iframe").mapNotNull {
            it.attr("src").takeIf { src -> src.isNotEmpty() }
        }.toMutableList()

        for (iframeUrl in iframes.distinct()) {
            val cleanUrl = if (iframeUrl.startsWith("//")) "https:$iframeUrl" else iframeUrl

            when {
                cleanUrl.contains("streamwish") -> Streamwish().getUrl(cleanUrl, data)?.forEach { callback(it) }
                cleanUrl.contains("closeload") -> CloseLoad().getUrl(cleanUrl, data)?.forEach { callback(it) }
                else -> loadExtractor(cleanUrl, data, subtitleCallback, callback)
            }
        }

        return true
    }
}
