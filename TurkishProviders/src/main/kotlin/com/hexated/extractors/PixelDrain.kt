package com.hexated.extractors

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink

open class PixelDrain : ExtractorApi() {
    override val name = "PixelDrain"
    override val mainUrl = "https://pixeldrain.com"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val pixelId = Regex("""([^/]+)(?=\?download)""").find(url)?.groupValues?.get(1) ?: return
        val downloadLink = "${mainUrl}/api/file/${pixelId}?download"

        callback.invoke(
            newExtractorLink(
                source = "pixeldrain - $pixelId",
                name = "pixeldrain - $pixelId",
                url = downloadLink,
                type = ExtractorLinkType.M3U8
            ) {
                this.referer = "${mainUrl}/u/${pixelId}?download"
                this.quality = Qualities.Unknown.value
            }
        )
    }
}
