package com.hexated

import com.hexated.core.DynamicDomainHelper
import com.hexated.core.ExtractorHelper
import com.hexated.core.NetworkHelper
import com.hexated.extractors.CloseLoad
import com.hexated.extractors.Rapidame
import com.hexated.extractors.Streamwish
import com.hexated.extractors.Vidmoly
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import org.jsoup.nodes.Element

class DiziWatchProvider : MainAPI() {
    override var name = "DiziWatch"
    override var mainUrl = "https://diziwatch.ac"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.TvSeries, TvType.Anime)

    private val fallbackDomains = listOf(
        "https://diziwatch.ac",
        "https://diziwatch.net",
        "https://diziwatch.vip"
    )

    private suspend fun getUrl(): String {
        return DynamicDomainHelper.getActiveDomain("diziwatch", fallbackDomains)
    }

    override val mainPage = mainPageOf(
        "" to "Son Eklenen Bölümler",
        "dizi-arsivi" to "Tüm Diziler",
        "anime-arsivi" to "Anime Arşivi",
        "trend-diziler" to "Trend Diziler",
        "populer-diziler" to "Popüler Diziler"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val domain = getUrl()
        val url = if (page <= 1) {
            if (request.data.isEmpty()) domain else "$domain/${request.data}"
        } else {
            "$domain/${request.data}/page/$page/"
        }

        return try {
            val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document
            val home = doc.select("a[href*=\"/dizi/\"], div.content-inner a, div.grid a").mapNotNull {
                it.toSearchResult(domain)
            }.distinctBy { it.url }
            newHomePageResponse(request.name, home)
        } catch (_: Exception) {
            newHomePageResponse(request.name, emptyList())
        }
    }

    private fun Element.toSearchResult(domain: String): SearchResponse? {
        val rawHref = this.attr("href").ifEmpty { this.selectFirst("a")?.attr("href") } ?: return null
        if (!rawHref.contains("/dizi/")) return null
        val href = if (rawHref.startsWith("http")) rawHref else "$domain$rawHref"

        val title = this.selectFirst("h2, h3, .title, img[alt]")?.let {
            if (it.tagName() == "img") it.attr("alt") else it.text()
        }?.trim()?.ifEmpty { null } ?: return null

        val posterUrl = this.selectFirst("img")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }.ifEmpty { it.attr("data-lazy-src") }
        }

        val isAnime = href.contains("anime") || this.text().contains("Anime", ignoreCase = true)
        val type = if (isAnime) TvType.Anime else TvType.TvSeries

        return if (type == TvType.Anime) {
            newAnimeSearchResponse(title, href, type) {
                this.posterUrl = posterUrl
            }
        } else {
            newTvSeriesSearchResponse(title, href, type) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val domain = getUrl()
        val searchUrl = "$domain/?s=$query"
        return try {
            val doc = app.get(searchUrl, headers = NetworkHelper.defaultHeaders).document
            doc.select("a[href*=\"/dizi/\"], div.content-inner a").mapNotNull {
                it.toSearchResult(domain)
            }.distinctBy { it.url }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val domain = getUrl()
        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document

        val title = doc.selectFirst("h1, h2, .title")?.text()?.trim()
            ?: doc.selectFirst("img[alt]")?.attr("alt")?.trim()
            ?: "Dizi"

        val poster = doc.selectFirst("img[data-src], meta[property=og:image]")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }.ifEmpty { it.attr("content") }
        }
        val description = doc.selectFirst("div.overview, div.description, p")?.text()?.trim()
        val year = doc.selectFirst(".year, .date")?.text()?.filter { it.isDigit() }?.toIntOrNull()
        val tags = doc.select("div.genres a, .tags a, a[href*=tur/]").map { it.text().trim() }

        val episodes = mutableListOf<Episode>()

        // Parse all episode links (/sezon-1/bolum-1, etc.)
        doc.select("a[href*=\"/sezon-\"], a[href*=\"/bolum-\"]").forEachIndexed { index, epLink ->
            val epHref = epLink.attr("href").let { if (it.startsWith("http")) it else "$domain$it" }
            val epText = epLink.text().trim()

            val seasonNum = Regex("""sezon-(\d+)""").find(epHref)?.groupValues?.get(1)?.toIntOrNull()
                ?: Regex("""(\d+)\.\s*Sezon""").find(epText)?.groupValues?.get(1)?.toIntOrNull()
                ?: 1

            val epNum = Regex("""bolum-(\d+)""").find(epHref)?.groupValues?.get(1)?.toIntOrNull()
                ?: Regex("""(\d+)\.\s*Bölüm""").find(epText)?.groupValues?.get(1)?.toIntOrNull()
                ?: (index + 1)

            val epName = epText.ifEmpty { "Sezon $seasonNum Bölüm $epNum" }

            episodes.add(
                newEpisode(epHref) {
                    this.name = epName
                    this.season = seasonNum
                    this.episode = epNum
                    this.posterUrl = poster
                }
            )
        }

        if (episodes.isEmpty()) {
            episodes.add(newEpisode(url) {
                this.name = "1. Bölüm"
                this.season = 1
                this.episode = 1
                this.posterUrl = poster
            })
        }

        val isAnime = url.contains("anime") || tags.any { it.contains("Anime", ignoreCase = true) }

        return if (isAnime) {
            newAnimeLoadResponse(title, url, TvType.Anime) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
                this.tags = tags
                this.episodes = mutableMapOf(DubStatus.Subbed to episodes)
            }
        } else {
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
                this.tags = tags
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data, headers = NetworkHelper.defaultHeaders).document

        val iframes = mutableListOf<String>()
        doc.select("iframe").forEach {
            val src = it.attr("src").ifEmpty { it.attr("data-src") }
            if (src.isNotEmpty()) iframes.add(src)
        }

        val vidmoly = Vidmoly()
        val rapidame = Rapidame()
        val streamwish = Streamwish()
        val closeLoad = CloseLoad()

        iframes.distinct().forEach { rawUrl ->
            val cleanUrl = if (rawUrl.startsWith("//")) "https:$rawUrl" else rawUrl

            when {
                cleanUrl.contains("vidmoly") -> vidmoly.getUrl(cleanUrl, data, subtitleCallback, callback)
                cleanUrl.contains("rapidame") -> rapidame.getUrl(cleanUrl, data, subtitleCallback, callback)
                cleanUrl.contains("streamwish") -> streamwish.getUrl(cleanUrl, data, subtitleCallback, callback)
                cleanUrl.contains("closeload") -> closeLoad.getUrl(cleanUrl, data, subtitleCallback, callback)
                else -> ExtractorHelper.resolveStream(cleanUrl, data, "DiziWatch", subtitleCallback, callback)
            }
        }

        return true
    }
}
