package com.hexated

import com.hexated.core.NetworkHelper
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.ExtractorLinkType

class UlusalTvProvider : MainAPI() {
    override var name = "Ulusal Canlı TV"
    override var mainUrl = "https://www.trtizle.com"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Live)

    data class Channel(
        val name: String,
        val streamUrl: String,
        val posterUrl: String,
        val referer: String = "",
        val origin: String = ""
    )

    private val channels = listOf(
        Channel(
            "TRT 1 HD",
            "https://tv-trt1.medya.trt.com.tr/master.m3u8",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/8/82/TRT_1_logo_2021.svg/512px-TRT_1_logo_2021.svg.png",
            "https://www.trtizle.com/"
        ),
        Channel(
            "TRT Spor HD",
            "https://tv-trtspor.medya.trt.com.tr/master.m3u8",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/6/6b/TRT_Spor_logo_2021.svg/512px-TRT_Spor_logo_2021.svg.png",
            "https://www.trtizle.com/"
        ),
        Channel(
            "TRT Belgesel HD",
            "https://tv-trtbelgesel.medya.trt.com.tr/master.m3u8",
            "https://upload.wikimedia.org/wikipedia/commons/thumb/e/e0/TRT_Belgesel_logo_2021.svg/512px-TRT_Belgesel_logo_2021.svg.png",
            "https://www.trtizle.com/"
        ),
        Channel(
            "ATV HD",
            "https://trkvz-live.ercdn.net/atvhd/atvhd.m3u8",
            "https://iaatv.tmgrup.com.tr/static/images/atv-logo.png",
            "https://www.atv.com.tr/"
        ),
        Channel(
            "A Spor HD",
            "https://trkvz-live.ercdn.net/asporhd/asporhd.m3u8",
            "https://iaaspr.tmgrup.com.tr/static/images/aspor-logo.png",
            "https://www.aspor.com.tr/"
        ),
        Channel(
            "Kanal D HD",
            "https://live.duhnet.tv/S2/HLS_LIVE/kanaldnp/playlist.m3u8",
            "https://i.kanald.com.tr/i/kanald/75/0x0/5cd96e574936480b583e6a4b.png",
            "https://www.kanald.com.tr/"
        ),
        Channel(
            "Show TV HD",
            "https://c.showtv.com.tr/live/showtv/index.m3u8",
            "https://mo.ciner.com.tr/showtv/assets/images/showtv-logo.png",
            "https://www.showtv.com.tr/"
        ),
        Channel(
            "Star TV HD",
            "https://dogus-live.daioncdn.net/startv/startv.m3u8",
            "https://media.startv.com.tr/startv-logo.png",
            "https://www.startv.com.tr/"
        ),
        Channel(
            "TV8 HD",
            "https://tv8-live.ercdn.net/tv8/tv8.m3u8",
            "https://www.tv8.com.tr/images/tv8-logo.png",
            "https://www.tv8.com.tr/"
        ),
        Channel(
            "Teve2 HD",
            "https://live.duhnet.tv/S2/HLS_LIVE/teve2np/playlist.m3u8",
            "https://i.teve2.com.tr/i/teve2/75/0x0/55bb24c36c70b809a47d25e0.png",
            "https://www.teve2.com.tr/"
        ),
        Channel(
            "TLC HD",
            "https://dogus-live.daioncdn.net/tlc/tlc.m3u8",
            "https://img-tlctv.mncdn.com/static/images/tlc-logo.png",
            "https://www.tlctv.com.tr/"
        ),
        Channel(
            "DMAX HD",
            "https://dogus-live.daioncdn.net/dmax/dmax.m3u8",
            "https://img-dmaxtv.mncdn.com/static/images/dmax-logo.png",
            "https://www.dmax.com.tr/"
        ),
        Channel(
            "HaberTürk TV",
            "https://c.haberturk.com/live/haberturk/index.m3u8",
            "https://mo.ciner.com.tr/haberturk/assets/images/haberturk-logo.png",
            "https://www.haberturk.tv/"
        ),
        Channel(
            "NTV HD",
            "https://dogus-live.daioncdn.net/ntv/ntv.m3u8",
            "https://cdn.ntv.com.tr/img/ntv-logo.png",
            "https://www.ntv.com.tr/"
        )
    )

    override val mainPage = mainPageOf(
        "ulusal" to "Ulusal Kanallar",
        "spor" to "Spor Kanalları",
        "haber" to "Haber Kanalları",
        "belgesel" to "Belgesel & Yaşam"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val filtered = when (request.data) {
            "spor" -> channels.filter { it.name.contains("Spor", ignoreCase = true) }
            "haber" -> channels.filter { it.name.contains("Haber", ignoreCase = true) || it.name.contains("NTV", ignoreCase = true) }
            "belgesel" -> channels.filter { it.name.contains("Belgesel", ignoreCase = true) || it.name.contains("TLC", ignoreCase = true) || it.name.contains("DMAX", ignoreCase = true) }
            else -> channels
        }

        val list = filtered.map { ch ->
            newLiveSearchResponse(ch.name, ch.name, TvType.Live) {
                this.posterUrl = ch.posterUrl
            }
        }

        return newHomePageResponse(request.name, list)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        return channels.filter { it.name.contains(query, ignoreCase = true) }.map { ch ->
            newLiveSearchResponse(ch.name, ch.name, TvType.Live) {
                this.posterUrl = ch.posterUrl
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val ch = channels.firstOrNull { it.name == url } ?: channels.first()
        return newLiveStreamLoadResponse(ch.name, ch.name, ch.streamUrl) {
            this.posterUrl = ch.posterUrl
            this.plot = "${ch.name} Canlı TV Yayını"
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val ch = channels.firstOrNull { it.streamUrl == data || it.name == data } ?: return false

        callback(
            ExtractorLink(
                source = name,
                name = ch.name,
                url = ch.streamUrl,
                referer = ch.referer.ifEmpty { mainUrl },
                quality = Qualities.P1080.value,
                type = ExtractorLinkType.M3U8,
                headers = NetworkHelper.getStreamHeaders(ch.origin.ifEmpty { ch.referer }, ch.referer.ifEmpty { mainUrl })
            )
        )
        return true
    }
}
