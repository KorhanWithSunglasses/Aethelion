package com.hexated

import com.hexated.core.DynamicDomainHelper
import com.hexated.core.ExtractorHelper
import com.hexated.core.ImageHelper
import com.hexated.core.NetworkHelper
import com.hexated.core.fixUrl
import com.hexated.extractors.CloseLoad
import com.hexated.extractors.Rapidame
import com.hexated.extractors.Streamwish
import com.hexated.extractors.Vidmoly
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class DiziBoxProvider : MainAPI() {
    override var name = "DiziBox"
    override var mainUrl = "https://www.dizibox.live"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.TvSeries)

    override var sequentialMainPage = true
    override var sequentialMainPageDelay = 500L
    override var sequentialMainPageScrollDelay = 500L

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
        "" to "Son Eklenen Bölümler",
        "diziler" to "Popüler Diziler",
        "tur/aksiyon" to "Aksiyon Dizileri",
        "tur/bilim-kurgu" to "Bilim Kurgu Dizileri",
        "tur/komedi" to "Komedi Dizileri",
        "tur/dram" to "Dram Dizileri",
        "tur/animasyon" to "Animasyon",
        "tur/korku" to "Korku & Gerilim"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val domain = getUrl()
        val url = if (page <= 1) {
            if (request.data.isEmpty()) domain else "$domain/${request.data}/"
        } else {
            if (request.data.isEmpty()) "$domain/page/$page/" else "$domain/${request.data}/page/$page/"
        }

        return try {
            val doc = app.get(
                url,
                cookies = authCookies,
                headers = NetworkHelper.defaultHeaders
            ).document

            val home = doc.select("article, div.detailed-article, div.post, div.swiper-slide, li.grid-six, li.grid-four").mapNotNull {
                it.toSearchResult(domain)
            }.distinctBy { it.url }

            newHomePageResponse(request.name, home)
        } catch (_: Exception) {
            newHomePageResponse(request.name, emptyList())
        }
    }

    private fun Element.toSearchResult(domain: String): SearchResponse? {
        val rawHref = this.selectFirst("a.poster-title, a[href*=\"/diziler/\"], a[href*=\"-bolum-izle/\"], a")?.attr("href") ?: return null
        if (rawHref.contains("javascript:") || rawHref.startsWith("#") || rawHref.contains("wp-login") || rawHref.contains("yardim")) return null
        val href = fixUrl(rawHref, domain)

        val title = this.selectFirst("a.poster-title, h2, h3, .title, .post-title, img[alt]")?.let {
            if (it.tagName() == "img") it.attr("alt") else it.text()
        }?.trim()?.ifEmpty { null } ?: this.text().trim().ifEmpty { null } ?: return null

        if (title.equals("Üye Girişi", ignoreCase = true) || title.equals("Giriş Yap", ignoreCase = true)) return null

        val posterUrl = ImageHelper.extractPosterUrl(this, domain)

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

            doc.select("article, div.detailed-article, div.post, div.swiper-slide, li.grid-six, li.grid-four").mapNotNull {
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

        val title = doc.selectFirst("div.title-terms h1 a, .tv-overview h1, div.tv-overview a.link-unstyled, h1")?.text()?.trim()
            ?.ifEmpty { null } ?: "Dizi"

        val poster = ImageHelper.extractPosterUrl(doc.selectFirst("div.tv-overview, div.poster, figure#main-cover") ?: doc, domain)
            ?: doc.selectFirst("meta[property=og:image]")?.attr("content")
        val description = doc.selectFirst("div.tv-story p, div.overview, div.description")?.text()?.trim()
        val year = doc.selectFirst(".terms a[href*='/yil/'], .year, .date")?.text()?.filter { it.isDigit() }?.toIntOrNull()
        val tags = doc.select("div.terms a[href*='/tur/'], div.genres a, .tags a").map { it.text().trim() }
        val rating = doc.selectFirst("span.label-imdb b")?.text()?.toDoubleOrNull()

        val episodes = mutableListOf<Episode>()
        val episodeElements = doc.select("a[href*=\"-bolum-izle/\"]")

        episodeElements.forEachIndexed { index, epLink ->
            val rawHref = epLink.attr("href")
            val epHref = fixUrl(rawHref, domain)
            var epText = epLink.text().trim()
            if (epText.isEmpty() || epText.contains("Ağustos") || epText.contains("Temmuz") || epText.contains("Haziran")) {
                epText = "${index + 1}. Bölüm"
            }

            val seasonNum = Regex("""(?:sezon|s)-?(\d+)""", RegexOption.IGNORE_CASE).find(epHref)?.groupValues?.get(1)?.toIntOrNull()
                ?: Regex("""(\d+)\.\s*Sezon""").find(epText)?.groupValues?.get(1)?.toIntOrNull()
                ?: 1

            val epNum = Regex("""(?:bolum|ep)-?(\d+)""", RegexOption.IGNORE_CASE).find(epHref)?.groupValues?.get(1)?.toIntOrNull()
                ?: Regex("""(\d+)\.\s*Bölüm""").find(epText)?.groupValues?.get(1)?.toIntOrNull()
                ?: (index + 1)

            episodes.add(
                newEpisode(epHref) {
                    this.name = "${seasonNum}. Sezon ${epNum}. Bölüm"
                    this.season = seasonNum
                    this.episode = epNum
                    this.posterUrl = poster
                }
            )
        }

        val distinctEpisodes = episodes.distinctBy { it.data }

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, if (distinctEpisodes.isEmpty()) listOf(newEpisode(url) {
            this.name = "1. Bölüm"
            this.season = 1
            this.episode = 1
            this.posterUrl = poster
        }) else distinctEpisodes) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.tags = tags
            this.score = rating?.let { Score.from10(it) }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val domain = getUrl()
        val doc = app.get(
            data,
            cookies = authCookies,
            headers = NetworkHelper.getRefererHeaders(domain)
        ).document

        val iframes = mutableListOf<String>()

        doc.select("iframe").forEach {
            val src = it.attr("src").ifEmpty { it.attr("data-src") }
            if (src.isNotEmpty() && !src.startsWith("#")) iframes.add(fixUrl(src, domain))
        }

        doc.select("div.video-player iframe, div.player iframe, div.source-item a, a.source-button").forEach { el ->
            val src = el.attr("data-src").ifEmpty { el.attr("src") }.ifEmpty { el.attr("href") }
            if (src.isNotEmpty() && !src.startsWith("#")) iframes.add(fixUrl(src, domain))
        }

        val vidmoly = Vidmoly()
        val rapidame = Rapidame()
        val streamwish = Streamwish()
        val closeLoad = CloseLoad()

        val allTargets = mutableListOf<String>()

        // Expand player iframe redirects (e.g. king.php)
        for (rawUrl in iframes.distinct()) {
            if (rawUrl.contains("player/") || rawUrl.contains("king.php") || rawUrl.contains("player.php")) {
                try {
                    val playerDoc = app.get(
                        rawUrl,
                        headers = NetworkHelper.getRefererHeaders(data)
                    ).document
                    playerDoc.select("iframe").forEach { nestedIfr ->
                        val nSrc = nestedIfr.attr("src").ifEmpty { nestedIfr.attr("data-src") }
                        if (nSrc.isNotEmpty()) allTargets.add(fixUrl(nSrc, domain))
                    }
                } catch (_: Exception) {
                    allTargets.add(rawUrl)
                }
            } else {
                allTargets.add(rawUrl)
            }
        }

        for (targetUrl in allTargets.distinct()) {
            try {
                when {
                    targetUrl.contains("molystream") || targetUrl.contains("vidmoly") -> {
                        vidmoly.getUrl(targetUrl, data, subtitleCallback, callback)
                    }
                    targetUrl.contains("rapidame") -> {
                        rapidame.getUrl(targetUrl, data, subtitleCallback, callback)
                    }
                    targetUrl.contains("streamwish") -> {
                        streamwish.getUrl(targetUrl, data, subtitleCallback, callback)
                    }
                    targetUrl.contains("closeload") -> {
                        closeLoad.getUrl(targetUrl, data, subtitleCallback, callback)
                    }
                    else -> {
                        loadExtractor(targetUrl, "$domain/", subtitleCallback, callback)
                        ExtractorHelper.resolveStream(targetUrl, data, "DiziBox", subtitleCallback, callback)
                    }
                }
            } catch (_: Exception) {
            }
        }

        return true
    }
}
