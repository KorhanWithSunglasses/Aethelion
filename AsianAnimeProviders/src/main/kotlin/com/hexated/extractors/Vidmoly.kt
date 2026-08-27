package com.hexated.extractors

import com.hexated.core.JsUnpacker
import com.hexated.core.NetworkHelper
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities

open class Vidmoly : ExtractorApi() {
    override var name = "Vidmoly"
    override var mainUrl = "https://vidmoly.to"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?): List<ExtractorLink>? {
        val cleanUrl = if (url.contains("/embed-") || url.contains("/w/")) {
            url
        } else {
            val id = url.substringAfterLast("/")
            "https://vidmoly.to/embed-$id.html"
        }

        val headers = NetworkHelper.getRefererHeaders(referer ?: mainUrl)
        val response = app.get(cleanUrl, headers = headers).text

        val unpacked = if (response.contains("eval(function(p,a,c,k,e,")) {
            JsUnpacker.unpack(response) ?: response
        } else {
            response
        }

        val m3u8Regex = Regex("""file:\s*["']([^"']+\.m3u8[^"']*)["']""")
        val m3u8Url = m3u8Regex.find(unpacked)?.groupValues?.get(1)
            ?: Regex("""sources:\s*\[\{\s*file:\s*["']([^"']+)""").find(unpacked)?.groupValues?.get(1)
            ?: return null

        return listOf(
            ExtractorLink(
                source = name,
                name = name,
                url = m3u8Url,
                referer = cleanUrl,
                quality = Qualities.P1080.value,
                isM3u8 = true,
                headers = NetworkHelper.getStreamHeaders("https://vidmoly.to", cleanUrl)
            )
        )
    }
}
