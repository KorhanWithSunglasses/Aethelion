package com.hexated.extractors

import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.ExtractorLinkType

open class CloseLoad : ExtractorApi() {
    override var name = "CloseLoad"
    override var mainUrl = "https://closeload.com"
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

            val m3u8Match = Regex("""["'](https?://[^"']+\.m3u8[^"']*)["']""").find(response)
            m3u8Match?.let { match ->
                val streamUrl = match.groupValues[1]
                callback.invoke(
                    ExtractorLink(
                        source = this.name,
                        name = "${this.name} Fast",
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
