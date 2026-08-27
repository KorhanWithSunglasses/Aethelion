package com.hexated

import com.hexated.core.DynamicDomainHelper
import com.hexated.core.NetworkHelper
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class AnimePaheProvider : MainAPI() {
    override var name = "AnimePahe"
    override var mainUrl = "https://animepahe.ru"
    override var lang = "en"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Anime)

    private val fallbackDomains = listOf(
        "https://animepahe.ru",
        "https://animepahe.com",
        "https://animepahe.org"
    )

    private suspend fun getUrl(): String {
        return DynamicDomainHelper.getActiveDomain("animepahe", fallbackDomains)
    }

    override val mainPage = mainPageOf(
        "" to "Latest Releases"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val domain = getUrl()
        val url = "$domain/api?m=airing&page=$page"
        val res = app.get(url, headers = NetworkHelper.defaultHeaders).text

        val results = mutableListOf<SearchResponse>()
        val matches = Regex(""""anime_title":\s*"([^"]+)",\s*"anime_id":\s*(\d+),\s*"snapshot":\s*"([^"]+)",\s*"anime_session":\s*"([^"]+)"""").findAll(res)

        for (m in matches) {
            val title = m.groupValues[1]
            val poster = m.groupValues[3].replace("""\/""", "/")
            val session = m.groupValues[4]
            val href = "$domain/anime/$session"

            results.add(
                newAnimeSearchResponse(title, href, TvType.Anime) {
                    this.posterUrl = poster
                }
            )
        }

        return newHomePageResponse(request.name, results)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val domain = getUrl()
        val searchUrl = "$domain/api?m=search&q=$query"
        val res = app.get(searchUrl, headers = NetworkHelper.defaultHeaders).text

        val results = mutableListOf<SearchResponse>()
        val matches = Regex(""""title":\s*"([^"]+)",\s*"poster":\s*"([^"]+)",\s*"session":\s*"([^"]+)"""").findAll(res)

        for (m in matches) {
            val title = m.groupValues[1]
            val poster = m.groupValues[2].replace("""\/""", "/")
            val session = m.groupValues[3]
            val href = "$domain/anime/$session"

            results.add(
                newAnimeSearchResponse(title, href, TvType.Anime) {
                    this.posterUrl = poster
                }
            )
        }

        return results
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url, headers = NetworkHelper.defaultHeaders).document

        val title = doc.selectFirst("div.title-wrapper h1")?.text()?.trim() ?: "Unknown Anime"
        val poster = doc.selectFirst("div.anime-poster img")?.attr("data-src")?.ifEmpty { doc.selectFirst("div.anime-poster img")?.attr("src") }
        val description = doc.selectFirst("div.anime-synopsis")?.text()?.trim()
        val year = doc.selectFirst("div.anime-info p:contains(Aired:)")?.text()?.filter { it.isDigit() }?.take(4)?.toIntOrNull()

        val animeId = doc.selectFirst("meta[name=id]")?.attr("content")
            ?: Regex("""/anime/([a-f0-9-]+)""").find(url)?.groupValues?.get(1) ?: ""

        val domain = getUrl()
        val epApiUrl = "$domain/api?m=release&id=$animeId&sort=episode_asc&page=1"
        val epRes = app.get(epApiUrl, headers = NetworkHelper.defaultHeaders).text

        val episodes = mutableListOf<Episode>()
        val epMatches = Regex(""""episode":\s*(\d+),\s*"session":\s*"([^"]+)"""").findAll(epRes)

        for (m in epMatches) {
            val epNum = m.groupValues[1].toIntOrNull() ?: 1
            val epSession = m.groupValues[2]
            val epUrl = "$domain/play/$animeId/$epSession"

            episodes.add(
                newEpisode(epUrl) {
                    this.name = "$epNum. Episode"
                    this.episode = epNum
                }
            )
        }

        return newAnimeLoadResponse(title, url, TvType.Anime) {
            this.posterUrl = poster
            this.plot = description
            this.year = year
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

        doc.select("div#resolutionMenu button").forEach { btn ->
            val playerUrl = btn.attr("data-src")
            if (playerUrl.isNotEmpty()) {
                loadExtractor(playerUrl, data, subtitleCallback, callback)
            }
        }

        return true
    }
}
