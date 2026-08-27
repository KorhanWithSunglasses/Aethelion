package com.hexated

import com.hexated.core.DynamicDomainHelper
import com.hexated.core.NetworkHelper
import com.hexated.extractors.Rapidame
import com.hexated.extractors.Vidmoly
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class SezonlukDiziProvider : MainAPI() {
    override var name = "SezonlukDizi"
    override var mainUrl = "https://sezonlukdizi.cc"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.TvSeries)

    private val fallbackDomains = listOf(
        "https://sezonlukdizi.cc",
        "https://sezonlukdizi.pro",
        "https://sezonlukdizi.net"
    )

    private suspend fun getUrl(): String {
        return DynamicDomainHelper.getActiveDomain("sezonlukdizi", fallbackDomains)
    }

    override val mainPage = mainPageOf(
        "" to "Son Eklenen Bölümler",
        "diziler" to "Tüm Diziler",
        "populer-diziler" to "Popüler Diziler",
        "tur/mini-dizi" to "Mini Diziler"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val domain = getUrl()
        val url = if (page <= 1) {
            "$domain/${request.data}"
        } else {
            "$domain/${request.data}?sayfa=$page"
        }

        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document
        val home = doc.select("div.afis, div.dizi-kutu, div.poster, article").mapNotNull {
            it.toSearchResult()
        }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h2, h3, .title, .dizi-adi, a")?.text()?.trim() ?: return null
        val href = this.selectFirst("a")?.attr("href") ?: return null
        val posterUrl = this.selectFirst("img")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }
        }

        return newTvSeriesSearchResponse(title, href, TvType.TvSeries) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val domain = getUrl()
        val searchUrl = "$domain/arama?q=$query"
        val doc = app.get(searchUrl, headers = NetworkHelper.defaultHeaders).document

        return doc.select("div.afis, div.dizi-kutu, div.search-result, article").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document

        val title = doc.selectFirst("h1, .dizi-baslik, .title")?.text()?.trim() ?: "Bilinmeyen Dizi"
        val poster = doc.selectFirst("div.dizi-afis img, .poster img")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }
        }
        val description = doc.selectFirst("div.ozet, div.description, p.story")?.text()?.trim()
        val year = doc.selectFirst(".yapim-yili, .year")?.text()?.filter { it.isDigit() }?.toIntOrNull()
        val rating = doc.selectFirst(".imdb-puani, .rating")?.text()?.toRatingInt()
        val tags = doc.select("div.turler a, div.genres a").map { it.text().trim() }

        val episodes = mutableListOf<Episode>()
        doc.select("div.bolum-satir, li.bolum, div.episodes a, a.bolum-link").forEachIndexed { index, ep ->
            val epHref = ep.attr("href")
            val epName = ep.text().trim().ifEmpty { "${index + 1}. Bölüm" }
            val seasonNum = ep.attr("data-sezon").toIntOrNull() ?: 1
            val epNum = ep.attr("data-bolum").toIntOrNull() ?: (index + 1)

            episodes.add(
                newEpisode(epHref) {
                    this.name = epName
                    this.season = seasonNum
                    this.episode = epNum
                }
            )
        }

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
            this.rating = rating
            this.tags = tags
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data, headers = NetworkHelper.defaultHeaders).document

        val iframes = doc.select("iframe, div#player iframe, div.video-container iframe").mapNotNull {
            it.attr("data-src").ifEmpty { it.attr("src") }.takeIf { src -> src.isNotEmpty() }
        }.toMutableList()

        doc.select("div.kaynaklar a, button.kaynak-btn, a.nav-link").forEach { btn ->
            val playerUrl = btn.attr("data-url").ifEmpty { btn.attr("data-src") }.ifEmpty { btn.attr("href") }
            if (playerUrl.startsWith("http") || playerUrl.startsWith("//")) {
                iframes.add(if (playerUrl.startsWith("//")) "https:$playerUrl" else playerUrl)
            }
        }

        for (iframeUrl in iframes.distinct()) {
            val cleanUrl = if (iframeUrl.startsWith("//")) "https:$iframeUrl" else iframeUrl

            when {
                cleanUrl.contains("vidmoly") -> Vidmoly().getUrl(cleanUrl, data)?.forEach { callback(it) }
                cleanUrl.contains("rapidame") -> Rapidame().getUrl(cleanUrl, data)?.forEach { callback(it) }
                else -> loadExtractor(cleanUrl, data, subtitleCallback, callback)
            }
        }

        return true
    }
}
