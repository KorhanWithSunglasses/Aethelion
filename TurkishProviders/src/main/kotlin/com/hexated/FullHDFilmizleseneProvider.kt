package com.hexated

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor
import org.jsoup.nodes.Element

class FullHDFilmizleseneProvider : MainAPI() {
    override var mainUrl = "https://www.fullhdfilmizlesene.de"
    override var name = "FullHDFilmizlesene"
    override val hasMainPage = true
    override var lang = "tr"
    override val hasQuickSearch = false
    override val supportedTypes = setOf(TvType.Movie)

    override var sequentialMainPage = true
    override var sequentialMainPageDelay = 500L
    override var sequentialMainPageScrollDelay = 500L

    override val mainPage = mainPageOf(
        "${mainUrl}/en-cok-izlenen-filmler-izle-hd/" to "En Çok İzlenen Filmler",
        "${mainUrl}/filmizle/imdb-puanina-gore-filmler-izle/" to "IMDb Puanına Göre Filmler",
        "${mainUrl}/filmizle/en-cok-begenilen-filmler-izle/" to "En Çok Beğenilen Filmler",
        "${mainUrl}/filmizle/en-son-eklenen-filmler/" to "En Son Eklenen Filmler",
        "${mainUrl}/filmizle/en-cok-yorumlanan-filmler/" to "En Çok Yorumlanan Filmler"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (page <= 1) request.data else "${request.data}page/$page/"
        val document = app.get(url).document
        val home = document.select("li.item, div.film-list li").mapNotNull { it.toSearchResult() }
        return newHomePageResponse(request.name, home)
    }

    private fun Element.toSearchResult(): SearchResponse? {
        val title = this.selectFirst("h2.title, a.title, .film-title, img[alt]")?.let {
            if (it.tagName() == "img") it.attr("alt") else it.text()
        }?.trim()?.ifEmpty { null } ?: return null

        val href = fixUrlNull(this.selectFirst("a")?.attr("href")) ?: return null
        val posterUrl = fixUrlNull(
            this.selectFirst("img")?.attr("data-src")
                ?: this.selectFirst("img")?.attr("src")
        )

        return newMovieSearchResponse(title, href, TvType.Movie) {
            this.posterUrl = posterUrl
        }
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val document = app.get("${mainUrl}/arama/${query}").document
        return document.select("li.item, div.film-list li").mapNotNull { it.toSearchResult() }
    }

    override suspend fun load(url: String): LoadResponse? {
        val document = app.get(url).document
        val title = document.selectFirst("h1.title, h1")?.text()?.trim() ?: return null
        val poster = fixUrlNull(document.selectFirst("div.poster img, img.poster")?.attr("src") ?: document.selectFirst("div.poster img")?.attr("data-src"))
        val description = document.selectFirst("div.ozet, div.description, p.description")?.text()?.trim()
        val year = document.selectFirst("span.year, a[href*='/yil/']")?.text()?.filter { it.isDigit() }?.toIntOrNull()
        val tags = document.select("span.genre a, a[href*='/kategori/']").map { it.text().trim() }
        val rating = document.selectFirst("span.imdb-score, span.imdb")?.text()?.trim()?.toRatingInt()
        val actors = document.select("div.actors a, a[href*='/oyuncu/']").map { Actor(it.text().trim()) }
        val trailer = document.selectFirst("div.trailer iframe")?.attr("src")?.let { fixUrlNull(it) }

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.year = year
            this.plot = description
            this.tags = tags
            this.rating = rating
            addActors(actors)
            addTrailer(trailer)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val document = app.get(data).document
        val iframes = mutableListOf<String>()

        document.select("iframe").forEach {
            val src = it.attr("src").ifEmpty { it.attr("data-src") }
            if (src.isNotEmpty() && !src.startsWith("#")) {
                iframes.add(fixUrl(src))
            }
        }

        document.select("div.player-sources a, div.sources a, button[data-src]").forEach {
            val src = it.attr("data-src").ifEmpty { it.attr("href") }
            if (src.isNotEmpty() && !src.startsWith("#")) {
                iframes.add(fixUrl(src))
            }
        }

        for (iframe in iframes.distinct()) {
            loadExtractor(iframe, "$mainUrl/", subtitleCallback, callback)
        }

        return true
    }
}
