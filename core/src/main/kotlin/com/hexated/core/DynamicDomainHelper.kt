package com.hexated.core

import com.lagradost.cloudstream3.app
import java.util.concurrent.ConcurrentHashMap

/**
 * 3-Tier Dynamic Domain & Fallback Resolver.
 * L1: Local in-memory cache
 * L2: Remote Config / GitHub raw json
 * L3: Hardcoded fallback mirrors
 */
object DynamicDomainHelper {

    private val domainCache = ConcurrentHashMap<String, String>()

    // Remote manifest URL (hosted on the repository builds/config branch)
    private const val REMOTE_CONFIG_URL = "https://raw.githubusercontent.com/username/CloudStreamRepo/builds/domains.json"

    suspend fun getActiveDomain(
        providerKey: String,
        hardcodedFallbacks: List<String>,
        testPath: String = ""
    ): String {
        // L1: Check memory cache
        domainCache[providerKey]?.let { cached ->
            if (testDomain(cached, testPath)) {
                return cached
            }
        }

        // L2: Try remote config
        try {
            val response = app.get(REMOTE_CONFIG_URL, timeout = 3L).text
            // Simple key-value lookup or regex
            val remoteDomain = extractDomainFromConfig(response, providerKey)
            if (remoteDomain != null && testDomain(remoteDomain, testPath)) {
                domainCache[providerKey] = remoteDomain
                return remoteDomain
            }
        } catch (_: Exception) {
            // Ignore remote config network failures
        }

        // L3: Iterate hardcoded fallback mirrors
        for (fallback in hardcodedFallbacks) {
            if (testDomain(fallback, testPath)) {
                domainCache[providerKey] = fallback
                return fallback
            }
        }

        // Fallback to first mirror if all tests fail
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
            val res = app.get(fullUrl, timeout = 5L, headers = NetworkHelper.defaultHeaders)
            res.code in 200..399
        } catch (_: Exception) {
            false
        }
    }

    private fun extractDomainFromConfig(jsonStr: String, key: String): String? {
        val regex = Regex(""""$key"\s*:\s*"([^"]+)"""")
        return regex.find(jsonStr)?.groupValues?.get(1)
    }
}
