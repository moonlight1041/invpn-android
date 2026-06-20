package io.nekohasekai.sfa.invpn

import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * InVPN config-delivery crypto. Deliberately pure JVM (javax.crypto + hand-rolled
 * base64) with NO Android framework imports, so the interop with the Python backend
 * (vpn-config-api) is verified by a plain JVM unit test (see CryptoTest).
 *
 * Contract (must match backend corelib.py byte-for-byte):
 *   body_key = PBKDF2-HMAC-SHA256(utf8(password), salt_body, 210000, 32 bytes)
 *   config   = AES-256-GCM(body_key, nonce=12B, ct=ciphertext||tag(16B), aad=none)
 *   wire encoding = URL-safe base64, no padding
 *
 * body_key never crosses the wire (only the password does, once, at login/register).
 */
object Crypto {

    const val PBKDF2_ITERS = 210_000

    private const val ALPHABET =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_"

    /** URL-safe base64 decode, tolerant of padding/whitespace. */
    fun b64UrlDecode(s: String): ByteArray {
        val rev = IntArray(128) { -1 }
        for (i in ALPHABET.indices) rev[ALPHABET[i].code] = i
        val out = ArrayList<Byte>(s.length * 3 / 4 + 3)
        var buf = 0
        var bits = 0
        for (c in s) {
            if (c == '=') break
            val v = if (c.code < 128) rev[c.code] else -1
            if (v < 0) continue // skip newlines / non-alphabet
            buf = (buf shl 6) or v
            bits += 6
            if (bits >= 8) {
                bits -= 8
                out.add(((buf ushr bits) and 0xFF).toByte())
            }
        }
        return out.toByteArray()
    }

    /** URL-safe base64 encode, no padding. */
    fun b64UrlEncode(data: ByteArray): String {
        val sb = StringBuilder((data.size + 2) / 3 * 4)
        var buf = 0
        var bits = 0
        for (b in data) {
            buf = (buf shl 8) or (b.toInt() and 0xFF)
            bits += 8
            while (bits >= 6) {
                bits -= 6
                sb.append(ALPHABET[(buf ushr bits) and 0x3F])
            }
        }
        if (bits > 0) sb.append(ALPHABET[(buf shl (6 - bits)) and 0x3F])
        return sb.toString()
    }

    /**
     * PBKDF2-HMAC-SHA256 over the raw password bytes. Implemented by hand (rather than
     * SecretKeyFactory) to avoid the JVM/Android charset ambiguity in PBEKeySpec and
     * guarantee a byte-exact match with Python's hashlib.pbkdf2_hmac.
     */
    fun deriveBodyKey(password: String, salt: ByteArray, iterations: Int = PBKDF2_ITERS): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(password.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val hLen = mac.macLength
        val dkLen = 32
        val out = ByteArray(dkLen)
        var offset = 0
        var blockIndex = 1
        while (offset < dkLen) {
            mac.update(salt)
            mac.update(
                byteArrayOf(
                    (blockIndex ushr 24).toByte(),
                    (blockIndex ushr 16).toByte(),
                    (blockIndex ushr 8).toByte(),
                    blockIndex.toByte(),
                ),
            )
            var u = mac.doFinal()
            val t = u.copyOf()
            for (j in 1 until iterations) {
                u = mac.doFinal(u)
                for (k in t.indices) t[k] = (t[k].toInt() xor u[k].toInt()).toByte()
            }
            val len = minOf(hLen, dkLen - offset)
            System.arraycopy(t, 0, out, offset, len)
            offset += len
            blockIndex++
        }
        return out
    }

    /** AES-256-GCM decrypt. [ctWithTag] is ciphertext with the 16-byte tag appended. */
    fun aesGcmDecrypt(key: ByteArray, nonce: ByteArray, ctWithTag: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, nonce))
        return cipher.doFinal(ctWithTag)
    }

    /** Decrypt a /config response body using an already-derived body key. */
    fun decryptWithBodyKey(bodyKey: ByteArray, nonceB64: String, ctB64: String): ByteArray =
        aesGcmDecrypt(bodyKey, b64UrlDecode(nonceB64), b64UrlDecode(ctB64))
}
