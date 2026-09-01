@file:Suppress("DEPRECATION")
package com.hexated

import com.hexated.core.DiziPalCryptoHelper
import com.hexated.core.DynamicDomainHelper
import com.hexated.core.ExtractorHelper
import com.hexated.core.ImageHelper
import com.hexated.core.NetworkHelper
import com.hexated.extractors.CloseLoad
import com.hexated.extractors.Rapidame
import com.hexated.extractors.Streamwish
import com.hexated.extractors.Vidmoly
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink
import org.jsoup.nodes.Element

class DiziPalProvider : MainAPI() {
    override var name = "DiziPal"
    override var mainUrl = "https://dizipalw.com"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.TvSeries, TvType.Movie)

    // 500ms delay prevents 429 Too Many Requests rate limiting from Cloudflare WAF
    override var sequentialMainPage = true
    override var sequentialMainPageDelay = 500L
    override var sequentialMainPageScrollDelay = 500L

    private val fallbackDomains = listOf(
        "https://dizipalw.com",
        "https://dizipal.site",
        "https://dizipal105.vip",
        "https://dizipal.me"
    )

    private suspend fun getUrl(): String {
        return DynamicDomainHelper.getActiveDomain("dizipal", fallbackDomains)
    }

    override val mainPage = mainPageOf(
        "trendler" to "Trend Yapımlar",
        "diziler" to "Yeni Diziler",
        "filmler" to "Yeni Filmler",
        "anime" to "Animeler"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val domain = getUrl()
        val url = if (page <= 1) {
            "$domain/${request.data}"
        } else {
            "$domain/${request.data}/page/$page/"
        }

        return try {
            val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document

            val home = doc.select("a[href*=\"/dizi/\"], a[href*=\"/film/\"], div.item, div.movie-item, article").mapNotNull {
                it.toSearchResult(domain)
            }.distinctBy { it.url }

            newHomePageResponse(request.name, home)
        } catch (_: Exception) {
            newHomePageResponse(request.name, emptyList())
        }
    }

    private fun Element.toSearchResult(domain: String): SearchResponse? {
        val rawHref = this.attr("href").ifEmpty { this.selectFirst("a")?.attr("href") } ?: return null
        if (rawHref.contains("javascript:") || rawHref.startsWith("#") || rawHref.contains("wp-login") || rawHref.contains("uye")) return null
        val href = if (rawHref.startsWith("http")) rawHref else "$domain$rawHref"

        val title = this.selectFirst("h2, h3, .title, .film-title, img[alt]")?.let {
            if (it.tagName() == "img") it.attr("alt") else it.text()
        }?.trim()?.ifEmpty { null } ?: this.text().trim().ifEmpty { null } ?: return null

        val posterUrl = ImageHelper.extractPosterUrl(this, domain)

        val isSeries = href.contains("/dizi/") || href.contains("sezon")
        val type = if (isSeries) TvType.TvSeries else TvType.Movie

        return if (type == TvType.TvSeries) {
            newTvSeriesSearchResponse(title.lines().first().trim(), href, type) {
                this.posterUrl = posterUrl
            }
        } else {
            newMovieSearchResponse(title.lines().first().trim(), href, type) {
                this.posterUrl = posterUrl
            }
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val domain = getUrl()
        val searchUrl = "$domain/?s=$query"
        return try {
            val doc = app.get(searchUrl, headers = NetworkHelper.defaultHeaders).document
            doc.select("a[href*=\"/dizi/\"], a[href*=\"/film/\"], div.item, div.search-result").mapNotNull {
                it.toSearchResult(domain)
            }.distinctBy { it.url }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val domain = getUrl()
        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document

        val title = doc.selectFirst("h1, .entry-title, .title")?.text()?.trim() ?: "İçerik"
        val poster = doc.selectFirst("div.poster img, .post-thumbnail img, meta[property=og:image], img[src*=\"poster\"], img[src*=\"back\"]")?.let { img ->
            val dataSrc = img.attr("data-src").ifEmpty { img.attr("data-srcset") }.ifEmpty { img.attr("data-lazy-src") }
            val src = img.attr("src")
            val og = img.attr("content")
            if (dataSrc.isNotEmpty() && !dataSrc.startsWith("data:")) dataSrc
            else if (src.isNotEmpty() && !src.startsWith("data:")) src
            else if (og.isNotEmpty()) og
            else null
        }
        val description = doc.selectFirst("div.overview, div.description, .entry-content p, p")?.text()?.trim()
        val year = doc.selectFirst(".year, .date")?.text()?.filter { it.isDigit() }?.toIntOrNull()
        val tags = doc.select("div.genres a, .tags a").map { it.text().trim() }

        val isSeries = url.contains("/dizi/") || doc.select("ul.episodes, div.episodes, a[href*=bolum]").isNotEmpty()

        return if (isSeries) {
            val episodes = mutableListOf<Episode>()
            doc.select("ul.episodes li a, div.episodes a, a[href*=\"-sezon-\"], a[href*=\"-bolum\"], a[href*=\"/bolum/\"]").forEachIndexed { index, epLink ->
                val epHref = epLink.attr("href").let { if (it.startsWith("http")) it else "$domain$it" }
                val epName = epLink.text().trim().ifEmpty { "${index + 1}. Bölüm" }
                val seasonNum = Regex("""(?:sezon|s)-?(\d+)""", RegexOption.IGNORE_CASE).find(epHref)?.groupValues?.get(1)?.toIntOrNull() ?: 1
                val epNum = Regex("""(?:bolum|ep)-?(\d+)""", RegexOption.IGNORE_CASE).find(epHref)?.groupValues?.get(1)?.toIntOrNull() ?: (index + 1)

                episodes.add(
                    newEpisode(epHref) {
                        this.name = epName
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
        val domain = getUrl()
        val doc = app.get(data, headers = NetworkHelper.defaultHeaders).document

        val iframes = mutableListOf<String>()

        // 1. Decrypt DiziPal data-rm-k encrypted player
        val encryptedData = doc.selectFirst("div[data-rm-k]")?.text()?.trim()
        if (!encryptedData.isNullOrEmpty()) {
            val decryptedUrl = DiziPalCryptoHelper.decrypt(encryptedData)
            if (!decryptedUrl.isNullOrEmpty()) {
                val cleanDecrypted = if (decryptedUrl.startsWith("//")) "https:$decryptedUrl" else decryptedUrl
                iframes.add(cleanDecrypted)
            }
        }

        // 2. Collect visible iframes
        doc.select("iframe").forEach {
            val src = it.attr("src").ifEmpty { it.attr("data-src") }
            if (src.isNotEmpty()) iframes.add(src)
        }

        doc.select("div.player-tabs button, div.sources a, button[data-src], div.video-players a, div.server a").forEach { tab ->
            val src = tab.attr("data-src").ifEmpty { tab.attr("data-url") }.ifEmpty { tab.attr("href") }
            if (src.isNotEmpty() && !src.startsWith("#")) iframes.add(src)
        }

        val vidmoly = Vidmoly()
        val rapidame = Rapidame()
        val streamwish = Streamwish()
        val closeLoad = CloseLoad()

        iframes.distinct().forEach { rawUrl ->
            val cleanUrl = if (rawUrl.startsWith("//")) "https:$rawUrl" else rawUrl

            try {
                // If it's a direct player host (e.g. dplayer)
                if (cleanUrl.contains("dplayer") || cleanUrl.contains("iframe.php")) {
                    val playerHtml = app.get(
                        cleanUrl,
                        headers = mapOf(
                            "Referer" to data,
                            "Origin" to domain,
                            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
                        )
                    ).text

                    val m3u8Match = Regex("""(?:file|source)\s*:\s*["']([^"']+\.m3u8[^"']*)["']""").find(playerHtml)?.groupValues?.get(1)
                    if (m3u8Match != null) {
                        callback(
                            newExtractorLink(
                                source = name,
                                name = "DiziPal HD",
                                url = m3u8Match,
                                type = INFER_TYPE
                            ) {
                                this.referer = cleanUrl
                                this.quality = Qualities.P1080.value
                            }
                        )
                    }
                }

                when {
                    cleanUrl.contains("vidmoly") -> vidmoly.getUrl(cleanUrl, data, subtitleCallback, callback)
                    cleanUrl.contains("rapidame") -> rapidame.getUrl(cleanUrl, data, subtitleCallback, callback)
                    cleanUrl.contains("streamwish") -> streamwish.getUrl(cleanUrl, data, subtitleCallback, callback)
                    cleanUrl.contains("closeload") -> closeLoad.getUrl(cleanUrl, data, subtitleCallback, callback)
                    else -> ExtractorHelper.resolveStream(cleanUrl, data, "DiziPal", subtitleCallback, callback)
                }
            } catch (_: Exception) {
            }
        }

        return true
    }
}
