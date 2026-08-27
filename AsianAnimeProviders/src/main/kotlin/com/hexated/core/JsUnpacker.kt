package com.hexated.core

import java.util.regex.Pattern

/**
 * Dean Edwards P.A.C.K.E.R. deobfuscator for CloudStream Extractors.
 * Resolves eval(function(p,a,c,k,e,d)...) obfuscated scripts.
 */
object JsUnpacker {

    private val PACKED_PATTERN = Pattern.compile(
        """eval\(function\(p,a,c,k,e,[rd]\)\{.*?\}\s*\('(.*?)'\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*'(.*?)'\.split\('\|'\)""",
        Pattern.DOTALL
    )

    fun unpack(packedJs: String): String? {
        val matcher = PACKED_PATTERN.matcher(packedJs)
        if (!matcher.find()) return null

        try {
            val payload = matcher.group(1) ?: return null
            val radixStr = matcher.group(2) ?: return null
            val countStr = matcher.group(3) ?: return null
            val symtabStr = matcher.group(4) ?: return null

            val radix = radixStr.toInt()
            val count = countStr.toInt()
            val symtab = symtabStr.split("|")

            fun lookup(word: String): String {
                val index = unbase(word, radix)
                return if (index < symtab.size && symtab[index].isNotEmpty()) {
                    symtab[index]
                } else {
                    word
                }
            }

            val wordPattern = Pattern.compile("""\b\w+\b""")
            val wordMatcher = wordPattern.matcher(payload)
            val sb = StringBuffer()

            while (wordMatcher.find()) {
                val word = wordMatcher.group()
                val replacement = lookup(word)
                wordMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement))
            }
            wordMatcher.appendTail(sb)

            return sb.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun unpackAndExtract(packedJs: String, regexPattern: String): String? {
        val unpacked = unpack(packedJs) ?: packedJs
        val pattern = Pattern.compile(regexPattern)
        val matcher = pattern.matcher(unpacked)
        return if (matcher.find()) matcher.group(1) else null
    }

    private fun unbase(str: String, radix: Int): Int {
        var result = 0
        val digits = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
        for (ch in str) {
            val idx = digits.indexOf(ch)
            if (idx == -1 || idx >= radix) return 0
            result = result * radix + idx
        }
        return result
    }
}
