package io.nekohasekai.sfa.invpn

import io.nekohasekai.sfa.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

/**
 * Register / login against the config-API and persist credentials. The body_key is
 * derived locally from the password (which is never stored) and kept for later
 * config decryption. Network calls run on IO.
 */
object AuthRepository {

    private val app get() = Application.application

    private val _authState by lazy { MutableStateFlow(SecureStore.isLoggedIn(app)) }

    /** Observable auth state: AuthGate shows the app when true, the login screen when false. */
    val authState: StateFlow<Boolean> get() = _authState

    /** Re-read persisted creds into [authState] — call after a successful login/redeem. */
    fun refreshAuthState() { _authState.value = isAuthenticated() }

    fun isAuthenticated(): Boolean = SecureStore.isLoggedIn(app)
    fun username(): String? = SecureStore.load(app)?.username
    fun displayName(): String? = SecureStore.load(app)?.displayName
    fun level(): String = SecureStore.load(app)?.level ?: "GOLDEN"
    fun isBrilliant(): Boolean = level() == "BRILLIANT"

    /** login+password account: body_key is derived from the password (never stored). */
    suspend fun login(username: String, password: String) = withContext(Dispatchers.IO) {
        val r = ApiClient.login(username, password)
        val bodyKey = Crypto.deriveBodyKey(password, Crypto.b64UrlDecode(r.salt_body))
        SecureStore.save(
            app,
            Credentials(username, r.device_token, r.salt_body, Crypto.b64UrlEncode(bodyKey), r.level, r.name),
        )
    }

    /** passwordless invite-link redemption: body_key arrives from the server over TLS. */
    suspend fun redeemInvite(code: String) = withContext(Dispatchers.IO) {
        val r = ApiClient.redeem(code)
        SecureStore.save(
            app,
            Credentials(r.name ?: "(invite)", r.device_token, "", r.body_key, r.level, r.name),
        )
    }

    /** BRILLIANT only — mint an invite link and return it. */
    suspend fun createInvite(name: String?, level: String): String = withContext(Dispatchers.IO) {
        val token = SecureStore.load(app)?.deviceToken ?: error("not authenticated")
        ApiClient.createInvite(token, name, level).link
    }

    fun logout() {
        SecureStore.clear(app)
        _authState.value = false
    }
}
