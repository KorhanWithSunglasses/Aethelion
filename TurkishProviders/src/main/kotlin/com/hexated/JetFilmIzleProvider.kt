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

class JetFilmIzleProvider : MainAPI() {
    override var name = "JetFilmİzle"
    override var mainUrl = "https://jetfilmizle.com"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie)

    // 500ms delay prevents 429 Too Many Requests rate limiting
    override var sequentialMainPage = true
    override var sequentialMainPageDelay = 500L
    override var sequentialMainPageScrollDelay = 500L

    private val fallbackDomains = listOf(
        "https://jetfilmizle.com",
        "https://jetfilmizle.mobi",
        "https://jetfilmizle.ws"
    )

    private suspend fun getUrl(): String {
        return DynamicDomainHelper.getActiveDomain("jetfilmizle", fallbackDomains)
    }

    override val mainPage = mainPageOf(
        "" to "Son Eklenen Filmler",
        "turkce-dublaj-filmler-izle" to "Türkçe Dublaj",
        "turkce-altyazili-filmler-izle" to "Türkçe Altyazılı",
        "tur/aksiyon-filmleri" to "Aksiyon",
        "tur/bilim-kurgu-filmleri" to "Bilim Kurgu",
        "tur/komedi-filmleri" to "Komedi",
        "tur/korku-filmleri" to "Korku",
        "tur/animasyon-filmleri" to "Animasyon"
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
            val home = doc.select("article, div.movie-card, div.poster, div.film-box, div.content-item a").mapNotNull {
                it.toSearchResult(domain)
            }.distinctBy { it.url }
            newHomePageResponse(request.name, home)
        } catch (_: Exception) {
            newHomePageResponse(request.name, emptyList())
        }
    }

    private fun Element.toSearchResult(domain: String): SearchResponse? {
        val rawHref = this.attr("href").ifEmpty { this.selectFirst("a")?.attr("href") } ?: return null
        val href = if (rawHref.startsWith("http")) rawHref else "$domain$rawHref"

        val title = this.selectFirst("h2, h3, .title, .film-title, img[alt]")?.let {
            if (it.tagName() == "img") it.attr("alt") else it.text()
        }?.trim()?.ifEmpty { null } ?: this.text().trim().ifEmpty { null } ?: return null

        val posterUrl = this.selectFirst("img")?.let { img ->
            val dataSrc = img.attr("data-src").ifEmpty { img.attr("data-srcset") }.ifEmpty { img.attr("data-lazy-src") }.ifEmpty { img.attr("srcset") }
            val src = img.attr("src")
            if (dataSrc.isNotEmpty() && !dataSrc.startsWith("data:")) {
                dataSrc.split(" ").firstOrNull { it.startsWith("http") } ?: dataSrc
            } else if (src.isNotEmpty() && !src.startsWith("data:")) {
                src
            } else {
                null
            }
        }

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val domain = getUrl()
        val searchUrl = "$domain/?s=$query"
        return try {
            val doc = app.get(searchUrl, headers = NetworkHelper.defaultHeaders).document
            doc.select("article, div.movie-card, div.search-result").mapNotNull {
                it.toSearchResult(domain)
            }.distinctBy { it.url }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val domain = getUrl()
        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document

        val title = doc.selectFirst("h1, .entry-title, .title")?.text()?.trim() ?: "Film"
        val poster = doc.selectFirst("div.poster img, .movie-poster img, meta[property=og:image]")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }.ifEmpty { it.attr("content") }
        }
        val description = doc.selectFirst("div.overview, div.description, div.entry-content p")?.text()?.trim()
        val year = doc.selectFirst(".year, .date")?.text()?.filter { it.isDigit() }?.toIntOrNull()
        val tags = doc.select("div.genres a, .tags a").map { it.text().trim() }

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

        val iframes = mutableListOf<String>()
        doc.select("iframe").forEach {
            val src = it.attr("src").ifEmpty { it.attr("data-src") }
            if (src.isNotEmpty()) iframes.add(src)
        }

        doc.select("div.player-tabs button, div.sources a, button[data-src]").forEach { tab ->
            val src = tab.attr("data-src").ifEmpty { tab.attr("data-url") }.ifEmpty { tab.attr("href") }
            if (src.isNotEmpty() && !src.startsWith("#")) iframes.add(src)
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
                else -> ExtractorHelper.resolveStream(cleanUrl, data, "JetFilmİzle", subtitleCallback, callback)
            }
        }

        return true
    }
}
