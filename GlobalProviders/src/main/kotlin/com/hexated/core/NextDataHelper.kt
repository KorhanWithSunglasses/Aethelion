package com.hexated.core

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.jsoup.nodes.Document

object NextDataHelper {
    val mapper: ObjectMapper = ObjectMapper().apply {
        registerModule(KotlinModule.Builder().build())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }

    fun getNextData(document: Document): JsonNode? {
        val script = document.selectFirst("script#__NEXT_DATA__")?.data()
            ?: document.selectFirst("script:containsData(__NEXT_DATA__)")?.data()
            ?: return null

        val jsonString = if (script.contains("__NEXT_DATA__ =")) {
            script.substringAfter("__NEXT_DATA__ =").substringBefore(";</script>").substringBefore(";\n").trim()
        } else {
            script.trim()
        }

        return try {
            mapper.readTree(jsonString)
        } catch (e: Exception) {
            null
        }
    }

    fun getProps(document: Document): JsonNode? {
        return getNextData(document)?.get("props")?.get("pageProps")
    }
}
