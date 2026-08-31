package com.hexated.core

import android.util.Base64
import org.json.JSONObject
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object DiziPalCryptoHelper {
    private const val PASSPHRASE = "3hPn4uCjTVtfYWcjIcoJQ4cL1WWk1qxXI39egLYOmNv6IblA7eKJz68uU3eLzux1biZLCms0quEjTYniGv5z1JcKbNIsDQFSeIZOBZJz4is6pD7UyWDggWWzTLBQbHcQFpBQdClnuQaMNUHtLHTpzCvZy33p6I7wFBvL4fnXBYH84aUIyWGTRvM2G5cfoNf4705tO2kv"

    fun decrypt(encryptedJsonString: String): String? {
        return try {
            val json = JSONObject(encryptedJsonString.replace("&quot;", "\""))
            val ciphertext = Base64.decode(json.getString("ciphertext"), Base64.DEFAULT)
            val salt = hexStringToByteArray(json.getString("salt"))
            val iv = hexStringToByteArray(json.getString("iv"))

            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
            val spec: KeySpec = PBEKeySpec(PASSPHRASE.toCharArray(), salt, 999, 256)
            val tmp = factory.generateSecret(spec)
            val secret = SecretKeySpec(tmp.encoded, "AES")

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secret, IvParameterSpec(iv))
            val decryptedBytes = cipher.doFinal(ciphertext)

            String(decryptedBytes, Charsets.UTF_8).trim()
        } catch (_: Exception) {
            null
        }
    }

    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4) + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
