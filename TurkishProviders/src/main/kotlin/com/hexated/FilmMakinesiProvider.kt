package com.hexated

import com.hexated.core.ImageHelper
import com.hexated.core.NetworkHelper
import com.hexated.core.fixUrl
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class FilmMakinesiProvider : MainAPI() {
    override var mainUrl = "https://filmmakinesi.de"
    override var name = "Film Makinesi"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = true
    override val supportedTypes = setOf(TvType.Movie)

    override var sequentialMainPage = true
    override var sequentialMainPageDelay = 500L
    override var sequentialMainPageScrollDelay = 500L

    override val mainPage = mainPageOf(
        "${mainUrl}/film-izle/page/" to "Son Eklenen Filmler",
        "${mainUrl}/film-izle/turkce-dublaj-film-izle/page/" to "Türkçe Dublaj",
        "${mainUrl}/film-izle/turkce-altyazili-film-izle/page/" to "Türkçe Altyazılı",
        "${mainUrl}/film-izle/aksiyon-filmleri-izle/page/" to "Aksiyon",
        "${mainUrl}/film-izle/bilim-kurgu-filmleri-izle/page/" to "Bilim Kurgu",
        "${mainUrl}/film-izle/komedi-filmi-izle/page/" to "Komedi",
        "${mainUrl}/film-izle/romantik-filmler-izle/page/" to "Romantik",
        "${mainUrl}/film-izle/belgesel/page/" to "Belgesel",
        "${mainUrl}/film-izle/fantastik-filmler-izle/page/" to "Fantastik",
        "${mainUrl}/film-izle/korku-filmleri-izle-hd/page/" to "Korku"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}${page}", headers = NetworkHelper.defaultHeaders).document
        val home = if (request.data.contains("/film-izle/")) {
            document.select("section#film_posts article").mapNotNull { it.toSearchResult() }
        } else {
            document.select("section#film_posts div.tooltip").mapNotNull { it.toSearchResult() }
        }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h6 a, h2 a, a.title")?.text()?.trim() ?: return null
        val rawHref = this.selectFirst("h6 a, h2 a, a")?.attr("href") ?: return null
        val href = fixUrl(rawHref, mainUrl)
        val posterUrl = ImageHelper.extractPosterUrl(this, mainUrl)

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    private fun Element.toRecommendResult(): SearchResponse? {
        val title = this.select("a").lastOrNull()?.text()?.trim() ?: return null
        val rawHref = this.select("a").lastOrNull()?.attr("href") ?: return null
        val href = fixUrl(rawHref, mainUrl)
        val posterUrl = ImageHelper.extractPosterUrl(this, mainUrl)

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}?s=${query}", headers = NetworkHelper.defaultHeaders).document
        return document.select("section#film_posts article").mapNotNull { it.toSearchResult() }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url, headers = NetworkHelper.defaultHeaders).document

        val title = document.selectFirst("div#film_izle h1, h1.film-title")?.text()?.trim() ?: return null
        val poster = ImageHelper.extractPosterUrl(document.selectFirst("div.poster, aside") ?: document, mainUrl)
            ?: document.selectFirst("[property='og:image']")?.attr("content")
        val description = document.select("section#film_single article p").lastOrNull()?.text()?.trim()
            ?: document.selectFirst("div.film-ozeti, div.overview")?.text()?.trim()
        val tags = document.selectFirst("dt:contains(Tür:) + dd")?.text()?.split(", ")
            ?: document.select("div.film-tur a").map { it.text().trim() }
        val rating = document.selectFirst("dt:contains(IMDB Puanı:) + dd")?.text()?.trim()?.toDoubleOrNull()
        val year = document.selectFirst("dt:contains(Yapım Yılı:) + dd")?.text()?.trim()?.toIntOrNull()

        val durationElement = document.select("dt:contains(Film Süresi:) + dd time").attr("datetime")
        val duration = if (durationElement.startsWith("PT") && durationElement.endsWith("M")) {
            durationElement.drop(2).dropLast(1).toIntOrNull() ?: 0
        } else {
            0
        }

        val recommendations = document.select("div.hidden-mobile li").mapNotNull { it.toRecommendResult() }
        val actors = document.selectFirst("dt:contains(Oyuncular:) + dd")?.text()?.split(", ")?.map {
            Actor(it.trim())
        }

        val trailer = fixUrlNull(document.selectXpath("//iframe[@title='Fragman']").attr("data-src"))

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.year = year
            this.plot = description
            this.tags = tags
            this.score = rating?.let { Score.from10(it) }
            this.duration = duration
            this.recommendations = recommendations
            actors?.let { addActors(it) }
            addTrailer(trailer)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data, headers = NetworkHelper.defaultHeaders).document
        val iframes = mutableListOf<String>()

        val iframeElement = document.selectFirst("div.player-div iframe, div#film_player iframe, iframe")
        val iframe = iframeElement?.attr("src") ?: iframeElement?.attr("data-src")
        if (!iframe.isNullOrEmpty() && !iframe.startsWith("#")) {
            iframes.add(fixUrl(iframe, mainUrl))
        }

        document.select("div.player-tabs button, div.sources a, button[data-src]").forEach { tab ->
            val src = tab.attr("data-src").ifEmpty { tab.attr("data-url") }.ifEmpty { tab.attr("href") }
            if (src.isNotEmpty() && !src.startsWith("#")) {
                iframes.add(fixUrl(src, mainUrl))
            }
        }

        for (u in iframes.distinct()) {
            loadExtractor(u, "${mainUrl}/", subtitleCallback, callback)
        }

        return true
    }
}
