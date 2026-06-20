package io.nekohasekai.sfa.invpn

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class Credentials(
    val username: String,
    val deviceToken: String,
    val saltBodyB64: String,
    val bodyKeyB64: String,
)

/**
 * Persists the device credentials encrypted under an AES key held in the Android
 * Keystore (hardware-backed where available). The password is never stored; only the
 * derived body_key is, so config bodies can be decrypted without re-prompting. The
 * blob in SharedPreferences is useless without the Keystore key (non-exportable).
 */
object SecureStore {
    private const val PREFS = "invpn_secure"
    private const val KEY_BLOB = "creds"
    private const val KS_ALIAS = "invpn_master"
    private val json = Json

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun masterKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getEntry(KS_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }
        val kg = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        kg.init(
            KeyGenParameterSpec.Builder(
                KS_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return kg.generateKey()
    }

    fun save(ctx: Context, creds: Credentials) {
        val pt = json.encodeToString(Credentials.serializer(), creds).toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, masterKey())
        }
        val blob = Crypto.b64UrlEncode(cipher.iv) + ":" + Crypto.b64UrlEncode(cipher.doFinal(pt))
        prefs(ctx).edit().putString(KEY_BLOB, blob).apply()
    }

    fun load(ctx: Context): Credentials? {
        val blob = prefs(ctx).getString(KEY_BLOB, null) ?: return null
        return try {
            val parts = blob.split(":", limit = 2)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, masterKey(), GCMParameterSpec(128, Crypto.b64UrlDecode(parts[0])))
            }
            val pt = cipher.doFinal(Crypto.b64UrlDecode(parts[1]))
            json.decodeFromString(Credentials.serializer(), String(pt, Charsets.UTF_8))
        } catch (e: Exception) {
            null
        }
    }

    fun isLoggedIn(ctx: Context): Boolean = prefs(ctx).contains(KEY_BLOB)

    fun clear(ctx: Context) {
        prefs(ctx).edit().remove(KEY_BLOB).apply()
    }
}
