package com.hexated.extractors

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.hexated.core.JsUnpacker

open class Rapidame : ExtractorApi() {
    override var name = "Rapidame"
    override var mainUrl = "https://rapidame.net"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (suspend () -> Unit)?,
        callback: (ExtractorLink) -> Unit
    ) {
        try {
            val response = app.get(
                url,
                headers = mapOf("Referer" to (referer ?: mainUrl))
            ).text

            val unpacked = JsUnpacker.unpackAndCombine(response) ?: response
            val m3u8Match = Regex("""file:\s*["']([^"']+\.m3u8[^"']*)["']""").find(unpacked)
                ?: Regex("""["'](https?://[^"']+\.m3u8[^"']*)["']""").find(unpacked)

            m3u8Match?.let { match ->
                val streamUrl = match.groupValues[1]
                callback.invoke(
                    ExtractorLink(
                        source = this.name,
                        name = "${this.name} Multi",
                        url = streamUrl,
                        referer = url,
                        quality = Qualities.P1080.value,
                        type = ExtractorLinkType.M3U8
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
