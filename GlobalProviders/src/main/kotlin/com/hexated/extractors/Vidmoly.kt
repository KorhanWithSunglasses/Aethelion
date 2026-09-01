package com.hexated.extractors

import com.hexated.core.JsUnpacker
import com.hexated.core.NetworkHelper
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink

open class Vidmoly : ExtractorApi() {
    override var name = "Vidmoly"
    override var mainUrl = "https://vidmoly.to"
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
                headers = mapOf(
                    "User-Agent" to NetworkHelper.USER_AGENT,
                    "Referer" to (referer ?: mainUrl)
                )
            ).text

            val unpacked = JsUnpacker.unpackAndCombine(response) ?: response
            var m3u8Url = Regex("""file:\s*["']([^"']+\.m3u8[^"']*)["']""").find(unpacked)?.groupValues?.get(1)
                ?: Regex("""["'](https?://[^"']+\.m3u8[^"']*)["']""").find(unpacked)?.groupValues?.get(1)

            if (m3u8Url == null && (url.contains("molystream") || url.contains("vidmoly"))) {
                if (url.contains("/embed/") && !url.contains("/embed/sheila/")) {
                    m3u8Url = url.replace("/embed/", "/embed/sheila/")
                }
            }

            m3u8Url?.let { streamUrl ->
                val isM3u8 = streamUrl.contains(".m3u8") || streamUrl.contains("molystream") || streamUrl.contains("vidmoly") || streamUrl.contains("sheila")
                val linkType = if (isM3u8) ExtractorLinkType.M3U8 else INFER_TYPE

                callback.invoke(
                    newExtractorLink(
                        source = this.name,
                        name = "${this.name} 1080p",
                        url = streamUrl,
                        type = linkType
                    ) {
                        this.referer = url
                        this.quality = Qualities.P1080.value
                        this.headers = mapOf(
                            "User-Agent" to NetworkHelper.USER_AGENT,
                            "Referer" to url,
                            "Origin" to (if (url.contains("/embed")) url.substringBefore("/embed") else url)
                        )
                    }
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
