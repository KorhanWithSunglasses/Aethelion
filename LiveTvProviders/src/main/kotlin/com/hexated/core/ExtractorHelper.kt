@file:Suppress("DEPRECATION")
package com.hexated.core

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.INFER_TYPE
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.loadExtractor

object ExtractorHelper {
    suspend fun resolveStream(
        url: String,
        referer: String? = null,
        name: String = "Sunucu",
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val cleanUrl = if (url.startsWith("//")) "https:$url" else url

        // 1. Direct M3U8 Stream
        if (cleanUrl.contains(".m3u8")) {
            callback.invoke(
                ExtractorLink(
                    source = name,
                    name = "$name HD",
                    url = cleanUrl,
                    referer = referer ?: "",
                    quality = Qualities.P1080.value,
                    type = INFER_TYPE,
                    headers = NetworkHelper.getStreamHeaders(referer ?: cleanUrl, referer ?: cleanUrl)
                )
            )
            return
        }

        // 2. Direct MP4 Stream
        if (cleanUrl.contains(".mp4")) {
            callback.invoke(
                ExtractorLink(
                    source = name,
                    name = "$name MP4",
                    url = cleanUrl,
                    referer = referer ?: "",
                    quality = Qualities.P1080.value,
                    type = INFER_TYPE,
                    headers = NetworkHelper.getStreamHeaders(referer ?: cleanUrl, referer ?: cleanUrl)
                )
            )
            return
        }

        // 3. Universal CloudStream Extractor
        try {
            loadExtractor(
                url = cleanUrl,
                referer = referer,
                subtitleCallback = subtitleCallback,
                callback = callback
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
