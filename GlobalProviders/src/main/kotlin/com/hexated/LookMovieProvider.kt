@file:Suppress("DEPRECATION")
package com.hexated

import com.hexated.core.DynamicDomainHelper
import com.hexated.core.ExtractorHelper
import com.hexated.core.NetworkHelper
import com.hexated.extractors.CloseLoad
import com.hexated.extractors.Streamwish
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import org.jsoup.nodes.Element

class LookMovieProvider : MainAPI() {
    override var name = "LookMovie"
    override var mainUrl = "https://lookmovie2.to"
    override var lang = "en"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    private val fallbackDomains = listOf(
        "https://lookmovie2.to",
        "https://lookmovie.la",
        "https://lookmovie.ag"
    )

    private suspend fun getUrl(): String {
        return DynamicDomainHelper.getActiveDomain("lookmovie", fallbackDomains)
    }

    override val mainPage = mainPageOf(
        "" to "Latest Movies",
        "shows" to "Latest TV Shows",
        "movies/popular" to "Popular Movies",
        "shows/popular" to "Popular TV Shows",
        "movies/trending" to "Trending Movies"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val domain = getUrl()
        val url = if (page <= 1) {
            if (request.data.isEmpty()) domain else "$domain/${request.data}"
        } else {
            "$domain/${request.data}?page=$page"
        }

        return try {
            val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document
            val home = doc.select("div.movie-item, div.item, article, div.content-item, div.poster").mapNotNull {
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

        val title = this.selectFirst("h3.title, .movie-title, .title, a, img[alt]")?.let {
            if (it.tagName() == "img") it.attr("alt") else it.text()
        }?.trim()?.ifEmpty { null } ?: this.text().trim().ifEmpty { null } ?: return null

        val posterUrl = this.selectFirst("img")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }.ifEmpty { it.attr("data-lazy-src") }
        }

        val isSeries = href.contains("/shows/")
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
        val searchUrl = "$domain/search?q=$query"
        return try {
            val doc = app.get(searchUrl, headers = NetworkHelper.defaultHeaders).document
            doc.select("div.movie-item, div.item, article, div.search-result").mapNotNull {
                it.toSearchResult(domain)
            }.distinctBy { it.url }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val domain = getUrl()
        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document

        val title = doc.selectFirst("h1, .movie-title, .title")?.text()?.trim() ?: "Movie"
        val poster = doc.selectFirst("div.poster img, .movie-poster img, meta[property=og:image]")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }.ifEmpty { it.attr("content") }
        }
        val description = doc.selectFirst("div.description, div.overview, p.story")?.text()?.trim()
        val year = doc.selectFirst(".year, .release-year")?.text()?.filter { it.isDigit() }?.toIntOrNull()
        val tags = doc.select("div.genres a, .tags a").map { it.text().trim() }

        val isSeries = url.contains("/shows/")

        return if (isSeries) {
            val episodes = mutableListOf<Episode>()
            doc.select("div.episodes a, ul.episodes-list li a, a.episode-btn").forEachIndexed { index, epLink ->
                val epHref = epLink.attr("href").let { if (it.startsWith("http")) it else "$domain$it" }
                val epTitle = epLink.text().trim().ifEmpty { "Episode ${index + 1}" }
                val epNum = Regex("""(?:ep|episode)\s*(\d+)""", RegexOption.IGNORE_CASE).find(epTitle)?.groupValues?.get(1)?.toIntOrNull() ?: (index + 1)
                val seasonNum = Regex("""(?:s|season)\s*(\d+)""", RegexOption.IGNORE_CASE).find(epTitle)?.groupValues?.get(1)?.toIntOrNull() ?: 1

                episodes.add(
                    newEpisode(epHref) {
                        this.name = epTitle
                        this.season = seasonNum
                        this.episode = epNum
                        this.posterUrl = poster
                    }
                )
            }

            if (episodes.isEmpty()) {
                episodes.add(newEpisode(url) {
                    this.name = "Episode 1"
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
            val subLang = track.attr("label").ifEmpty { "English" }
            if (subSrc.isNotEmpty()) {
                subtitleCallback.invoke(SubtitleFile(lang = subLang, url = subSrc))
            }
        }

        val iframes = mutableListOf<String>()
        doc.select("iframe").forEach {
            val src = it.attr("src").ifEmpty { it.attr("data-src") }
            if (src.isNotEmpty()) iframes.add(src)
        }

        doc.select("div.player-tabs button, div.sources a, button[data-src]").forEach { tab ->
            val src = tab.attr("data-src").ifEmpty { tab.attr("data-url") }.ifEmpty { tab.attr("href") }
            if (src.isNotEmpty() && !src.startsWith("#")) iframes.add(src)
        }

        val closeLoad = CloseLoad()
        val streamwish = Streamwish()

        iframes.distinct().forEach { rawUrl ->
            val cleanUrl = if (rawUrl.startsWith("//")) "https:$rawUrl" else rawUrl

            when {
                cleanUrl.contains("streamwish") -> streamwish.getUrl(cleanUrl, data, subtitleCallback, callback)
                cleanUrl.contains("closeload") -> closeLoad.getUrl(cleanUrl, data, subtitleCallback, callback)
                else -> ExtractorHelper.resolveStream(cleanUrl, data, "LookMovie", subtitleCallback, callback)
            }
        }

        return true
    }
}
