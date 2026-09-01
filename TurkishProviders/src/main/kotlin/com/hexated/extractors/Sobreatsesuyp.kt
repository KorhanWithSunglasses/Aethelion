package com.hexated.extractors

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.getAndUnpack
import com.lagradost.cloudstream3.utils.newExtractorLink

open class Sobreatsesuyp : ExtractorApi() {
    override val name = "Sobreatsesuyp"
    override val mainUrl = "https://sobreatsesuyp.com"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val extRef = referer ?: ""
        val iSource = app.get(url, referer = extRef)

        val scripts = iSource.document.select("script[type=text/javascript]")
        for (s in scripts) {
            val scriptData = s.data().trim()
            if (scriptData.contains("eval(function")) {
                try {
                    val rawScript = getAndUnpack(scriptData)
                    val match = Regex("""sources:\[\{file:"(.*?)"""").find(rawScript)
                    if (match != null) {
                        val videoLink = match.groupValues[1]
                        val isHls = videoLink.contains(".m3u8")
                        callback.invoke(
                            newExtractorLink(
                                source = this.name,
                                name = this.name,
                                url = videoLink,
                                type = INFER_TYPE
                            ) {
                                this.referer = mainUrl
                                this.quality = if (isHls) Qualities.Unknown.value else Qualities.P1080.value
                            }
                        )
                        return
                    }
                } catch (_: Exception) {
                }
            }
        }
    }
}
