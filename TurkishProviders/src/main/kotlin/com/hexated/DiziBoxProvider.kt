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

class DiziBoxProvider : MainAPI() {
    override var name = "DiziBox"
    override var mainUrl = "https://www.dizibox.net"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.TvSeries)

    private val fallbackDomains = listOf(
        "https://www.dizibox.net",
        "https://www.dizibox.pw",
        "https://www.dizibox.top",
        "https://dizibox.tv"
    )

    private suspend fun getUrl(): String {
        return DynamicDomainHelper.getActiveDomain("dizibox", fallbackDomains)
    }

    override val mainPage = mainPageOf(
        "" to "Son Eklenen Bölümler",
        "dizi-arsivi" to "Dizi Arşivi",
        "tur/trend" to "Popüler Diziler"
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
            val home = doc.select("article, div.post, div.movie-card, div.box, div.film").mapNotNull {
                it.toSearchResult(domain)
            }
            newHomePageResponse(request.name, home)
        } catch (_: Exception) {
            newHomePageResponse(request.name, emptyList())
        }
    }

    private fun Element.toSearchResult(domain: String): SearchResponse? {
        val title = this.selectFirst("h2, h3, .title, .post-title, a")?.text()?.trim() ?: return null
        val rawHref = this.selectFirst("a")?.attr("href") ?: return null
        val href = if (rawHref.startsWith("http")) rawHref else "$domain$rawHref"
        val posterUrl = this.selectFirst("img")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }.ifEmpty { it.attr("data-lazy-src") }
        }

        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val domain = getUrl()
        val searchUrl = "$domain/?s=$query"
        return try {
            val doc = app.get(searchUrl, headers = NetworkHelper.defaultHeaders).document
            doc.select("article, div.post, div.search-result, div.box").mapNotNull {
                it.toSearchResult(domain)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val domain = getUrl()
        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document

        val title = doc.selectFirst("h1, .entry-title, .title")?.text()?.trim() ?: "Dizi"
        val poster = doc.selectFirst("div.poster img, .post-thumbnail img, meta[property=og:image]")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }.ifEmpty { it.attr("content") }
        }
        val description = doc.selectFirst("div.overview, div.description, .entry-content p")?.text()?.trim()
        val year = doc.selectFirst(".year, .date")?.text()?.filter { it.isDigit() }?.toIntOrNull()
        val tags = doc.select("div.genres a, .tags a").map { it.text().trim() }

        val episodes = mutableListOf<Episode>()
        doc.select("ul.episodes li a, div.episode-item a, div.episodes a, a.episode-link").forEachIndexed { index, ep ->
            val epHref = ep.attr("href").let { if (it.startsWith("http")) it else "$domain$it" }
            val epName = ep.text().trim().ifEmpty { "${index + 1}. Bölüm" }
            val seasonNum = Regex("""(\d+)\.\s*Sezon|Sezon\s*(\d+)""").find(epName)?.groupValues?.filter { it.isNotEmpty() }?.lastOrNull()?.toIntOrNull() ?: 1
            val epNum = Regex("""(\d+)\.\s*Bölüm|Bölüm\s*(\d+)""").find(epName)?.groupValues?.filter { it.isNotEmpty() }?.lastOrNull()?.toIntOrNull() ?: (index + 1)

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

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
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
                else -> ExtractorHelper.resolveStream(cleanUrl, data, "DiziBox", subtitleCallback, callback)
            }
        }

        return true
    }
}
