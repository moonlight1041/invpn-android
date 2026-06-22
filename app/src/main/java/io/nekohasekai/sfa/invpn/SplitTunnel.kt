package io.nekohasekai.sfa.invpn

import android.content.Context
import android.content.pm.PackageManager
import io.nekohasekai.sfa.Application
import org.json.JSONArray
import org.json.JSONObject

/**
 * Per-app split tunneling. Stores the set of package names that BYPASS the VPN (their traffic
 * goes out directly, not through the exit). Russian apps (banking, gov, local services, big RU
 * platforms) bypass by default so they keep working without the foreign exit / antifraud friction.
 *
 * Applied by writing `exclude_package` onto the config's `tun` inbound — filtered to INSTALLED
 * packages only, because Android's VpnService rejects an unknown package.
 */
object SplitTunnel {
    private const val PREFS = "invpn_apps"
    private const val KEY_EXCLUDED = "excluded"
    private const val KEY_INIT = "initialized"

    /** Default-bypass list: common RU banking / gov / telecom / platforms. */
    val RU_DEFAULT_BYPASS = setOf(
        "ru.sberbankmobile", "ru.sberbank.android", "ru.sberbankmobile_alpha",
        "com.idamob.tinkoff.android", "ru.vtb24.mobilebanking.android", "ru.alfabank.mobile.android",
        "ru.gazprombank.android.mobilebank.app", "ru.raiffeisennews", "ru.psbank.mobile",
        "ru.bank_okb.mobile", "ru.rosbank.android", "com.openbank", "ru.mkb.mobile",
        "ru.gosuslugi.app", "ru.fns.lkfl2", "ru.nalog.app", "ru.gibdd.app",
        "ru.yandex.yandexmaps", "ru.yandex.taxi", "ru.yandex.searchplugin", "ru.yandex.market",
        "ru.yandex.mail", "com.yandex.browser", "ru.yandex.music", "ru.kinopoisk", "ru.yandex.bank",
        "com.vkontakte.android", "ru.ok.android", "ru.mail.mailapp", "com.my.mail",
        "ru.megafon.mlk", "ru.beeline.services", "ru.tele2.mytele2", "ru.mts.mtstv",
        "ru.wildberries.ru", "com.ozon.app.android", "ru.dns_shop.android",
    )

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun ensureInit(ctx: Context) {
        val p = prefs(ctx)
        if (!p.getBoolean(KEY_INIT, false)) {
            p.edit().putStringSet(KEY_EXCLUDED, RU_DEFAULT_BYPASS).putBoolean(KEY_INIT, true).apply()
        }
    }

    /** Packages set to bypass the VPN (as stored; may include not-installed entries). */
    fun excluded(ctx: Context = Application.application): Set<String> {
        ensureInit(ctx)
        return prefs(ctx).getStringSet(KEY_EXCLUDED, emptySet())?.toSet() ?: emptySet()
    }

    fun isExcluded(ctx: Context, pkg: String): Boolean = excluded(ctx).contains(pkg)

    fun setExcluded(ctx: Context, pkg: String, bypass: Boolean) {
        val cur = excluded(ctx).toMutableSet()
        if (bypass) cur.add(pkg) else cur.remove(pkg)
        prefs(ctx).edit().putStringSet(KEY_EXCLUDED, cur).apply()
    }

    /** Excluded packages that are actually installed — safe to hand to the VpnService. */
    fun excludedInstalled(ctx: Context = Application.application): List<String> {
        val pm = ctx.packageManager
        return excluded(ctx).filter { pkg ->
            try {
                pm.getApplicationInfo(pkg, 0); true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    /** Inject `exclude_package` (installed-filtered) onto the config's tun inbound. */
    fun applyExcludes(configJson: String): String {
        return try {
            val root = JSONObject(configJson)
            val inbounds = root.optJSONArray("inbounds") ?: return configJson
            val pkgs = excludedInstalled()
            for (i in 0 until inbounds.length()) {
                val ib = inbounds.optJSONObject(i) ?: continue
                if (ib.optString("type") == "tun") {
                    ib.remove("include_package")
                    if (pkgs.isEmpty()) ib.remove("exclude_package") else ib.put("exclude_package", JSONArray(pkgs))
                }
            }
            root.toString()
        } catch (e: Exception) {
            configJson
        }
    }
}
