package com.hexated

import com.hexated.core.DynamicDomainHelper
import com.hexated.core.NetworkHelper
import com.hexated.extractors.Rapidame
import com.hexated.extractors.Vidmoly
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class WebteIzleProvider : MainAPI() {
    override var name = "WebteIzle"
    override var mainUrl = "https://webteizle3.xyz"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    private val fallbackDomains = listOf(
        "https://webteizle3.xyz",
        "https://webteizle.cx",
        "https://webteizle.vip"
    )

    private suspend fun getUrl(): String {
        return DynamicDomainHelper.getActiveDomain("webteizle", fallbackDomains)
    }

    override val mainPage = mainPageOf(
        "" to "Son Eklenen Filmler",
        "turkce-dublaj" to "Türkçe Dublaj",
        "turkce-altyazi" to "Türkçe Altyazılı",
        "diziler" to "Diziler"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val domain = getUrl()
        val url = if (page <= 1) {
            "$domain/${request.data}"
        } else {
            "$domain/${request.data}/page/$page/"
        }

        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document
        val home = doc.select("div.film, div.poster, article, div.movie-card").mapNotNull {
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

        val isSeries = href.contains("/dizi/")
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
        val searchUrl = "$domain/arama?q=$query"
        val doc = app.get(searchUrl, headers = NetworkHelper.defaultHeaders).document

        return doc.select("div.film, div.poster, article, div.search-result").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document

        val title = doc.selectFirst("h1, .movie-title, .title")?.text()?.trim() ?: "Bilinmeyen İçerik"
        val poster = doc.selectFirst("div.poster img, .movie-poster img")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }
        }
        val description = doc.selectFirst("div.description, div.story, p.overview")?.text()?.trim()
        val year = doc.selectFirst(".year, .release-year")?.text()?.filter { it.isDigit() }?.toIntOrNull()
        val rating = doc.selectFirst(".imdb, .rating")?.text()?.toRatingInt()
        val tags = doc.select("div.genres a, div.tags a").map { it.text().trim() }

        val isSeries = url.contains("/dizi/")

        return if (isSeries) {
            val episodes = mutableListOf<Episode>()
            doc.select("div.episode, li.bolum, a.episode-link").forEachIndexed { index, ep ->
                val epHref = ep.attr("href")
                val epName = ep.text().trim().ifEmpty { "${index + 1}. Bölüm" }
                val seasonNum = ep.attr("data-season").toIntOrNull() ?: 1
                val epNum = ep.attr("data-episode").toIntOrNull() ?: (index + 1)

                episodes.add(
                    newEpisode(epHref) {
                        this.name = epName
                        this.season = seasonNum
                        this.episode = epNum
                    }
                )
            }

            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
                this.rating = rating
                this.tags = tags
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.plot = description
                this.year = year
                this.rating = rating
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
