package com.hexated.core

import java.util.regex.Matcher
import java.util.regex.Pattern

object JsUnpacker {
    private val PACKER_PATTERN = Pattern.compile(
        """eval\(function\(p,a,c,k,e,[rd]\)\{.+?\}\s*\('.*?',\s*\d+,\s*\d+,\s*'.*?'\.split\('\|'\),\s*\d+,\s*\{.*?\}\)\)""",
        Pattern.DOTALL
    )

    fun unpack(packedJs: String?): String? {
        if (packedJs.isNullOrEmpty()) return null
        return try {
            val matcher: Matcher = PACKER_PATTERN.matcher(packedJs)
            if (matcher.find()) {
                val match = matcher.group()
                unpackerLogic(match)
            } else {
                unpackerLogic(packedJs)
            }
        } catch (e: Exception) {
            packedJs
        }
    }

    fun unpackAndCombine(packedJs: String?): String? {
        if (packedJs.isNullOrEmpty()) return null
        val result = StringBuilder()
        val matcher: Matcher = PACKER_PATTERN.matcher(packedJs)
        var found = false
        while (matcher.find()) {
            found = true
            val unpacked = unpack(matcher.group())
            if (!unpacked.isNullOrEmpty()) {
                result.append(unpacked).append("\n")
            }
        }
        return if (found) result.toString() else unpack(packedJs)
    }

    private fun unpackerLogic(packedJs: String): String {
        val pattern = Pattern.compile(
            """\}\s*\('(.*?)',\s*(\d+),\s*(\d+),\s*'(.*?)'\.split\('\|'\)""",
            Pattern.DOTALL
        )
        val matcher = pattern.matcher(packedJs)
        if (!matcher.find()) return packedJs

        val payload = matcher.group(1) ?: return packedJs
        val radix = matcher.group(2)?.toIntOrNull() ?: return packedJs
        val count = matcher.group(3)?.toIntOrNull() ?: return packedJs
        val symtab = matcher.group(4)?.split("|") ?: return packedJs

        val unbase = Unbase(radix)
        val wordPattern = Pattern.compile("""\b\w+\b""")
        val wordMatcher = wordPattern.matcher(payload)
        val sb = StringBuffer()

        while (wordMatcher.find()) {
            val word = wordMatcher.group()
            val index = unbase.unbase(word)
            val replacement = if (index in symtab.indices && symtab[index].isNotEmpty()) {
                symtab[index]
            } else {
                word
            }
            wordMatcher.appendReplacement(sb, Matcher.quoteReplacement(replacement))
        }
        wordMatcher.appendTail(sb)
        return sb.toString()
    }

    private class Unbase(private val radix: Int) {
        private val alphabet = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"

        fun unbase(str: String): Int {
            if (radix in 2..36) {
                return str.toIntOrNull(radix) ?: -1
            }
            var result = 0
            for (char in str) {
                val index = alphabet.indexOf(char)
                if (index == -1 || index >= radix) return -1
                result = result * radix + index
            }
            return result
        }
    }
}
