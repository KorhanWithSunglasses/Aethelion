package com.hexated.core

import org.jsoup.nodes.Element

object ImageHelper {
    fun extractPosterUrl(element: Element, baseUrl: String): String? {
        val img = element.selectFirst("img") ?: element
        val candidates = listOf(
            img.attr("data-src"),
            img.attr("data-original"),
            img.attr("data-lazy-src"),
            img.attr("data-srcset"),
            img.attr("srcset"),
            img.attr("src")
        )

        for (cand in candidates) {
            if (cand.isEmpty() || cand.startsWith("data:")) continue
            val clean = cand.split(" ").firstOrNull { 
                it.startsWith("http") || it.startsWith("/") || it.startsWith("//") 
            } ?: cand
            if (clean.isEmpty() || clean.startsWith("data:") || clean.endsWith(".svg") || clean.endsWith(".gif")) continue
            return fixUrl(clean, baseUrl)
        }

        val style = element.attr("style").ifEmpty { element.selectFirst("[style*=\"background\"]")?.attr("style") ?: "" }
        if (style.contains("url(")) {
            val bgMatch = Regex("""url\(['"]?(.*?)['"]?\)""").find(style)?.groupValues?.get(1)
            if (!bgMatch.isNullOrEmpty() && !bgMatch.startsWith("data:")) {
                return fixUrl(bgMatch, baseUrl)
            }
        }

        return null
    }
}
