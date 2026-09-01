@file:Suppress("DEPRECATION")
package com.hexated

import com.hexated.core.DynamicDomainHelper
import com.hexated.core.NetworkHelper
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.Qualities
import org.jsoup.nodes.Element

class CanliYayinProvider : MainAPI() {
    override var name = "Canlı Yayın"
    override var mainUrl = "https://canlitv.center"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Live)

    private val fallbackDomains = listOf(
        "https://canlitv.center",
        "https://canlitv.me",
        "https://canlitv.plus"
    )

    private suspend fun getUrl(): String {
        return DynamicDomainHelper.getActiveDomain("canliyayin", fallbackDomains)
    }

    override val mainPage = mainPageOf(
        "" to "Tüm Kanallar",
        "ulusal-kanallar" to "Ulusal Kanallar",
        "haber-kanallari" to "Haber Kanalları",
        "spor-kanallari" to "Spor Kanalları",
        "sinema-dizi-kanallari" to "Sinema & Dizi",
        "belgesel-kanallari" to "Belgesel Kanalları",
        "cocuk-kanallari" to "Çocuk Kanalları",
        "muzik-kanallari" to "Müzik Kanalları"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val domain = getUrl()
        val url = if (request.data.isEmpty()) domain else "$domain/${request.data}"

        return try {
            val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document
            val channels = doc.select("div.channel-item, a.channel-card, div.tv-card, a[href*=\"canli-tv\"]").mapNotNull {
                it.toSearchResult(domain)
            }.distinctBy { it.url }

            newHomePageResponse(request.name, channels)
        } catch (_: Exception) {
            newHomePageResponse(request.name, emptyList())
        }
    }

    private fun Element.toSearchResult(domain: String): SearchResponse? {
        val rawHref = this.attr("href").ifEmpty { this.selectFirst("a")?.attr("href") } ?: return null
        if (rawHref.contains("javascript:") || rawHref.startsWith("#")) return null
        val href = if (rawHref.startsWith("http")) rawHref else "$domain$rawHref"

        val title = this.selectFirst("h2, h3, .title, .channel-name, img[alt]")?.let {
            if (it.tagName() == "img") it.attr("alt") else it.text()
        }?.trim()?.ifEmpty { null } ?: this.text().trim().ifEmpty { null } ?: return null

        val posterUrl = this.selectFirst("img")?.let { img ->
            val dataSrc = img.attr("data-src").ifEmpty { img.attr("data-srcset") }
            val src = img.attr("src")
            if (dataSrc.isNotEmpty() && !dataSrc.startsWith("data:")) dataSrc else src
        }

        return newMovieSearchResponse(title, href, TvType.Live) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val domain = getUrl()
        val searchUrl = "$domain/?s=$query"
        return try {
            val doc = app.get(searchUrl, headers = NetworkHelper.defaultHeaders).document
            doc.select("div.channel-item, a.channel-card, div.tv-card, a[href*=\"canli-tv\"]").mapNotNull {
                it.toSearchResult(domain)
            }.distinctBy { it.url }
        } catch (_: Exception) {
            emptyList()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document
        val title = doc.selectFirst("h1, .channel-title, .title")?.text()?.trim() ?: "Canlı TV"
        val poster = doc.selectFirst("div.channel-logo img, .poster img, meta[property=og:image]")?.let {
            it.attr("src").ifEmpty { it.attr("content") }
        }
        val description = doc.selectFirst("div.channel-info, div.description, p")?.text()?.trim() ?: "Canlı Yayın"

        return newMovieLoadResponse(title, url, TvType.Live, url) {
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

        val iframeSrc = doc.selectFirst("iframe")?.attr("src")
        val streamPage = if (!iframeSrc.isNullOrEmpty() && iframeSrc.startsWith("http")) {
            app.get(iframeSrc, headers = NetworkHelper.getRefererHeaders(data)).text
        } else {
            doc.html()
        }

        val m3u8Regex = Regex("""file:\s*["']([^"']+\.m3u8[^"']*)["']""")
        val m3u8Url = m3u8Regex.find(streamPage)?.groupValues?.get(1)
            ?: Regex("""source\s*:\s*["']([^"']+\.m3u8[^"']*)["']""").find(streamPage)?.groupValues?.get(1)
            ?: return false

        callback(
            ExtractorLink(
                source = name,
                name = "Canlı Yayın HD",
                url = m3u8Url,
                referer = data,
                quality = Qualities.P1080.value,
                type = INFER_TYPE,
                headers = NetworkHelper.getStreamHeaders(data, data)
            )
        )
        return true
    }
}
