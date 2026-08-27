package com.hexated.core

object NetworkHelper {

    const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    val defaultHeaders: Map<String, String> = mapOf(
        "User-Agent" to USER_AGENT,
        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
        "Accept-Language" to "tr-TR,tr;q=0.9,en-US;q=0.8,en;q=0.7",
        "Sec-Ch-Ua" to """"Chromium";v="122", "Not(A:Brand";v="24", "Google Chrome";v="122"""",
        "Sec-Ch-Ua-Mobile" to "?0",
        "Sec-Ch-Ua-Platform" to """"Windows""""
    )

    fun getRefererHeaders(referer: String): Map<String, String> {
        return defaultHeaders + mapOf("Referer" to referer)
    }

    fun getStreamHeaders(origin: String, referer: String): Map<String, String> {
        return mapOf(
            "User-Agent" to USER_AGENT,
            "Origin" to origin,
            "Referer" to referer
        )
    }
}
