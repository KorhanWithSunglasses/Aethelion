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

class DiziBoxProvider : MainAPI() {
    override var name = "DiziBox"
    override var mainUrl = "https://www.dizibox.live"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.TvSeries)

    override var sequentialMainPage = true
    override var sequentialMainPageDelay = 50L
    override var sequentialMainPageScrollDelay = 50L

    private val authCookies = mapOf(
        "LockUser" to "true",
        "isTrustedUser" to "true",
        "dbxu" to "1722403730363"
    )

    private val fallbackDomains = listOf(
        "https://www.dizibox.live",
        "https://www.dizibox.net",
        "https://www.dizibox.pw",
        "https://www.dizibox.tv"
    )

    private suspend fun getUrl(): String {
        return DynamicDomainHelper.getActiveDomain("dizibox", fallbackDomains)
    }

    override val mainPage = mainPageOf(
        "arsiv" to "Dizi Arşivi",
        "arsiv/?tur[0]=aksiyon" to "Aksiyon",
        "arsiv/?tur[0]=bilimkurgu" to "Bilim Kurgu",
        "arsiv/?tur[0]=komedi" to "Komedi",
        "arsiv/?tur[0]=dram" to "Dram",
        "arsiv/?tur[0]=korku" to "Korku",
        "arsiv/?tur[0]=animasyon" to "Animasyon",
        "arsiv/?ulke[]=turkiye" to "Yerli Diziler"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val domain = getUrl()
        val url = if (page <= 1) {
            if (request.data.isEmpty()) "$domain/arsiv" else "$domain/${request.data}"
        } else {
            if (request.data.contains("?")) {
                val parts = request.data.split("?")
                "$domain/${parts[0]}/page/$page/?${parts[1]}"
            } else {
                "$domain/${request.data}/page/$page/"
            }
        }

        return try {
            val doc = app.get(
                url,
                cookies = authCookies,
                headers = NetworkHelper.defaultHeaders
            ).document

            val home = doc.select("div.detailed-article, article, div.post, a[href*=\"/diziler/\"], a[href*=\"/dizi/\"]").mapNotNull {
                it.toSearchResult(domain)
            }.distinctBy { it.url }

            newHomePageResponse(request.name, home)
        } catch (_: Exception) {
            newHomePageResponse(request.name, emptyList())
        }
    }

    private fun Element.toSearchResult(domain: String): SearchResponse? {
        val rawHref = this.attr("href").ifEmpty { this.selectFirst("a")?.attr("href") } ?: return null
        if (rawHref.contains("javascript:") || rawHref.startsWith("#") || rawHref.contains("wp-login") || rawHref.contains("yardim")) return null
        val href = if (rawHref.startsWith("http")) rawHref else "$domain$rawHref"

        val title = this.selectFirst("h2, h3, .title, .post-title, img[alt]")?.let {
            if (it.tagName() == "img") it.attr("alt") else it.text()
        }?.trim()?.ifEmpty { null } ?: this.text().trim().ifEmpty { null } ?: return null

        val posterUrl = this.selectFirst("img")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }.ifEmpty { it.attr("data-lazy-src") }
        }

        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val domain = getUrl()
        val searchUrl = "$domain/?s=$query"
        return try {
            val doc = app.get(
                searchUrl,
                cookies = authCookies,
                headers = NetworkHelper.defaultHeaders
            ).document

            doc.select("div.detailed-article, article, div.post, a[href*=\"/diziler/\"], a[href*=\"/dizi/\"]").mapNotNull {
                it.toSearchResult(domain)
            }.distinctBy { it.url }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val domain = getUrl()
        val doc = app.get(
            url,
            cookies = authCookies,
            headers = NetworkHelper.defaultHeaders
        ).document

        val title = doc.selectFirst("h1, .tv-overview h1, .title")?.text()?.trim() ?: "Dizi"
        val poster = doc.selectFirst("div.tv-overview figure img, div.poster img, meta[property=og:image]")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }.ifEmpty { it.attr("content") }
        }
        val description = doc.selectFirst("div.tv-story p, div.overview, div.description")?.text()?.trim()
        val year = doc.selectFirst(".year, .date")?.text()?.filter { it.isDigit() }?.toIntOrNull()
        val tags = doc.select("div.genres a, .tags a, div.tv-extra a").map { it.text().trim() }

        val episodes = mutableListOf<Episode>()
        doc.select("ul.episodes li a, div.episodes a, a[href*=\"-sezon-\"], a[href*=\"-bolum\"]").forEachIndexed { index, epLink ->
            val epHref = epLink.attr("href").let { if (it.startsWith("http")) it else "$domain$it" }
            val epText = epLink.text().trim().ifEmpty { "${index + 1}. Bölüm" }

            val seasonNum = Regex("""(?:sezon|s)-?(\d+)""", RegexOption.IGNORE_CASE).find(epHref)?.groupValues?.get(1)?.toIntOrNull()
                ?: Regex("""(\d+)\.\s*Sezon""").find(epText)?.groupValues?.get(1)?.toIntOrNull()
                ?: 1

            val epNum = Regex("""(?:bolum|ep)-?(\d+)""", RegexOption.IGNORE_CASE).find(epHref)?.groupValues?.get(1)?.toIntOrNull()
                ?: Regex("""(\d+)\.\s*Bölüm""").find(epText)?.groupValues?.get(1)?.toIntOrNull()
                ?: (index + 1)

            episodes.add(
                newEpisode(epHref) {
                    this.name = epText
                    this.season = seasonNum
                    this.episode = epNum
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

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
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
        val doc = app.get(
            data,
            cookies = authCookies,
            headers = NetworkHelper.defaultHeaders
        ).document

        val iframes = mutableListOf<String>()
        doc.select("iframe").forEach {
            val src = it.attr("src").ifEmpty { it.attr("data-src") }
            if (src.isNotEmpty()) iframes.add(src)
        }

        doc.select("div.video-player iframe, div.player iframe, div.source-item a").forEach { el ->
            val src = el.attr("data-src").ifEmpty { el.attr("src") }.ifEmpty { el.attr("href") }
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
                else -> ExtractorHelper.resolveStream(cleanUrl, data, "DiziBox", subtitleCallback, callback)
            }
        }

        return true
    }
}
