@file:Suppress("DEPRECATION")
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

class AnimecixProvider : MainAPI() {
    override var name = "Animecix"
    override var mainUrl = "https://animecix.tv"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Anime, TvType.AnimeMovie, TvType.OVA)

    private val fallbackDomains = listOf(
        "https://animecix.tv",
        "https://animecix.com",
        "https://animecix.net"
    )

    private suspend fun getUrl(): String {
        return DynamicDomainHelper.getActiveDomain("animecix", fallbackDomains)
    }

    override val mainPage = mainPageOf(
        "" to "Son Eklenen Animeler",
        "turler/aksiyon" to "Aksiyon Animeleri",
        "turler/macera" to "Macera Animeleri",
        "turler/fantastik" to "Fantastik",
        "turler/komedi" to "Komedi",
        "turler/romantizm" to "Romantizm"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val domain = getUrl()
        val url = if (page <= 1) {
            if (request.data.isEmpty()) domain else "$domain/${request.data}"
        } else {
            "$domain/${request.data}?sayfa=$page"
        }

        return try {
            val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document
            val home = doc.select("div.anime-card, div.poster, article, div.content-item, div.film").mapNotNull {
                it.toSearchResult(domain)
            }
            newHomePageResponse(request.name, home)
        } catch (_: Exception) {
            newHomePageResponse(request.name, emptyList())
        }
    }

    private fun Element.toSearchResult(domain: String): SearchResponse? {
        val title = this.selectFirst("h2, h3, .anime-title, .title, a")?.text()?.trim() ?: return null
        val rawHref = this.selectFirst("a")?.attr("href") ?: return null
        val href = if (rawHref.startsWith("http")) rawHref else "$domain$rawHref"
        val posterUrl = this.selectFirst("img")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }.ifEmpty { it.attr("data-lazy-src") }
        }

        return newAnimeSearchResponse(title, href, TvType.Anime) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val domain = getUrl()
        val searchUrl = "$domain/arama?q=$query"
        return try {
            val doc = app.get(searchUrl, headers = NetworkHelper.defaultHeaders).document
            doc.select("div.anime-card, div.poster, article, div.search-result").mapNotNull {
                it.toSearchResult(domain)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val domain = getUrl()
        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document

        val title = doc.selectFirst("h1, .anime-title, .title")?.text()?.trim() ?: "Anime"
        val poster = doc.selectFirst("div.poster img, .anime-poster img, meta[property=og:image], .cover img")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }.ifEmpty { it.attr("content") }
        }
        val description = doc.selectFirst("div.description, div.overview, p.story, div.summary, .anime-desc")?.text()?.trim()
        val year = doc.selectFirst(".year, .release-date, .date")?.text()?.filter { it.isDigit() }?.toIntOrNull()
        val tags = doc.select("div.genres a, div.tags a, .genre a").map { it.text().trim() }

        val episodes = mutableListOf<Episode>()

        doc.select("div.episodes-list a, ul.bolumler li a, div.episode-item a, div.tab-content a").forEachIndexed { index, epLink ->
            val epHref = epLink.attr("href").let { if (it.startsWith("http")) it else "$domain$it" }
            val epTitle = epLink.text().trim().ifEmpty { "${index + 1}. Bölüm" }
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

        doc.select("track[kind=subtitles], track[kind=captions]").forEach { track ->
            val subSrc = track.attr("src")
            val subLang = track.attr("label").ifEmpty { "Türkçe" }
            if (subSrc.isNotEmpty()) {
                subtitleCallback.invoke(SubtitleFile(lang = subLang, url = subSrc))
            }
        }

        val iframes = mutableListOf<String>()

        doc.select("iframe").forEach { iframe ->
            val src = iframe.attr("src").ifEmpty { iframe.attr("data-src") }
            if (src.isNotEmpty()) iframes.add(src)
        }

        doc.select("div.player-tabs button, div.sources a, button[data-src], div.video-players a").forEach { tab ->
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
                else -> ExtractorHelper.resolveStream(cleanUrl, data, "Animecix", subtitleCallback, callback)
            }
        }

        return true
    }
}
