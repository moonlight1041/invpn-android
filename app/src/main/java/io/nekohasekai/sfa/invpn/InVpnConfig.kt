package io.nekohasekai.sfa.invpn

/**
 * InVPN deployment constants. Filled once the pseudonymous backend exists:
 *  - API_BASE  -> the Njalla domain fronting the LUX config-API (TLS 1.3).
 *  - PINS      -> SPKI SHA-256 pins (standard base64) of the server leaf/intermediate.
 *
 * Until then API_BASE points nowhere (the app builds and the auth UI shows, but a real
 * register/login needs the backend up). PINS empty == rely on system CA validation only;
 * as soon as the cert is known, add the pin(s) and pinning is enforced automatically.
 */
object InVpnConfig {
    const val API_BASE = "https://api.invpn.invalid" // TODO: real backend domain (Njalla → LUX)

    val PINS: Set<String> = emptySet() // TODO: e.g. "k3a1f...=" (sha256 of SubjectPublicKeyInfo)

    /** Single managed profile name created from the fetched config. */
    const val MANAGED_PROFILE_NAME = "InVPN"
}
