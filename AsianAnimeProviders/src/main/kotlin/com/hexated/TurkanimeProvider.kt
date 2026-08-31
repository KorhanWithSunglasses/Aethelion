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

class TurkanimeProvider : MainAPI() {
    override var name = "TürkAnime"
    override var mainUrl = "https://www.turkanime.tv"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Anime, TvType.AnimeMovie, TvType.OVA)

    private val fallbackDomains = listOf(
        "https://www.turkanime.tv",
        "https://turkanime.tv",
        "https://www.turkanime.co"
    )

    private suspend fun getUrl(): String {
        return DynamicDomainHelper.getActiveDomain("turkanime", fallbackDomains)
    }

    override val mainPage = mainPageOf(
        "" to "Son Eklenen Bölümler",
        "anime-listesi" to "Anime Listesi",
        "populer-animeler" to "Popüler Animeler",
        "tur/aksiyon" to "Aksiyon",
        "tur/macera" to "Macera",
        "tur/fantastik" to "Fantastik"
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
            val home = doc.select("div.panel-body a, a[href*=\"/anime/\"], div.anime-card, div.box, article").mapNotNull {
                it.toSearchResult(domain)
            }.distinctBy { it.url }
            newHomePageResponse(request.name, home)
        } catch (_: Exception) {
            newHomePageResponse(request.name, emptyList())
        }
    }

    private fun Element.toSearchResult(domain: String): SearchResponse? {
        val rawHref = this.attr("href").ifEmpty { this.selectFirst("a")?.attr("href") } ?: return null
        if (!rawHref.contains("/anime/") && !rawHref.contains("/video/")) return null

        val href = when {
            rawHref.startsWith("//") -> "https:$rawHref"
            rawHref.startsWith("http") -> rawHref
            else -> "$domain$rawHref"
        }

        val title = this.selectFirst("h4, h3, .title, a, img[alt]")?.let {
            if (it.tagName() == "img") it.attr("alt") else it.text()
        }?.trim()?.ifEmpty { null } ?: this.text().trim().ifEmpty { null } ?: return null

        val posterUrl = this.selectFirst("img")?.let {
            val src = it.attr("data-src").ifEmpty { it.attr("src") }
            if (src.startsWith("//")) "https:$src" else src
        }

        return newAnimeSearchResponse(title, href, TvType.Anime) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val domain = getUrl()
        val searchUrl = "$domain/arama?arama=$query"
        return try {
            val doc = app.get(searchUrl, headers = NetworkHelper.defaultHeaders).document
            doc.select("a[href*=\"/anime/\"], div.panel-body a").mapNotNull {
                it.toSearchResult(domain)
            }.distinctBy { it.url }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val domain = getUrl()
        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document

        val title = doc.selectFirst("h1, .panel-title, .title, img[alt]")?.let {
            if (it.tagName() == "img") it.attr("alt") else it.text()
        }?.trim() ?: "Anime"

        val poster = doc.selectFirst("div.panel-body img, meta[property=og:image], img.img-responsive")?.let {
            val src = it.attr("data-src").ifEmpty { it.attr("src") }.ifEmpty { it.attr("content") }
            if (src.startsWith("//")) "https:$src" else src
        }
        val description = doc.selectFirst("div.panel-body p, .summary, .description")?.text()?.trim()
        val year = doc.selectFirst(".year, .date")?.text()?.filter { it.isDigit() }?.toIntOrNull()
        val tags = doc.select("div.panel-body a[href*=tur/]").map { it.text().trim() }

        val episodes = mutableListOf<Episode>()
        doc.select("div.bolumler a, ul.episodes li a, a[href*=\"/video/\"], a[href*=bolum]").forEachIndexed { index, epLink ->
            val rawEpHref = epLink.attr("href")
            val epHref = if (rawEpHref.startsWith("//")) "https:$rawEpHref" else if (rawEpHref.startsWith("http")) rawEpHref else "$domain$rawEpHref"
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

        val iframes = mutableListOf<String>()
        doc.select("iframe").forEach {
            val src = it.attr("src").ifEmpty { it.attr("data-src") }
            if (src.isNotEmpty()) iframes.add(src)
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
                else -> ExtractorHelper.resolveStream(cleanUrl, data, "TürkAnime", subtitleCallback, callback)
            }
        }

        return true
    }
}
