package com.hexated

import com.hexated.core.DynamicDomainHelper
import com.hexated.core.NetworkHelper
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor

class VidsrcProvider : MainAPI() {
    override var name = "VidSrc"
    override var mainUrl = "https://vidsrc.to"
    override var lang = "en"
    override val hasMainPage = false
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    private val fallbackDomains = listOf(
        "https://vidsrc.to",
        "https://vidsrc.me",
        "https://vidsrc.in",
        "https://vidsrc.pm"
    )

    private suspend fun getUrl(): String {
        return DynamicDomainHelper.getActiveDomain("vidsrc", fallbackDomains)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        // Direct resolver for IMDb/TMDB IDs or search mirrors
        return emptyList()
    }

    override suspend fun load(url: String): LoadResponse {
        val title = "VidSrc Stream"
        val isSeries = url.contains("/tv/")

        return if (isSeries) {
            newTvSeriesLoadResponse(title, url, TvType.TvSeries, emptyList())
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val domain = getUrl()
        val embedUrl = if (data.startsWith("http")) data else "$domain/embed/$data"
        val res = app.get(embedUrl, headers = NetworkHelper.getRefererHeaders(domain)).text

        val iframeRegex = Regex("""iframe\s+src=["']([^"']+)""")
        val playerUrl = iframeRegex.find(res)?.groupValues?.get(1) ?: return false
        val cleanUrl = if (playerUrl.startsWith("//")) "https:$playerUrl" else playerUrl

        loadExtractor(cleanUrl, embedUrl, subtitleCallback, callback)
        return true
    }
}
