package com.hexated

import com.hexated.core.DynamicDomainHelper
import com.hexated.core.NetworkHelper
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import org.jsoup.nodes.Element

class CanliYayinProvider : MainAPI() {
    override var name = "Canlı Yayın & Spor"
    override var mainUrl = "https://www.canlitv.me"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Live)

    private val fallbackDomains = listOf(
        "https://www.canlitv.me",
        "https://www.canlitv.vin",
        "https://taraftarium24.mobi"
    )

    private suspend fun getUrl(): String {
        return DynamicDomainHelper.getActiveDomain("canlitv", fallbackDomains)
    }

    override val mainPage = mainPageOf(
        "" to "Tüm Canlı Kanallar",
        "kategori/spor" to "Spor Yayınları",
        "kategori/ulusal" to "Ulusal Kanallar"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val domain = getUrl()
        val url = if (page <= 1) {
            "$domain/${request.data}"
        } else {
            "$domain/${request.data}?sayfa=$page"
        }

        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document
        val home = doc.select("div.kanal-kutu, div.channel-card, article").mapNotNull {
            it.toSearchResult()
        }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h2, h3, .kanal-adi, .title, a")?.text()?.trim() ?: return null
        val href = this.selectFirst("a")?.attr("href") ?: return null
        val posterUrl = this.selectFirst("img")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }
        }

        return newLiveSearchResponse(title, href, TvType.Live) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val domain = getUrl()
        val searchUrl = "$domain/ara?q=$query"
        val doc = app.get(searchUrl, headers = NetworkHelper.defaultHeaders).document

        return doc.select("div.kanal-kutu, div.channel-card, article, div.search-result").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document

        val title = doc.selectFirst("h1, .kanal-baslik")?.text()?.trim() ?: "Canlı TV"
        val poster = doc.selectFirst("div.kanal-logo img, meta[property=og:image]")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }.ifEmpty { it.attr("content") }
        }
        val description = doc.selectFirst("div.kanal-aciklama, p.desc")?.text()?.trim()

        return newLiveStreamLoadResponse(title, url, url) {
            this.posterUrl = poster
            this.plot = description
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data, headers = NetworkHelper.defaultHeaders).document

        val iframeUrl = doc.selectFirst("iframe#tv-frame, div.player iframe")?.attr("src")
        val streamPage = if (iframeUrl != null) {
            val clean = if (iframeUrl.startsWith("//")) "https:$iframeUrl" else iframeUrl
            app.get(clean, headers = NetworkHelper.getRefererHeaders(data)).text
        } else {
            doc.html()
        }

        val m3u8Regex = Regex("""file:\s*["']([^"']+\.m3u8[^"']*)["']""")
        val m3u8Url = m3u8Regex.find(streamPage)?.groupValues?.get(1)
            ?: Regex("""source\s*:\s*["']([^"']+\.m3u8[^"']*)["']""").find(streamPage)?.groupValues?.get(1)
            ?: return false

        callback(
            newExtractorLink(
                name,
                "Canlı Yayın HD",
                m3u8Url,
                data,
                Qualities.P1080.value,
                ExtractorLinkType.M3U8
            ) {
                this.headers = NetworkHelper.getStreamHeaders(data, data)
            }
        )
        return true
    }
}
