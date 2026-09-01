package com.hexated.extractors

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities

open class TurboImgz : ExtractorApi() {
    override val name = "TurboImgz"
    override val mainUrl = "https://turbo.imgz.me"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val extRef = referer ?: ""
        val videoReq = app.get(url.substringAfter("||"), referer = extRef).text

        val videoLink = Regex("""file:\s*"(.*)",""").find(videoReq)?.groupValues?.get(1) ?: return

        callback.invoke(
            ExtractorLink(
                source = "${this.name} - " + url.substringBefore("||").uppercase(),
                name = "${this.name} - " + url.substringBefore("||").uppercase(),
                url = videoLink,
                referer = extRef,
                quality = Qualities.Unknown.value,
                isM3u8 = true
            )
        )
    }
}
