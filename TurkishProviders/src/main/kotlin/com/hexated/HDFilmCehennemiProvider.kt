@file:Suppress("DEPRECATION")
package com.hexated

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element

class HDFilmCehennemiProvider : MainAPI() {
    override var name = "HDFilmCehennemi"
    override var mainUrl = "https://www.hdfilmcehennemi.nl"
    override var lang = "tr"
    override val hasMainPage = true
    override val hasQuickSearch = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    override var sequentialMainPage = true
    override var sequentialMainPageDelay = 500L
    override var sequentialMainPageScrollDelay = 500L

    override val mainPage = mainPageOf(
        mainUrl to "Yeni Eklenen Filmler",
        "${mainUrl}/yabancidiziizle-2" to "Yeni Eklenen Diziler",
        "${mainUrl}/category/tavsiye-filmler-izle2" to "Tavsiye Filmler",
        "${mainUrl}/imdb-7-puan-uzeri-filmler" to "IMDb 7+ Filmler",
        "${mainUrl}/en-cok-yorumlananlar-1" to "En Çok Yorumlananlar",
        "${mainUrl}/en-cok-begenilen-filmleri-izle" to "En Çok Beğenilenler",
        "${mainUrl}/tur/aile-filmleri-izleyin-6" to "Aile Filmleri",
        "${mainUrl}/tur/aksiyon-filmleri-izleyin-3" to "Aksiyon Filmleri",
        "${mainUrl}/tur/animasyon-filmlerini-izleyin-4" to "Animasyon Filmleri",
        "${mainUrl}/tur/belgesel-filmlerini-izle-1" to "Belgesel Filmleri",
        "${mainUrl}/tur/bilim-kurgu-filmlerini-izleyin-2" to "Bilim Kurgu Filmleri",
        "${mainUrl}/tur/komedi-filmlerini-izleyin-1" to "Komedi Filmleri",
        "${mainUrl}/tur/korku-filmlerini-izle-2/" to "Korku Filmleri",
        "${mainUrl}/tur/romantik-filmleri-izle-1" to "Romantik Filmleri"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) request.data else "${request.data}/page/$page/"
        val document = app.get(url).document
        val home = document.select("div.section-content a.poster").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("strong.poster-title")?.text()?.trim() ?: return null
        val href = fixUrlNull(this.attr("href")) ?: return null
        val posterUrl = fixUrlNull(
            this.selectFirst("img")?.attr("data-src")
                ?: this.selectFirst("img")?.attr("data-srcset")
                ?: this.selectFirst("img")?.attr("src")
        )

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun search(query: String): List<SearchResponse> {
        val response = try {
            app.get(
                "${mainUrl}/search?q=${query}",
                headers = mapOf("X-Requested-With" to "fetch")
            ).parsedSafe<Results>()
        } catch (_: Exception) {
            null
        } ?: return emptyList()

        val searchResults = mutableListOf<SearchResponse>()
        response.results.forEach { resultHtml ->
            val document = Jsoup.parse(resultHtml)
            val title = document.selectFirst("h4.title")?.text() ?: return@forEach
            val href = fixUrlNull(document.selectFirst("a")?.attr("href")) ?: return@forEach
            val posterUrl = fixUrlNull(
                document.selectFirst("img")?.attr("src")
                    ?: document.selectFirst("img")?.attr("data-src")
            )

            searchResults.add(
                newMovieSearchResponse(title, href, TvType.Movie) {
                    this.posterUrl = posterUrl?.replace("/thumb/", "/list/")
                }
            )
        }
        return searchResults
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document

        val title = document.selectFirst("h1.section-title")?.text()?.substringBefore(" izle")?.trim() ?: return null
        val poster = fixUrlNull(document.select("aside.post-info-poster img.lazyload").lastOrNull()?.attr("data-src"))
            ?: fixUrlNull(document.selectFirst("aside.post-info-poster img")?.attr("src"))
        val tags = document.select("div.post-info-genres a").map { it.text().trim() }
        val year = document.selectFirst("div.post-info-year-country a")?.text()?.trim()?.toIntOrNull()
        val tvType = if (document.select("div.seasons, div.seasons-tab-content").isEmpty()) TvType.Movie else TvType.TvSeries
        val description = document.selectFirst("article.post-info-content > p, div.post-info-content")?.text()?.trim()
        val rating = document.selectFirst("div.post-info-imdb-rating span")?.text()?.substringBefore("(")?.trim()?.toDoubleOrNull()?.times(1000)?.toInt()
        val actors = document.select("div.post-info-cast a").mapNotNull {
            val name = it.selectFirst("strong")?.text()?.trim() ?: return@mapNotNull null
            val img = fixUrlNull(it.select("img").attr("data-src"))
            Actor(name, img)
        }

        val recommendations = document.select("div.section-slider-container div.slider-slide").mapNotNull {
            val recName = it.selectFirst("a")?.attr("title") ?: return@mapNotNull null
            val recHref = fixUrlNull(it.selectFirst("a")?.attr("href")) ?: return@mapNotNull null
            val recPosterUrl = fixUrlNull(it.selectFirst("img")?.attr("data-src")) ?: fixUrlNull(it.selectFirst("img")?.attr("src"))

            newTvSeriesSearchResponse(recName, recHref, TvType.TvSeries) {
                this.posterUrl = recPosterUrl
            }
        }

        val trailer = document.selectFirst("div.post-info-trailer button")?.attr("data-modal")
            ?.substringAfter("trailer/")?.let { "https://www.youtube.com/embed/$it" }

        return if (tvType == TvType.TvSeries) {
            val episodes = document.select("div.seasons-tab-content a").mapNotNull {
                val epName = it.selectFirst("h4")?.text()?.trim() ?: return@mapNotNull null
                val epHref = fixUrlNull(it.attr("href")) ?: return@mapNotNull null
                val epEpisode = Regex("""(\d+)\.\s*Bölüm""", RegexOption.IGNORE_CASE).find(epName)?.groupValues?.get(1)?.toIntOrNull()
                val epSeason = Regex("""(\d+)\.\s*Sezon""", RegexOption.IGNORE_CASE).find(epName)?.groupValues?.get(1)?.toIntOrNull() ?: 1

                newEpisode(epHref) {
                    this.name = epName
                    this.season = epSeason
                    this.episode = epEpisode
                    this.posterUrl = poster
                }
            }

            newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                this.rating = rating
                this.recommendations = recommendations
                addActors(actors)
                addTrailer(trailer)
            }
        } else {
            newMovieLoadResponse(title, url, TvType.Movie, url) {
                this.posterUrl = poster
                this.year = year
                this.plot = description
                this.tags = tags
                this.rating = rating
                this.recommendations = recommendations
                addActors(actors)
                addTrailer(trailer)
            }
        }
    }

    private suspend fun invokeLocalSource(
        source: String,
        url: String,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        try {
            val script = app.get(url, referer = "${mainUrl}/").document.select("script").find {
                it.data().contains("sources:")
            }?.data() ?: return

            val videoData = getAndUnpack(script).substringAfter("file_link=\"").substringBefore("\";")
            val subData = script.substringAfter("tracks: [").substringBefore("]")

            val decodedUrl = base64Decode(videoData)
            if (decodedUrl.isNotEmpty()) {
                callback.invoke(
                    ExtractorLink(
                        source = source,
                        name = source,
                        url = decodedUrl,
                        referer = "${mainUrl}/",
                        quality = Qualities.Unknown.value,
                        type = INFER_TYPE
                    )
                )
            }

            AppUtils.tryParseJson<List<SubSource>>("[$subData]")?.filter { it.kind == "captions" }?.forEach {
                val subUrl = it.file ?: return@forEach
                subtitleCallback.invoke(
                    SubtitleFile(it.label ?: "Türkçe", fixUrl(subUrl))
                )
            }
        } catch (_: Exception) {
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document

        // 1. Check alternative links video API
        document.select("div.alternative-links").map { element ->
            element to element.attr("data-lang").uppercase()
        }.forEach { (element, langCode) ->
            element.select("button.alternative-link").map { button ->
                button.text().replace("(HDrip Xbet)", "").trim() + " $langCode" to button.attr("data-video")
            }.forEach { (source, videoID) ->
                try {
                    val apiGet = app.get(
                        "${mainUrl}/video/$videoID/",
                        headers = mapOf(
                            "Content-Type" to "application/json",
                            "X-Requested-With" to "fetch"
                        ),
                        referer = data
                    ).text

                    var iframe = Regex("""data-src=\\"([^"]+)""").find(apiGet)?.groupValues?.get(1)?.replace("\\", "")
                    if (iframe != null) {
                        if (iframe.contains("?rapidrame_id=")) {
                            iframe = "${mainUrl}/playerr/" + iframe.substringAfter("?rapidrame_id=")
                        }
                        invokeLocalSource(source, iframe, subtitleCallback, callback)
                        loadExtractor(iframe, "$mainUrl/", subtitleCallback, callback)
                    }
                } catch (_: Exception) {
                }
            }
        }

        // 2. Fallback to direct iframes on page
        document.select("iframe").forEach {
            val src = it.attr("src").ifEmpty { it.attr("data-src") }
            if (src.isNotEmpty() && !src.startsWith("#")) {
                loadExtractor(fixUrl(src), "$mainUrl/", subtitleCallback, callback)
            }
        }

        return true
    }

    private data class SubSource(
        @JsonProperty("file") val file: String? = null,
        @JsonProperty("label") val label: String? = null,
        @JsonProperty("kind") val kind: String? = null
    )

    data class Results(
        @JsonProperty("results") val results: List<String> = arrayListOf()
    )
}
