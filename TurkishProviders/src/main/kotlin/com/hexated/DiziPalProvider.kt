package com.hexated

import com.hexated.core.DynamicDomainHelper
import com.hexated.core.ExtractorHelper
import com.hexated.core.NetworkHelper
import com.hexated.core.NextDataHelper
import com.hexated.extractors.CloseLoad
import com.hexated.extractors.Rapidame
import com.hexated.extractors.Streamwish
import com.hexated.extractors.Vidmoly
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class DiziPalProvider : MainAPI() {
    override var name = "DiziPal"
    override var mainUrl = "https://dizipal1577.com"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    private val fallbackDomains = listOf(
        "https://dizipal1577.com",
        "https://dizipal2119.com",
        "https://dizipal824.org",
        "https://dizipal.site"
    )

    private suspend fun getUrl(): String {
        return DynamicDomainHelper.getActiveDomain("dizipal", fallbackDomains)
    }

    override val mainPage = mainPageOf(
        "" to "Son Eklenenler",
        "filmler" to "Filmler",
        "diziler" to "Diziler",
        "tur/yerli-filmler" to "Yerli Filmler",
        "tur/netflix" to "Netflix İçerikleri",
        "tur/trend" to "Trend Yapımlar"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val domain = getUrl()
        val url = if (page <= 1) {
            if (request.data.isEmpty()) domain else "$domain/${request.data}"
        } else {
            "$domain/${request.data}/page/$page/"
        }

        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document

        // Try NextData JSON first
        val props = NextDataHelper.getProps(doc)
        val homeList = mutableListOf<SearchResponse>()

        props?.get("posts")?.forEach { post ->
            val title = post.get("title")?.asText() ?: post.get("name")?.asText() ?: return@forEach
            val slug = post.get("slug")?.asText() ?: ""
            val isSeries = post.get("isSeries")?.asBoolean() ?: (post.get("type")?.asText() == "series") || slug.contains("dizi")
            val href = if (slug.startsWith("http")) slug else "$domain/$slug"
            val poster = post.get("poster")?.asText() ?: post.get("image")?.asText()

            if (isSeries) {
                homeList.add(newTvSeriesSearchResponse(title, href, TvType.TvSeries) { this.posterUrl = poster })
            } else {
                homeList.add(newMovieSearchResponse(title, href, TvType.Movie) { this.posterUrl = poster })
            }
        }

        // HTML DOM fallback
        if (homeList.isEmpty()) {
            doc.select("div.poster, div.movie-card, article, div.film, div.content-item").forEach { el ->
                el.toSearchResult(domain)?.let { homeList.add(it) }
            }
        }

        return newHomePageResponse(request.name, homeList)
    }

    private fun Element.toSearchResult(domain: String): SearchResponse? {
        val title = this.selectFirst("h2, h3, .title, .poster-title, .name, a")?.text()?.trim() ?: return null
        val rawHref = this.selectFirst("a")?.attr("href") ?: return null
        val href = if (rawHref.startsWith("http")) rawHref else "$domain$rawHref"
        val posterUrl = this.selectFirst("img")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }.ifEmpty { it.attr("data-lazy-src") }
        }

        val isSeries = href.contains("/dizi/") || this.selectFirst(".is-series, .badge-series, .series") != null
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

        val searchList = mutableListOf<SearchResponse>()
        doc.select("div.poster, div.movie-card, article, div.search-result, div.film").forEach { el ->
            el.toSearchResult(domain)?.let { searchList.add(it) }
        }

        return searchList
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document
        val domain = getUrl()

        val title = doc.selectFirst("h1, .movie-title, .title, .name")?.text()?.trim() ?: "İçerik"
        val poster = doc.selectFirst("div.poster img, .movie-poster img, meta[property=og:image], .cover img")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }.ifEmpty { it.attr("content") }
        }
        val description = doc.selectFirst("div.description, div.overview, p.story, div.summary, .plot")?.text()?.trim()
        val year = doc.selectFirst(".year, .release-date, .date")?.text()?.filter { it.isDigit() }?.toIntOrNull()
        val tags = doc.select("div.genres a, div.tags a, .genre a").map { it.text().trim() }

        val isSeries = url.contains("/dizi/") || doc.select(".season-list, .episodes, .episode-item, .seasons, ul.bolumler").isNotEmpty()

        return if (isSeries) {
            val episodes = mutableListOf<Episode>()

            // 1. Check __NEXT_DATA__ for seasons & episodes
            val props = NextDataHelper.getProps(doc)
            props?.get("seasons")?.forEach { seasonNode ->
                val seasonNum = seasonNode.get("season_number")?.asInt() ?: 1
                seasonNode.get("episodes")?.forEach { epNode ->
                    val epNum = epNode.get("episode_number")?.asInt() ?: 1
                    val epName = epNode.get("name")?.asText() ?: "Bölüm $epNum"
                    val epSlug = epNode.get("slug")?.asText() ?: ""
                    val epUrl = if (epSlug.startsWith("http")) epSlug else "$domain/$epSlug"
                    val epPoster = epNode.get("image")?.asText() ?: poster

                    episodes.add(
                        newEpisode(epUrl) {
                            this.name = epName
                            this.season = seasonNum
                            this.episode = epNum
                            this.posterUrl = epPoster
                        }
                    )
                }
            }

            // 2. HTML DOM Fallback for Seasons and Episodes
            if (episodes.isEmpty()) {
                doc.select(".episode-item, .episodes a, ul.bolumler li a, div.tab-content a").forEachIndexed { index, epLink ->
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
            }

            // Fallback single episode if list is empty
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

        // Collect all iframes and video player tabs
        val iframes = mutableListOf<String>()

        doc.select("iframe").forEach { iframe ->
            val src = iframe.attr("src").ifEmpty { iframe.attr("data-src") }
            if (src.isNotEmpty()) iframes.add(src)
        }

        doc.select("div.player-tabs button, div.sources a, button[data-src]").forEach { tab ->
            val src = tab.attr("data-src").ifEmpty { tab.attr("data-url") }.ifEmpty { tab.attr("href") }
            if (src.isNotEmpty()) iframes.add(src)
        }

        // Custom Extractor Instances
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
                else -> ExtractorHelper.resolveStream(cleanUrl, data, "DiziPal Oynatıcı", subtitleCallback, callback)
            }
        }

        return true
    }
}
