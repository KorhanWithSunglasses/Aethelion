package com.hexated.core

import com.lagradost.cloudstream3.CommonActivity.showToast
import com.lagradost.cloudstream3.app
import java.util.concurrent.ConcurrentHashMap

object DynamicDomainHelper {

    private val domainCache = ConcurrentHashMap<String, String>()
    private const val REMOTE_CONFIG_URL = "https://raw.githubusercontent.com/KorhanWithSunglasses/Aethelion/builds/domains.json"

    // Public Telegram channels for automatic domain tracking
    private val telegramChannels = mapOf(
        "dizipal" to "dizipal",
        "hdfilmcehennemi" to "hdfilmcehennemi"
    )

    suspend fun getActiveDomain(
        providerKey: String,
        hardcodedFallbacks: List<String>,
        testPath: String = ""
    ): String {
        // 1. In-memory cache
        domainCache[providerKey]?.let { cached ->
            return cached
        }

        // 2. Try Telegram Public Web Scraper (Zero-Auth)
        telegramChannels[providerKey]?.let { channel ->
            try {
                val teleUrl = "https://t.me/s/$channel"
                val doc = app.get(teleUrl, timeout = 3L, headers = NetworkHelper.defaultHeaders).document
                val textNodes = doc.select(".tgme_widget_message_text").map { it.text() }
                for (text in textNodes.reversed()) {
                    val foundUrl = Regex("""https?://[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}""").find(text)?.value
                    if (foundUrl != null && testDomain(foundUrl, testPath)) {
                        domainCache[providerKey] = foundUrl
                        showToast("$providerKey güncel adrese bağlandı.")
                        return foundUrl
                    }
                }
            } catch (_: Exception) {
                // Ignore telegram network failures
            }
        }

        // 3. Try Remote Config from GitHub (domains.json)
        try {
            val res = app.get(REMOTE_CONFIG_URL, timeout = 3L, headers = NetworkHelper.defaultHeaders).text
            val regex = Regex(""""$providerKey"\s*:\s*"([^"]+)"""")
            val remoteUrl = regex.find(res)?.groupValues?.get(1)
            if (!remoteUrl.isNullOrEmpty() && testDomain(remoteUrl, testPath)) {
                domainCache[providerKey] = remoteUrl
                return remoteUrl
            }
        } catch (_: Exception) {
            // Ignore remote config failures
        }

        // 4. Try hardcoded verified fallbacks
        for (fallback in hardcodedFallbacks) {
            if (testDomain(fallback, testPath)) {
                domainCache[providerKey] = fallback
                return fallback
            }
        }

        // 5. Default fallback
        val defaultUrl = hardcodedFallbacks.firstOrNull() ?: ""
        domainCache[providerKey] = defaultUrl
        return defaultUrl
    }

    private suspend fun testDomain(domain: String, testPath: String): Boolean {
        return try {
            val fullUrl = if (testPath.isNotEmpty()) {
                if (domain.endsWith("/") || testPath.startsWith("/")) "$domain$testPath" else "$domain/$testPath"
            } else {
                domain
            }
            val res = app.get(fullUrl, timeout = 3L, headers = NetworkHelper.defaultHeaders)
            res.code in 200..399
        } catch (_: Exception) {
            false
        }
    }
}
