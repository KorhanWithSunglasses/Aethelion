package com.hexated.extractors

import com.hexated.core.JsUnpacker
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink

open class Streamwish : ExtractorApi() {
    override var name = "Streamwish"
    override var mainUrl = "https://streamwish.to"
    override val requiresReferer = false

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
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
                    newExtractorLink(
                        source = this.name,
                        name = "${this.name} Fast",
                        url = streamUrl,
                        type = ExtractorLinkType.M3U8
                    ) {
                        this.referer = url
                        this.quality = Qualities.P1080.value
                    }
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
