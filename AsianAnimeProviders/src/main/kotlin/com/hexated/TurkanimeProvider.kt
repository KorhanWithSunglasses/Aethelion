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

class TurkanimeProvider : MainAPI() {
    override var name = "Turkanime"
    override var mainUrl = "https://www.turkanime.co"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Anime, TvType.AnimeMovie)

    private val fallbackDomains = listOf(
        "https://www.turkanime.co",
        "https://www.turkanime.net",
        "https://www.turkanime.tv"
    )

    private suspend fun getUrl(): String {
        return DynamicDomainHelper.getActiveDomain("turkanime", fallbackDomains)
    }

    override val mainPage = mainPageOf(
        "" to "Son Eklenen Bölümler",
        "anime/populer" to "Popüler Animeler",
        "anime/filmler" to "Anime Filmleri"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val domain = getUrl()
        val url = if (page <= 1) {
            "$domain/${request.data}"
        } else {
            "$domain/${request.data}?sayfa=$page"
        }

        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document
        val home = doc.select("div.panel-body div.col-md-3, div.anime-kutu, div.poster, article").mapNotNull {
            it.toSearchResult()
        }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h2, h3, .panel-title, .anime-adi, a")?.text()?.trim() ?: return null
        val href = this.selectFirst("a")?.attr("href") ?: return null
        val posterUrl = this.selectFirst("img")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }
        }

        val isMovie = href.contains("-film")
        val type = if (isMovie) TvType.AnimeMovie else TvType.Anime

        return if (type == TvType.AnimeMovie) {
            newMovieSearchResponse(title, href, type) {
                this.posterUrl = posterUrl
            }
        } else {
            newAnimeSearchResponse(title, href, type) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val domain = getUrl()
        val searchUrl = "$domain/arama?q=$query"
        val doc = app.get(searchUrl, headers = NetworkHelper.defaultHeaders).document

        return doc.select("div.panel-body div.col-md-3, div.anime-kutu, div.search-result").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document

        val title = doc.selectFirst("h1, .panel-title, .anime-title")?.text()?.trim() ?: "Bilinmeyen Anime"
        val poster = doc.selectFirst("div.panel-body img.img-responsive, .anime-afis img, meta[property=og:image]")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }.ifEmpty { it.attr("content") }
        }
        val description = doc.selectFirst("div.ozet, div.panel-body p, .story")?.text()?.trim()
        val year = doc.selectFirst(".yil, .release-year")?.text()?.filter { it.isDigit() }?.toIntOrNull()
        
        val tags = doc.select("div.turler a, div.genres a").map { it.text().trim() }

        val episodes = mutableListOf<Episode>()
        doc.select("div#bolumler a, li.bolum, a.bolum-link, div.episode-list a").forEachIndexed { index, ep ->
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

        return newAnimeLoadResponse(title, url, TvType.Anime) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            
            this.tags = tags
            this.episodes = mutableMapOf(DubStatus.Subbed to episodes)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data, headers = NetworkHelper.defaultHeaders).document

        val iframes = doc.select("iframe, div#videoplay iframe, div.player iframe").mapNotNull {
            it.attr("data-src").ifEmpty { it.attr("src") }.takeIf { src -> src.isNotEmpty() }
        }.toMutableList()

        doc.select("div.videolar a, button.video-btn, a.dropdown-item").forEach { btn ->
            val playerUrl = btn.attr("data-url").ifEmpty { btn.attr("data-src") }.ifEmpty { btn.attr("href") }
            if (playerUrl.startsWith("http") || playerUrl.startsWith("//")) {
                iframes.add(if (playerUrl.startsWith("//")) "https:$playerUrl" else playerUrl)
            }
        }

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
