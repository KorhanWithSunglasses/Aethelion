package com.hexated

import com.hexated.core.DynamicDomainHelper
import com.hexated.core.NetworkHelper
import com.hexated.extractors.Rapidame
import com.hexated.extractors.Vidmoly
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class FilmModuProvider : MainAPI() {
    override var name = "FilmModu"
    override var mainUrl = "https://www.filmmodu.nl"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie)

    private val fallbackDomains = listOf(
        "https://www.filmmodu.nl",
        "https://www.filmmodu.cx",
        "https://www.filmmodu.org"
    )

    private suspend fun getUrl(): String {
        return DynamicDomainHelper.getActiveDomain("filmmodu", fallbackDomains)
    }

    override val mainPage = mainPageOf(
        "" to "Son Eklenen Filmler",
        "kategori/turkce-dublaj-filmler" to "Türkçe Dublaj",
        "kategori/turkce-altyazili-filmler" to "Türkçe Altyazılı",
        "kategori/4k-filmler" to "4K Ultra HD Filmler",
        "en-cok-izlenenler" to "Popüler Filmler"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val domain = getUrl()
        val url = if (page <= 1) {
            "$domain/${request.data}"
        } else {
            "$domain/${request.data}?page=$page"
        }

        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document
        val home = doc.select("div.movie-item, div.film-item, article, div.movie").mapNotNull {
            it.toSearchResult()
        }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h2, h3, .movie-title, .title, a")?.text()?.trim() ?: return null
        val href = this.selectFirst("a")?.attr("href") ?: return null
        val posterUrl = this.selectFirst("img")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }
        }

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val domain = getUrl()
        val searchUrl = "$domain/ara?term=$query"
        val doc = app.get(searchUrl, headers = NetworkHelper.defaultHeaders).document

        return doc.select("div.movie-item, div.film-item, article, div.search-result").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document

        val title = doc.selectFirst("h1, .movie-header-title, .title")?.text()?.trim() ?: "Bilinmeyen Film"
        val poster = doc.selectFirst("div.poster-image img, div.movie-poster img, meta[property=og:image]")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }.ifEmpty { it.attr("content") }
        }
        val description = doc.selectFirst(".summary, .movie-overview, p.story")?.text()?.trim()
        val year = doc.selectFirst(".release-year, .year, span.date")?.text()?.filter { it.isDigit() }?.toIntOrNull()
        
        val tags = doc.select("div.categories a, div.genres a").map { it.text().trim() }

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
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

        val iframes = doc.select("iframe, div.video-player iframe, div#video iframe").mapNotNull {
            it.attr("data-src").ifEmpty { it.attr("src") }.takeIf { src -> src.isNotEmpty() }
        }.toMutableList()

        doc.select("div.sources a, button.source-btn, a.dropdown-item").forEach { btn ->
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
                else -> loadExtractor(cleanUrl, data, subtitleCallback, callback)
            }
        }

        return true
    }
}
