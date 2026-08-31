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

class HDFilmCehennemiProvider : MainAPI() {
    override var name = "HDFilmCehennemi"
    override var mainUrl = "https://www.hdfilmcehennemi.nl"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    // Sequential loading prevents Cloudflare / server rate-limiting on categories
    override var sequentialMainPage = true
    override var sequentialMainPageDelay = 50L
    override var sequentialMainPageScrollDelay = 50L

    private val fallbackDomains = listOf(
        "https://www.hdfilmcehennemi.nl",
        "https://www.hdfilmcehennemi.life",
        "https://hdfilmcehennemi.vip",
        "https://hdfilmcehennemi.cx",
        "https://hdfilmcehennemi.net"
    )

    private suspend fun getUrl(): String {
        return DynamicDomainHelper.getActiveDomain("hdfilmcehennemi", fallbackDomains)
    }

    override val mainPage = mainPageOf(
        "" to "Yeni Eklenen Filmler",
        "yabancidiziizle-2" to "Yeni Eklenen Diziler",
        "category/tavsiye-filmler-izle2" to "Tavsiye Filmler",
        "imdb-7-puan-uzeri-filmler" to "IMDb 7+ Filmler",
        "en-cok-yorumlananlar-1" to "En Çok Yorumlananlar",
        "en-cok-begenilen-filmleri-izle" to "En Çok Beğenilenler",
        "tur/aile-filmleri-izleyin-6" to "Aile Filmleri",
        "tur/aksiyon-filmleri-izleyin-4" to "Aksiyon Filmleri",
        "tur/animasyon-filmleri-izle-1" to "Animasyon Filmleri",
        "tur/komedi-filmleri-izleyin-1" to "Komedi Filmleri"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val domain = getUrl()
        val url = if (page <= 1) {
            if (request.data.isEmpty()) domain else "$domain/${request.data}"
        } else {
            "$domain/${request.data}/page/$page/"
        }

        return try {
            val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document
            val home = doc.select("div.card, div.poster, article, div.movie-card, div.film-item, a[href*=\"-izle\"]").mapNotNull {
                it.toSearchResult(domain)
            }.distinctBy { it.url }

            newHomePageResponse(request.name, home)
        } catch (_: Exception) {
            newHomePageResponse(request.name, emptyList())
        }
    }

    private fun Element.toSearchResult(domain: String): SearchResponse? {
        val rawHref = this.attr("href").ifEmpty { this.selectFirst("a")?.attr("href") } ?: return null
        if (rawHref.contains("javascript:") || rawHref.startsWith("#")) return null
        val href = if (rawHref.startsWith("http")) rawHref else "$domain$rawHref"

        val title = this.selectFirst("h2, h3, .title, .card-title, img[alt]")?.let {
            if (it.tagName() == "img") it.attr("alt") else it.text()
        }?.trim()?.ifEmpty { null } ?: this.text().trim().ifEmpty { null } ?: return null

        val posterUrl = this.selectFirst("img")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }.ifEmpty { it.attr("data-lazy-src") }
        }

        val isSeries = href.contains("dizi") || href.contains("sezon")
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
        val searchUrl = "$domain/?s=$query"
        return try {
            val doc = app.get(searchUrl, headers = NetworkHelper.defaultHeaders).document
            doc.select("div.card, div.poster, article, div.movie-card, div.search-result").mapNotNull {
                it.toSearchResult(domain)
            }.distinctBy { it.url }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val domain = getUrl()
        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document

        val title = doc.selectFirst("h1, .card-title, .title")?.text()?.trim() ?: "Film"
        val poster = doc.selectFirst("div.poster img, .card img, meta[property=og:image]")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }.ifEmpty { it.attr("content") }
        }
        val description = doc.selectFirst("div.overview, div.description, div.card-body p, p")?.text()?.trim()
        val year = doc.selectFirst(".year, .date")?.text()?.filter { it.isDigit() }?.toIntOrNull()
        val tags = doc.select("div.genres a, .tags a, div.card-category a").map { it.text().trim() }

        val isSeries = url.contains("dizi") || doc.select("div.episodes, ul.episodes, a[href*=bolum]").isNotEmpty()

        return if (isSeries) {
            val episodes = mutableListOf<Episode>()
            doc.select("ul.episodes li a, div.episodes a, a[href*=\"-sezon-\"], a[href*=\"-bolum\"]").forEachIndexed { index, epLink ->
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
        val doc = app.get(data, headers = NetworkHelper.defaultHeaders).document

        val iframes = mutableListOf<String>()
        doc.select("iframe").forEach {
            val src = it.attr("src").ifEmpty { it.attr("data-src") }
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
                else -> ExtractorHelper.resolveStream(cleanUrl, data, "HDFilmCehennemi", subtitleCallback, callback)
            }
        }

        return true
    }
}
