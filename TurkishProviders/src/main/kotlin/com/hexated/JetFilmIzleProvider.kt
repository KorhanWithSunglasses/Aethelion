package com.hexated

import com.hexated.core.ImageHelper
import com.hexated.core.NetworkHelper
import com.hexated.core.fixUrl
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class JetFilmIzleProvider : MainAPI() {
    override var mainUrl = "https://jetfilmizle.io"
    override var name = "JetFilmİzle"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.Movie)

    override var sequentialMainPage = true
    override var sequentialMainPageDelay = 500L
    override var sequentialMainPageScrollDelay = 500L

    override val mainPage = mainPageOf(
        "${mainUrl}/page/" to "Son Filmler",
        "${mainUrl}/netflix/page/" to "Netflix",
        "${mainUrl}/editorun-secimi/page/" to "Editörün Seçimi",
        "${mainUrl}/turk-film-izle/page/" to "Türk Filmleri",
        "${mainUrl}/cizgi-filmler-izle/page/" to "Çizgi Filmler",
        "${mainUrl}/kategoriler/yesilcam-filmleri-izlee/page/" to "Yeşilçam Filmleri"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val document = app.get("${request.data}${page}", headers = NetworkHelper.defaultHeaders).document
        val home = document.select("article.movie").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        var title = this.selectFirst("h2 a")?.text() ?: this.selectFirst("h3 a")?.text()
            ?: this.selectFirst("h4 a")?.text() ?: this.selectFirst("h5 a")?.text()
            ?: this.selectFirst("h6 a")?.text() ?: return null
        title = title.substringBefore(" izle").trim()

        val rawHref = this.selectFirst("a")?.attr("href") ?: return null
        val href = fixUrl(rawHref, mainUrl)
        val posterUrl = ImageHelper.extractPosterUrl(this, mainUrl)

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = try {
            app.post(
                "${mainUrl}/filmara.php",
                headers = NetworkHelper.getRefererHeaders("${mainUrl}/"),
                data = mapOf("s" to query)
            ).document
        } catch (_: Exception) {
            app.get("${mainUrl}/?s=${query}", headers = NetworkHelper.defaultHeaders).document
        }

        return document.select("article.movie").mapNotNull { it.toSearchResult() }
    }

    override suspend fun quickSearch(query: String): List<SearchResponse> = search(query)

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url, headers = NetworkHelper.defaultHeaders).document

        val title = document.selectFirst("section.movie-exp div.movie-exp-title")?.text()?.substringBefore(" izle")?.trim()
            ?: document.selectFirst("h1")?.text()?.trim() ?: return null
        val poster = ImageHelper.extractPosterUrl(document.selectFirst("section.movie-exp") ?: document, mainUrl)
            ?: document.selectFirst("meta[property=og:image]")?.attr("content")
        val yearDiv = document.selectXpath("//div[@class='yap' and contains(strong, 'Vizyon') or contains(strong, 'Yapım')]").text().trim()
        val year = Regex("""(\d{4})""").find(yearDiv)?.groupValues?.get(1)?.toIntOrNull()
        val description = document.selectFirst("section.movie-exp p.aciklama")?.text()?.trim()
        val tags = document.select("section.movie-exp div.catss a").map { it.text().trim() }
        val rating = document.selectFirst("section.movie-exp div.imdb_puan span")?.text()?.split(" ")?.lastOrNull()?.toDoubleOrNull()
        val actors = document.select("section.movie-exp div.oyuncu").mapNotNull {
            val name = it.selectFirst("div.name")?.text()?.trim() ?: return@mapNotNull null
            val img = ImageHelper.extractPosterUrl(it, mainUrl)
            Actor(name, img)
        }

        val recommendations = document.select("div#benzers article").mapNotNull {
            var recName = it.selectFirst("h2 a")?.text() ?: it.selectFirst("h3 a")?.text()
                ?: it.selectFirst("h4 a")?.text() ?: it.selectFirst("h5 a")?.text()
                ?: it.selectFirst("h6 a")?.text() ?: return@mapNotNull null
            recName = recName.substringBefore(" izle").trim()

            val recHref = fixUrl(it.selectFirst("a")?.attr("href") ?: "", mainUrl)
            val recPosterUrl = ImageHelper.extractPosterUrl(it, mainUrl)

            newMovieSearchResponse(recName, recHref, TvType.Movie) {
                this.posterUrl = recPosterUrl
            }
        }

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.year = year
            this.plot = description
            this.tags = tags
            this.score = rating?.let { Score.from10(it) }
            this.recommendations = recommendations
            addActors(actors)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data, headers = NetworkHelper.defaultHeaders).document
        val iframes = mutableListOf<String>()

        val mainIframe = document.selectFirst("div#movie iframe")?.attr("data-src")
            ?: document.selectFirst("div#movie iframe")?.attr("data")
            ?: document.selectFirst("div#movie iframe")?.attr("src")
        if (!mainIframe.isNullOrEmpty()) {
            iframes.add(fixUrl(mainIframe, mainUrl))
        }

        document.select("div.film_part a").forEach {
            val source = it.selectFirst("span")?.text()?.trim() ?: ""
            if (source.lowercase().contains("fragman")) return@forEach

            val href = it.attr("href")
            if (href.isNotEmpty() && !href.startsWith("#")) {
                try {
                    val movDoc = app.get(href, headers = NetworkHelper.getRefererHeaders(data)).document
                    val iframe = movDoc.selectFirst("div#movie iframe")?.attr("data-src")
                        ?: movDoc.selectFirst("div#movie iframe")?.attr("data")
                        ?: movDoc.selectFirst("div#movie iframe")?.attr("src")
                    if (!iframe.isNullOrEmpty()) {
                        iframes.add(fixUrl(iframe, mainUrl))
                    } else {
                        movDoc.select("div#movie p a").forEach { link ->
                            val downloadLink = link.attr("href")
                            if (downloadLink.isNotEmpty() && !downloadLink.startsWith("#")) {
                                iframes.add(fixUrl(downloadLink, mainUrl))
                            }
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }

        for (iframe in iframes.distinct()) {
            if (iframe.contains("jetv.xyz")) {
                try {
                    val jetvDoc = app.get(iframe, headers = NetworkHelper.getRefererHeaders("${mainUrl}/")).document
                    val jetvIframe = jetvDoc.selectFirst("iframe")?.attr("src")
                    if (!jetvIframe.isNullOrEmpty()) {
                        loadExtractor(fixUrl(jetvIframe, mainUrl), "${mainUrl}/", subtitleCallback, callback)
                    }
                } catch (_: Exception) {
                }
            } else {
                loadExtractor(iframe, "${mainUrl}/", subtitleCallback, callback)
            }
        }

        return true
    }
}
