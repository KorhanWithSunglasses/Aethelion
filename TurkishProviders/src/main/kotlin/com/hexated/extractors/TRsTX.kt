package com.hexated.extractors

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.Qualities

open class TRsTX : ExtractorApi() {
    override val name = "TRsTX"
    override val mainUrl = "https://trstx.org"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val extRef = referer ?: ""
        val videoReq = app.get(url, referer = extRef).text

        val file = Regex("""file":"([^"]+)""").find(videoReq)?.groupValues?.get(1) ?: return
        val postLink = "${mainUrl}/" + file.replace("\\", "")
        val rawList = app.post(postLink, referer = extRef).parsedSafe<List<Any>>() ?: return

        val postJson = rawList.drop(1).mapNotNull { item ->
            val mapItem = item as? Map<*, *> ?: return@mapNotNull null
            TrstxVideoData(
                title = mapItem["title"] as? String,
                file = mapItem["file"] as? String
            )
        }

        for (item in postJson) {
            val f = item.file ?: continue
            val title = item.title ?: continue
            val fileUrl = "${mainUrl}/playlist/" + f.substring(1) + ".txt"
            val videoData = app.post(fileUrl, referer = extRef).text

            callback.invoke(
                ExtractorLink(
                    source = this.name,
                    name = "${this.name} - $title",
                    url = videoData,
                    referer = extRef,
                    quality = Qualities.Unknown.value,
                    type = INFER_TYPE
                )
            )
        }
    }

    data class TrstxVideoData(
        @JsonProperty("title") val title: String? = null,
        @JsonProperty("file") val file: String? = null
    )
}
