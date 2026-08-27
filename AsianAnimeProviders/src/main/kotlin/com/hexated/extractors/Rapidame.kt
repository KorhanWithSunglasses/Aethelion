package com.hexated.extractors

import com.hexated.core.JsUnpacker
import com.hexated.core.NetworkHelper
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities

open class Rapidame : ExtractorApi() {
    override var name = "Rapidame"
    override var mainUrl = "https://rapidame.net"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        val headers = NetworkHelper.getRefererHeaders(referer ?: mainUrl)
        val response = app.get(url, headers = headers).text

        val unpacked = if (response.contains("eval(function(p,a,c,k,e,")) {
            JsUnpacker.unpack(response) ?: response
        } else {
            response
        }

        val m3u8Regex = Regex("""file:\s*["']([^"']+\.m3u8[^"']*)["']""")
        val m3u8Url = m3u8Regex.find(unpacked)?.groupValues?.get(1)
            ?: Regex("""source\s*:\s*["']([^"']+\.m3u8[^"']*)["']""").find(unpacked)?.groupValues?.get(1)
            ?: return null

        return listOf(
            ExtractorLink(
                source = name,
                name = name,
                url = m3u8Url,
                referer = url,
                quality = Qualities.P1080.value,
                isM3u8 = true,
                headers = NetworkHelper.getStreamHeaders("https://rapidame.net", url)
            )
        )
    }
}
