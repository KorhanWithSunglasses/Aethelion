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

class HDFilmCehennemiProvider : MainAPI() {
    override var name = "HDFilmCehennemi"
    override var mainUrl = "https://hdfilmcehennemi.to"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    private val fallbackDomains = listOf(
        "https://hdfilmcehennemi.to",
        "https://www.hdfilmcehennemi.life",
        "https://www.hdfilmcehennemi.nl",
        "https://www.hdfilmcehennemi.net"
    )

    private suspend fun getUrl(): String {
        return DynamicDomainHelper.getActiveDomain("hdfilmcehennemi", fallbackDomains)
    }

    override val mainPage = mainPageOf(
        "" to "Son Eklenen Filmler",
        "diziler" to "Yabancı Diziler",
        "tur/yerli-filmler" to "Yerli Filmler",
        "tur/netflix-filmleri" to "Netflix Filmleri",
        "imdb-7-puan-uzeri-filmler" to "IMDb 7+ Filmler",
        "tur/animasyon" to "Animasyon Filmleri"
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
            val home = doc.select("div.poster, div.movie-card, article.card, div.film-item").mapNotNull {
                it.toSearchResult(domain)
            }
            newHomePageResponse(request.name, home)
        } catch (_: Exception) {
            newHomePageResponse(request.name, emptyList())
        }
    }

    private fun Element.toSearchResult(domain: String): SearchResponse? {
        val title = this.selectFirst("h2, h3, .poster-title, .title, a.card-title")?.text()?.trim() ?: return null
        val rawHref = this.selectFirst("a")?.attr("href") ?: return null
        val href = if (rawHref.startsWith("http")) rawHref else "$domain$rawHref"
        val posterUrl = this.selectFirst("img")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }.ifEmpty { it.attr("data-lazy-src") }
        }

        val isSeries = href.contains("/dizi/") || this.selectFirst(".is-series, .badge-series") != null
        val type = if (isSeries) TvType.TvSeries else TvType.Movie

        return if (type == TvType.TvSeries) {
            newTvSeriesSearchResponse(title, href, type) {
                this.posterUrl = posterUrl
            }
        } else {
            newMovieSearchResponse(title, href, type) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val domain = getUrl()
        val searchUrl = "$domain/search/$query"
        return try {
            val doc = app.get(searchUrl, headers = NetworkHelper.defaultHeaders).document
            doc.select("div.poster, div.movie-card, article.card, div.search-result").mapNotNull {
                it.toSearchResult(domain)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val domain = getUrl()
        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document

        val title = doc.selectFirst("h1, .movie-title, .title, .card-title")?.text()?.trim() ?: "Film"
        val poster = doc.selectFirst("div.poster img, .movie-poster img, meta[property=og:image], .card img")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }.ifEmpty { it.attr("content") }
        }
        val description = doc.selectFirst("div.description, div.overview, p.story, div.summary, .movie-story")?.text()?.trim()
        val year = doc.selectFirst(".year, .release-date, .date")?.text()?.filter { it.isDigit() }?.toIntOrNull()
        val tags = doc.select("div.genres a, div.tags a, .genre a").map { it.text().trim() }

        val isSeries = url.contains("/dizi/") || doc.select(".season-list, .episodes, .episode-item, .seasons, ul.bolumler").isNotEmpty()

        return if (isSeries) {
            val episodes = mutableListOf<Episode>()

            doc.select(".episode-item, .episodes a, ul.bolumler li a, div.tab-content a, .season-list a").forEachIndexed { index, epLink ->
                val epHref = epLink.attr("href").let { if (it.startsWith("http")) it else "$domain$it" }
                val epTitle = epLink.text().trim().ifEmpty { "Bölüm ${index + 1}" }
                val epNumber = Regex("""(\d+)\.\s*Bölüm|Bölüm\s*(\d+)""").find(epTitle)?.groupValues?.filter { it.isNotEmpty() }?.lastOrNull()?.toIntOrNull() ?: (index + 1)
                val seasonNumber = Regex("""(\d+)\.\s*Sezon|Sezon\s*(\d+)""").find(epTitle)?.groupValues?.filter { it.isNotEmpty() }?.lastOrNull()?.toIntOrNull() ?: 1

                episodes.add(
                    newEpisode(epHref) {
                        this.name = epTitle
                        this.season = seasonNumber
                        this.episode = epNumber
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

            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
                this.tags = tags
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
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

        doc.select("track[kind=subtitles], track[kind=captions]").forEach { track ->
            val subSrc = track.attr("src")
            val subLang = track.attr("label").ifEmpty { track.attr("srclang") }.ifEmpty { "Türkçe" }
            if (subSrc.isNotEmpty()) {
                subtitleCallback.invoke(SubtitleFile(lang = subLang, url = subSrc))
            }
        }

        val iframes = mutableListOf<String>()

        doc.select("iframe").forEach { iframe ->
            val src = iframe.attr("src").ifEmpty { iframe.attr("data-src") }
            if (src.isNotEmpty()) iframes.add(src)
        }

        doc.select("nav.nav-tabs a, div.player-tabs button, div.sources a, button[data-src]").forEach { tab ->
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
                else -> ExtractorHelper.resolveStream(cleanUrl, data, "HDFilmCehennemi", subtitleCallback, callback)
            }
        }

        return true
    }
}
