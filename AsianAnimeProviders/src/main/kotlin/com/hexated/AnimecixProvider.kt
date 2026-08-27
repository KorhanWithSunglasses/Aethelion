package com.hexated

import com.hexated.core.DynamicDomainHelper
import com.hexated.core.NetworkHelper
import com.hexated.extractors.Rapidame
import com.hexated.extractors.Vidmoly
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class AnimecixProvider : MainAPI() {
    override var name = "AnimeciX"
    override var mainUrl = "https://animecix.net"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Anime, TvType.AnimeMovie)

    private val fallbackDomains = listOf(
        "https://animecix.net",
        "https://animecix.com",
        "https://animecix.tv"
    )

    private suspend fun getUrl(): String {
        return DynamicDomainHelper.getActiveDomain("animecix", fallbackDomains)
    }

    override val mainPage = mainPageOf(
        "" to "Son Eklenen Bölümler",
        "animeler" to "Tüm Animeler",
        "trendler" to "Trend Animeler"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val domain = getUrl()
        val url = if (page <= 1) {
            "$domain/${request.data}"
        } else {
            "$domain/${request.data}?page=$page"
        }

        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document
        val home = doc.select("div.anime-card, div.poster, article, div.item").mapNotNull {
            it.toSearchResult()
        }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h2, h3, .title, .name, a")?.text()?.trim() ?: return null
        val href = this.selectFirst("a")?.attr("href") ?: return null
        val posterUrl = this.selectFirst("img")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }
        }

        return newAnimeSearchResponse(title, href, TvType.Anime) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val domain = getUrl()
        val searchUrl = "$domain/arama?q=$query"
        val doc = app.get(searchUrl, headers = NetworkHelper.defaultHeaders).document

        return doc.select("div.anime-card, div.poster, article, div.search-result").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document

        val title = doc.selectFirst("h1, .anime-title, .title")?.text()?.trim() ?: "Bilinmeyen Anime"
        val poster = doc.selectFirst("div.poster img, .anime-poster img, meta[property=og:image]")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }.ifEmpty { it.attr("content") }
        }
        val description = doc.selectFirst("div.overview, div.description, p.story")?.text()?.trim()
        val year = doc.selectFirst(".year, .release-date")?.text()?.filter { it.isDigit() }?.toIntOrNull()
        val rating = doc.selectFirst(".score, .rating")?.text()?.toRatingInt()
        val tags = doc.select("div.genres a").map { it.text().trim() }

        val episodes = mutableListOf<Episode>()
        doc.select("div.episode-item, li.episode, a.episode-link, div.episodes a").forEachIndexed { index, ep ->
            val epHref = ep.attr("href")
            val epName = ep.text().trim().ifEmpty { "${index + 1}. Bölüm" }
            val epNum = Regex("""(\d+)\.\s*bölüm""", RegexOption.IGNORE_CASE).find(epName)?.groupValues?.get(1)?.toIntOrNull() ?: (index + 1)

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
            this.rating = rating
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

        val iframes = doc.select("iframe, div.player iframe").mapNotNull {
            it.attr("data-src").ifEmpty { it.attr("src") }.takeIf { src -> src.isNotEmpty() }
        }.toMutableList()

        for (iframeUrl in iframes.distinct()) {
            val cleanUrl = if (iframeUrl.startsWith("//")) "https:$iframeUrl" else iframeUrl

            when {
                cleanUrl.contains("vidmoly") -> Vidmoly().getUrl(cleanUrl, data)?.forEach { callback(it) }
                cleanUrl.contains("rapidame") -> Rapidame().getUrl(cleanUrl, data)?.forEach { callback(it) }
                else -> loadExtractor(cleanUrl, data, subtitleCallback, callback)
            }
        }

        return true
    }
}
