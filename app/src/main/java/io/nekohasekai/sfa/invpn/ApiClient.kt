package io.nekohasekai.sfa.invpn

import android.util.Base64
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class ApiException(val code: Int, message: String) : IOException(message)

@Serializable
data class AuthResult(val device_token: String, val salt_body: String, val level: String = "GOLDEN", val name: String? = null)

@Serializable
data class ConfigBlob(val v: Int = 1, val alg: String = "", val nonce: String, val ct: String)

@Serializable
data class RedeemResult(val device_token: String, val body_key: String, val name: String? = null, val level: String = "GOLDEN")

@Serializable
data class InviteResult(val code: String, val link: String, val level: String = "GOLDEN", val invitee_name: String? = null)

@Serializable
private data class LoginReq(val username: String, val password: String)

@Serializable
private data class RedeemReq(val code: String)

@Serializable
private data class InviteReq(val invitee_name: String?, val level: String)

/**
 * Authenticated config-delivery client (plan §3). Blocking — call off the main thread.
 * TLS via [HttpsURLConnection]; optional SPKI pinning when [InVpnConfig.PINS] is set.
 */
object ApiClient {
    private val json = Json { ignoreUnknownKeys = true }

    fun login(username: String, password: String): AuthResult =
        json.decodeFromString(
            AuthResult.serializer(),
            postJson("/api/v1/login", json.encodeToString(LoginReq.serializer(), LoginReq(username, password))),
        )

    /** Passwordless invite redemption — server returns device_token + body_key + name + level. */
    fun redeem(code: String): RedeemResult =
        json.decodeFromString(
            RedeemResult.serializer(),
            postJson("/api/v1/redeem", json.encodeToString(RedeemReq.serializer(), RedeemReq(code))),
        )

    /** BRILLIANT only — mint an invite. Returns the shareable link. */
    fun createInvite(deviceToken: String, name: String?, level: String): InviteResult =
        json.decodeFromString(
            InviteResult.serializer(),
            postJson("/api/v1/invites", json.encodeToString(InviteReq.serializer(), InviteReq(name, level)), bearer = deviceToken),
        )

    fun fetchConfig(deviceToken: String): ConfigBlob =
        json.decodeFromString(
            ConfigBlob.serializer(),
            postJson("/api/v1/config", "{}", bearer = deviceToken),
        )

    private fun postJson(path: String, body: String, bearer: String? = null): String {
        val conn = URL(InVpnConfig.API_BASE + path).openConnection() as HttpURLConnection
        try {
            if (conn is HttpsURLConnection && InVpnConfig.PINS.isNotEmpty()) {
                conn.sslSocketFactory = pinnedSocketFactory()
            }
            conn.connectTimeout = 15_000
            conn.readTimeout = 20_000
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            bearer?.let { conn.setRequestProperty("Authorization", "Bearer $it") }
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                val msg = runCatching {
                    json.parseToJsonElement(text).jsonObject["error"]?.jsonPrimitive?.content
                }.getOrNull()
                throw ApiException(code, msg ?: "HTTP $code")
            }
            return text
        } finally {
            conn.disconnect()
        }
    }

    /** System CA validation first, then require a SubjectPublicKeyInfo pin match. */
    private fun pinnedSocketFactory(): SSLSocketFactory {
        val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        tmf.init(null as KeyStore?)
        val system = tmf.trustManagers.first { it is X509TrustManager } as X509TrustManager
        val pinning = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) =
                system.checkClientTrusted(chain, authType)

            override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {
                system.checkServerTrusted(chain, authType)
                val matched = chain.any { cert ->
                    val spki = MessageDigest.getInstance("SHA-256").digest(cert.publicKey.encoded)
                    Base64.encodeToString(spki, Base64.NO_WRAP) in InVpnConfig.PINS
                }
                if (!matched) throw CertificateException("InVPN: server key not pinned")
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> = system.acceptedIssuers
        }
        return SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(pinning), null)
        }.socketFactory
    }
}
