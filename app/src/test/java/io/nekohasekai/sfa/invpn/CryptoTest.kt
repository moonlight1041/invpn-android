package io.nekohasekai.sfa.invpn

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Cross-implementation interop test. The vector below was produced by the Python
 * backend (vpn-config-api/_dev_e2e_vector.py). If this passes, the Android client
 * derives the same body_key and decrypts a /config body that the server encrypted.
 */
class CryptoTest {

    private val password = "correct horse battery staple"
    private val saltBodyB64 = "MDEyMzQ1Njc4OWFiY2RlZg"
    private val expectedBodyKeyB64 = "H_ZMdkjDEDpZ4T7DlMYQs7RkF9UgyowFSsi_pzKxi-s"
    private val nonceB64 = "SU5WUE5ub25jZTEy"
    private val ctB64 = "LimGDJm-eBnxuEakLIcgPjjV9kIQA46ME3P0BjgDK-DMgdVlPxj_Hu0VqgVF"
    private val expectedPlaintext = "{\"invpn\":\"interop-ok\",\"n\":42}"

    @Test
    fun base64UrlRoundTrips() {
        val bytes = byteArrayOf(0, 1, 2, -1, -2, 65, 66, 67, 100)
        assertEquals(
            "decode(encode(x)) == x",
            bytes.toList(),
            Crypto.b64UrlDecode(Crypto.b64UrlEncode(bytes)).toList(),
        )
    }

    @Test
    fun pbkdf2MatchesBackendVector() {
        val bodyKey = Crypto.deriveBodyKey(password, Crypto.b64UrlDecode(saltBodyB64))
        assertEquals(expectedBodyKeyB64, Crypto.b64UrlEncode(bodyKey))
    }

    @Test
    fun decryptsBackendEncryptedConfig() {
        val bodyKey = Crypto.deriveBodyKey(password, Crypto.b64UrlDecode(saltBodyB64))
        val plaintext = Crypto.decryptWithBodyKey(bodyKey, nonceB64, ctB64)
        assertEquals(expectedPlaintext, String(plaintext, Charsets.UTF_8))
    }
}
