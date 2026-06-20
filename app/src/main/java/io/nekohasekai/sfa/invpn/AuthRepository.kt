package io.nekohasekai.sfa.invpn

import io.nekohasekai.sfa.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Register / login against the config-API and persist credentials. The body_key is
 * derived locally from the password (which is never stored) and kept for later
 * config decryption. Network calls run on IO.
 */
object AuthRepository {

    fun isAuthenticated(): Boolean = SecureStore.isLoggedIn(Application.application)

    fun username(): String? = SecureStore.load(Application.application)?.username

    suspend fun register(inviteCode: String, username: String, password: String) =
        withContext(Dispatchers.IO) {
            persist(username, password, ApiClient.register(inviteCode, username, password))
        }

    suspend fun login(username: String, password: String) =
        withContext(Dispatchers.IO) {
            persist(username, password, ApiClient.login(username, password))
        }

    fun logout() = SecureStore.clear(Application.application)

    private fun persist(username: String, password: String, r: AuthResult) {
        val bodyKey = Crypto.deriveBodyKey(password, Crypto.b64UrlDecode(r.salt_body))
        SecureStore.save(
            Application.application,
            Credentials(
                username = username,
                deviceToken = r.device_token,
                saltBodyB64 = r.salt_body,
                bodyKeyB64 = Crypto.b64UrlEncode(bodyKey),
            ),
        )
    }
}
