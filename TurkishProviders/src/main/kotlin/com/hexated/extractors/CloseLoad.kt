package com.hexated.extractors

import android.util.Base64
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.fixUrl
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.getAndUnpack

open class CloseLoad : ExtractorApi() {
    override val name = "CloseLoad"
    override val mainUrl = "https://closeload.filmmakinesi.de"
    override val requiresReferer = true

    private fun getm3uLink(data: String): String {
        val first = Base64.decode(data, Base64.DEFAULT).reversedArray()
        val second = Base64.decode(first, Base64.DEFAULT)
        val result = second.toString(Charsets.UTF_8).split("|")[1]
        return result
    }

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val extRef = referer ?: ""
        val iSource = app.get(url, referer = extRef)

        iSource.document.select("track").forEach {
            subtitleCallback.invoke(
                SubtitleFile(
                    lang = it.attr("label"),
                    url = fixUrl(it.attr("src"))
                )
            )
        }

        val scripts = iSource.document.select("script[type=text/javascript]")
        for (s in scripts) {
            val scriptData = s.data().trim()
            if (scriptData.contains("eval(function")) {
                try {
                    val rawScript = getAndUnpack(scriptData)
                    val match = Regex("""return result\}var .*?=.*?\("(.*?)"\)""").find(rawScript)
                    if (match != null) {
                        val data = match.groupValues[1]
                        val m3uLink = getm3uLink(data)
                        callback.invoke(
                            ExtractorLink(
                                source = this.name,
                                name = this.name,
                                url = m3uLink,
                                referer = mainUrl,
                                quality = Qualities.Unknown.value,
                                isM3u8 = true
                            )
                        )
                        return
                    }
                } catch (_: Exception) {
                }
            }
        }
    }
}
