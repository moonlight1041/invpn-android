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
    const val API_BASE = "https://ofjnb.net" // MVP backend domain (existing infra); → clean LUX host later

    // SPKI SHA-256 pins (standard base64) for ofjnb.net. ApiClient enforces an ANY-match against
    // the server-presented chain AFTER normal system-CA validation. We pin the leaf (rotates ~90d),
    // the Let's Encrypt issuing intermediate (stable across leaf renewals → the primary pin), and the
    // cross-signed ISRG root present in the served chain (backstop so an intermediate rotation can't
    // brick the app). Refresh if Let's Encrypt rotates intermediates (announced years in advance).
    val PINS: Set<String> = setOf(
        "zRFPPXTDpTUwB+MiZbHezW/i1jzpgyO9t2Gw+zkeaVQ=", // leaf  CN=ofjnb.net
        "nWN7PSep5XDQdge5zK24CnCRXHr3KvzhKEGxsdqCX9E=", // intermediate (issuer) — renewal-safe
        "fk6IOKit1ild5647BH06ujSIq5XbCgqlbYl6ANhhi88=", // cross-signed root in chain — backstop
    )

    /** Single managed profile name created from the fetched config. */
    const val MANAGED_PROFILE_NAME = "InVPN"
}
