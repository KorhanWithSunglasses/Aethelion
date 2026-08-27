package com.hexated

import com.hexated.core.DynamicDomainHelper
import com.hexated.core.NetworkHelper
import com.hexated.extractors.CloseLoad
import com.hexated.extractors.Rapidame
import com.hexated.extractors.Streamwish
import com.hexated.extractors.Vidmoly
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class DramacoolProvider : MainAPI() {
    override var name = "Dramacool"
    override var mainUrl = "https://dramacool.ch"
    override var lang = "en"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.AsianDrama)

    private val fallbackDomains = listOf(
        "https://dramacool.ch",
        "https://dramacool.sr",
        "https://dramacool.city"
    )

    private suspend fun getUrl(): String {
        return DynamicDomainHelper.getActiveDomain("dramacool", fallbackDomains)
    }

    override val mainPage = mainPageOf(
        "" to "Recently Added",
        "most-popular-drama" to "Popular Dramas",
        "korean-drama" to "Korean Dramas",
        "japanese-drama" to "Japanese Dramas",
        "chinese-drama" to "Chinese Dramas"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val domain = getUrl()
        val url = if (page <= 1) {
            "$domain/${request.data}"
        } else {
            "$domain/${request.data}?page=$page"
        }

        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document
        val home = doc.select("ul.list-episode-item li, div.block-tab div.item, ul.switch-block li").mapNotNull {
            it.toSearchResult()
        }

        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h3.title, .title, a.title, h2")?.text()?.trim() ?: return null
        val href = this.selectFirst("a")?.attr("href") ?: return null
        val posterUrl = this.selectFirst("img")?.let {
            it.attr("data-original").ifEmpty { it.attr("src") }
        }

        return newTvSeriesSearchResponse(title, href, TvType.AsianDrama) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val domain = getUrl()
        val searchUrl = "$domain/search?type=drama&keyword=$query"
        val doc = app.get(searchUrl, headers = NetworkHelper.defaultHeaders).document

        return doc.select("ul.list-episode-item li, ul.switch-block li").mapNotNull {
            it.toSearchResult()
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document

        val title = doc.selectFirst("h1, div.info h1")?.text()?.trim() ?: "Unknown Drama"
        val poster = doc.selectFirst("div.img img, div.details img")?.let {
            it.attr("src").ifEmpty { it.attr("data-original") }
        }
        val description = doc.selectFirst("div.info p, div.details p")?.text()?.trim()
        val tags = doc.select("div.info p a, div.details a").map { it.text().trim() }

        val episodes = mutableListOf<Episode>()
        doc.select("ul.list-episode-item-2 li a, ul.all-episode li a").forEachIndexed { index, ep ->
            val epHref = ep.attr("href")
            val epName = ep.selectFirst("h3")?.text()?.trim() ?: "${index + 1}. Episode"
            val epNum = Regex("""Episode\s*(\d+)""", RegexOption.IGNORE_CASE).find(epName)?.groupValues?.get(1)?.toIntOrNull() ?: (index + 1)

            episodes.add(
                newEpisode(epHref) {
                    this.name = epName
                    this.episode = epNum
                }
            )
        }

        return newTvSeriesLoadResponse(title, url, TvType.AsianDrama, episodes) {
            this.posterUrl = poster
            this.plot = description
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

        val iframes = doc.select("iframe, div.block-watch iframe").mapNotNull {
            it.attr("src").takeIf { src -> src.isNotEmpty() }
        }.toMutableList()

        doc.select("div.anime_muti_link ul li, div.block-tab ul li").forEach { btn ->
            val playerUrl = btn.attr("data-video").ifEmpty { btn.selectFirst("a")?.attr("data-video") ?: "" }
            if (playerUrl.startsWith("http") || playerUrl.startsWith("//")) {
                iframes.add(if (playerUrl.startsWith("//")) "https:$playerUrl" else playerUrl)
            }
        }

        for (iframeUrl in iframes.distinct()) {
            val cleanUrl = if (iframeUrl.startsWith("//")) "https:$iframeUrl" else iframeUrl

            when {
                cleanUrl.contains("streamwish") -> Streamwish().getUrl(cleanUrl, data)?.forEach { callback(it) }
                cleanUrl.contains("closeload") -> CloseLoad().getUrl(cleanUrl, data)?.forEach { callback(it) }
                cleanUrl.contains("vidmoly") -> Vidmoly().getUrl(cleanUrl, data)?.forEach { callback(it) }
                cleanUrl.contains("rapidame") -> Rapidame().getUrl(cleanUrl, data)?.forEach { callback(it) }
                else -> loadExtractor(cleanUrl, data, subtitleCallback, callback)
            }
        }

        return true
    }
}
