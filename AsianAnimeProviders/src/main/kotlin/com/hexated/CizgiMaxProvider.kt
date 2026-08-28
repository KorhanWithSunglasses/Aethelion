package com.hexated

import com.hexated.core.ExtractorHelper

import com.hexated.core.DynamicDomainHelper
import com.hexated.core.NetworkHelper
import com.hexated.extractors.Rapidame
import com.hexated.extractors.Vidmoly
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class CizgiMaxProvider : MainAPI() {
    override var name = "CizgiMax"
    override var mainUrl = "https://cizgimax.online"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Cartoon, TvType.Anime)

    private val fallbackDomains = listOf(
        "https://cizgimax.online",
        "https://cizgimax.com",
        "https://cizgivedizi.com"
    )

    private suspend fun getUrl(): String {
        return DynamicDomainHelper.getActiveDomain("cizgimax", fallbackDomains)
    }

    override val mainPage = mainPageOf(
        "" to "Son Eklenen Çizgi Diziler",
        "cizgi-filmler" to "Çizgi Filmler",
        "animeler" to "Animeler"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val domain = getUrl()
        val url = if (page <= 1) {
            "$domain/${request.data}"
        } else {
            "$domain/${request.data}/page/$page/"
        }

        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document
        val home = doc.select("div.poster, div.card, article, div.item").mapNotNull {
            it.toSearchResult()
        }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h2, h3, .title, a")?.text()?.trim() ?: return null
        val href = this.selectFirst("a")?.attr("href") ?: return null
        val posterUrl = this.selectFirst("img")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }
        }

        return newTvSeriesSearchResponse(title, href, TvType.Cartoon) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val domain = getUrl()
        val searchUrl = "$domain/?s=$query"
        val doc = app.get(searchUrl, headers = NetworkHelper.defaultHeaders).document

        return doc.select("div.poster, div.card, article, div.search-result").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document

        val title = doc.selectFirst("h1, .entry-title, .title")?.text()?.trim() ?: "Bilinmeyen Çizgi Dizi"
        val poster = doc.selectFirst("div.poster img, .entry-thumbnail img")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }
        }
        val description = doc.selectFirst("div.entry-content p, .overview")?.text()?.trim()
        val year = doc.selectFirst(".year")?.text()?.filter { it.isDigit() }?.toIntOrNull()
        val tags = doc.select("div.genres a").map { it.text().trim() }

        val episodes = mutableListOf<Episode>()
        doc.select("div.episode-item, li.bolum a, a.episode-link, div.episodes a").forEachIndexed { index, ep ->
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

        return newTvSeriesLoadResponse(title, url, TvType.Cartoon, episodes) {
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

        val iframes = doc.select("iframe, div.player iframe").mapNotNull {
            it.attr("data-src").ifEmpty { it.attr("src") }.takeIf { src -> src.isNotEmpty() }
        }.toMutableList()

        for (iframeUrl in iframes.distinct()) {
            val cleanUrl = if (iframeUrl.startsWith("//")) "https:$iframeUrl" else iframeUrl

            when {
                cleanUrl.contains("vidmoly") -> Vidmoly().getUrl(cleanUrl, data, subtitleCallback, callback)
                cleanUrl.contains("rapidame") -> Rapidame().getUrl(cleanUrl, data, subtitleCallback, callback)
                else -> ExtractorHelper.resolveStream(cleanUrl, data, name, subtitleCallback, callback)
            }
        }

        return true
    }
}
