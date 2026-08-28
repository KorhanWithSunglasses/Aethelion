package com.hexated

import com.hexated.core.ExtractorHelper

import com.hexated.core.DynamicDomainHelper
import com.hexated.core.NetworkHelper
import com.hexated.extractors.CloseLoad
import com.hexated.extractors.Streamwish
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class SFlixProvider : MainAPI() {
    override var name = "SFlix"
    override var mainUrl = "https://sflix.to"
    override var lang = "en"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    private val fallbackDomains = listOf(
        "https://sflix.to",
        "https://sflix.se",
        "https://sflix.is"
    )

    private suspend fun getUrl(): String {
        return DynamicDomainHelper.getActiveDomain("sflix", fallbackDomains)
    }

    override val mainPage = mainPageOf(
        "" to "Home",
        "movie" to "Movies",
        "tv-show" to "TV Series",
        "top-imdb" to "Top IMDb"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val domain = getUrl()
        val url = if (page <= 1) {
            "$domain/${request.data}"
        } else {
            "$domain/${request.data}?page=$page"
        }

        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document
        val home = doc.select("div.flw-item").mapNotNull {
            it.toSearchResult()
        }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h2.film-name, .film-name a")?.text()?.trim() ?: return null
        val href = this.selectFirst("a.film-poster-ahref, a")?.attr("href") ?: return null
        val posterUrl = this.selectFirst("img.film-poster-img, img")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }
        }

        val isSeries = href.contains("/tv/") || this.selectFirst("span.fdi-type")?.text()?.contains("TV", ignoreCase = true) == true
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
        val doc = app.get(searchUrl, headers = NetworkHelper.defaultHeaders).document

        return doc.select("div.flw-item").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document

        val title = doc.selectFirst("h2.heading-name, h1")?.text()?.trim() ?: "Unknown Title"
        val poster = doc.selectFirst("div.film-poster img, meta[property=og:image]")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }.ifEmpty { it.attr("content") }
        }
        val description = doc.selectFirst("div.description, div.overview")?.text()?.trim()
        val year = doc.selectFirst(".item-year, .year")?.text()?.filter { it.isDigit() }?.toIntOrNull()
        
        val tags = doc.select("div.item-genres a").map { it.text().trim() }

        val isSeries = url.contains("/tv/")

        return if (isSeries) {
            val episodes = mutableListOf<Episode>()
            doc.select("div.eps-item, a.eps-item").forEachIndexed { index, ep ->
                val epHref = ep.attr("href")
                val epName = ep.attr("title").ifEmpty { "${index + 1}. Episode" }
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

        val iframes = doc.select("iframe, div#iframe-embed iframe").mapNotNull {
            it.attr("src").takeIf { src -> src.isNotEmpty() }
        }.toMutableList()

        for (iframeUrl in iframes.distinct()) {
            val cleanUrl = if (iframeUrl.startsWith("//")) "https:$iframeUrl" else iframeUrl

            when {
                cleanUrl.contains("streamwish") -> Streamwish().getUrl(cleanUrl, data, subtitleCallback, callback)
                cleanUrl.contains("closeload") -> CloseLoad().getUrl(cleanUrl, data, subtitleCallback, callback)
                else -> ExtractorHelper.resolveStream(cleanUrl, data, name, subtitleCallback, callback)
            }
        }

        return true
    }
}
