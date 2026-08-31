package com.hexated.core

import com.lagradost.cloudstream3.app
import java.util.concurrent.ConcurrentHashMap

object DynamicDomainHelper {

    private val domainCache = ConcurrentHashMap<String, String>()

    suspend fun getActiveDomain(
        providerKey: String,
        hardcodedFallbacks: List<String>,
        testPath: String = ""
    ): String {
        // 1. Check in-memory cache
        domainCache[providerKey]?.let { cached ->
            return cached
        }

        // 2. Try fallbacks in order with fast timeout
        for (fallback in hardcodedFallbacks) {
            if (testDomain(fallback, testPath)) {
                domainCache[providerKey] = fallback
                return fallback
            }
        }

        // 3. Fallback to first non-empty domain
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
