package com.hexated

import com.hexated.core.DynamicDomainHelper
import com.hexated.core.NetworkHelper
import com.hexated.extractors.CloseLoad
import com.hexated.extractors.Rapidame
import com.hexated.extractors.Vidmoly
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
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
        "dizi-arsivi" to "Popüler Diziler",
        "anime-arsivi" to "Animeler"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val domain = getUrl()
        val url = if (page <= 1) {
            "$domain/${request.data}"
        } else {
            "$domain/${request.data}/page/$page/"
        }

        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document
        val home = doc.select("div.post-card, div.anime-card, div.series-card, article").mapNotNull {
            it.toSearchResult()
        }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h2, h3, .title, .post-title, a")?.text()?.trim() ?: return null
        val href = this.selectFirst("a")?.attr("href") ?: return null
        val posterUrl = this.selectFirst("img")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }
        }

        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val domain = getUrl()
        val searchUrl = "$domain/?s=$query"
        val doc = app.get(searchUrl, headers = NetworkHelper.defaultHeaders).document

        return doc.select("div.post-card, div.search-result, article, div.series-card").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document

        val title = doc.selectFirst("h1, .series-title, .entry-title")?.text()?.trim() ?: "Bilinmeyen Dizi"
        val poster = doc.selectFirst("div.series-poster img, .post-thumbnail img")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }
        }
        val description = doc.selectFirst(".series-summary, .entry-content p, .overview")?.text()?.trim()
        val year = doc.selectFirst(".year, .release-year")?.text()?.filter { it.isDigit() }?.toIntOrNull()
        
        val tags = doc.select(".series-genres a, .genres a").map { it.text().trim() }

        val episodes = mutableListOf<Episode>()
        doc.select("div.episode-item, li.episode, div.episodes a, a.episode-link").forEachIndexed { index, ep ->
            val epHref = ep.attr("href")
            val epName = ep.text().trim().ifEmpty { "${index + 1}. Bölüm" }
            val seasonNum = ep.attr("data-season").toIntOrNull()
                ?: Regex("""(\d+)\.\s*sezon""", RegexOption.IGNORE_CASE).find(epName)?.groupValues?.get(1)?.toIntOrNull()
                ?: 1
            val epNum = ep.attr("data-episode").toIntOrNull()
                ?: Regex("""(\d+)\.\s*bölüm""", RegexOption.IGNORE_CASE).find(epName)?.groupValues?.get(1)?.toIntOrNull()
                ?: (index + 1)

            episodes.add(
                newEpisode(epHref) {
                    this.name = epName
                    this.season = seasonNum
                    this.episode = epNum
                }
            )
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

        val iframes = doc.select("iframe, div.player iframe").mapNotNull {
            it.attr("data-src").ifEmpty { it.attr("src") }.takeIf { src -> src.isNotEmpty() }
        }.toMutableList()

        doc.select("div.player-nav a, button.video-source, a.source-btn").forEach { btn ->
            val playerUrl = btn.attr("data-url").ifEmpty { btn.attr("data-src") }.ifEmpty { btn.attr("href") }
            if (playerUrl.startsWith("http") || playerUrl.startsWith("//")) {
                iframes.add(if (playerUrl.startsWith("//")) "https:$playerUrl" else playerUrl)
            }
        }

        for (iframeUrl in iframes.distinct()) {
            val cleanUrl = if (iframeUrl.startsWith("//")) "https:$iframeUrl" else iframeUrl

            when {
                cleanUrl.contains("vidmoly") -> Vidmoly().getUrl(cleanUrl, data)?.forEach { callback(it) }
                cleanUrl.contains("rapidame") -> Rapidame().getUrl(cleanUrl, data)?.forEach { callback(it) }
                cleanUrl.contains("closeload") -> CloseLoad().getUrl(cleanUrl, data)?.forEach { callback(it) }
                else -> loadExtractor(cleanUrl, data, subtitleCallback, callback)
            }
        }

        return true
    }
}
