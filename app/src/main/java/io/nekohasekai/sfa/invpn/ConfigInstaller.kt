package io.nekohasekai.sfa.invpn

import io.nekohasekai.libbox.Libbox
import io.nekohasekai.sfa.Application
import io.nekohasekai.sfa.database.Profile
import io.nekohasekai.sfa.database.ProfileManager
import io.nekohasekai.sfa.database.Settings
import io.nekohasekai.sfa.database.TypedProfile
import java.io.File
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Fetches the authenticated config, decrypts it in memory, validates it with libbox,
 * and installs it as the single managed local profile (creating or updating it), then
 * selects it and reloads the running service. Replaces the URL/file import flow.
 *
 * Note (hardening TODO): the validated config is written to the profile file because
 * libbox reads the config from a path at service start. At-rest encryption of that
 * file (decrypt-to-tmpfs only at connect) is a follow-up; for now it matches how every
 * SFA profile already stores its config.
 */
object ConfigInstaller {

    /** Pull a fresh config from the backend and install it. Requires prior auth. */
    suspend fun refreshAndInstall() = withContext(Dispatchers.IO) {
        val creds = SecureStore.load(Application.application)
            ?: throw IllegalStateException("not authenticated")
        val blob = ApiClient.fetchConfig(creds.deviceToken)
        val content = String(
            Crypto.decryptWithBodyKey(Crypto.b64UrlDecode(creds.bodyKeyB64), blob.nonce, blob.ct),
            Charsets.UTF_8,
        )
        Libbox.checkConfig(content)
        install(content)
    }

    private suspend fun install(content: String) {
        val ctx = Application.application
        val existing = ProfileManager.list()
            .firstOrNull { it.name == InVpnConfig.MANAGED_PROFILE_NAME }
        if (existing != null) {
            val file = File(existing.typed.path)
            if (!file.exists() || file.readText() != content) file.writeText(content)
            existing.typed.lastUpdated = Date()
            ProfileManager.update(existing)
            Settings.selectedProfile = existing.id
            runCatching { Libbox.newStandaloneCommandClient().serviceReload() }
        } else {
            val fileID = ProfileManager.nextFileID()
            val dir = File(ctx.filesDir, "configs").also { it.mkdirs() }
            val file = File(dir, "$fileID.json").apply { writeText(content) }
            val typed = TypedProfile().apply {
                type = TypedProfile.Type.Local
                path = file.path
                lastUpdated = Date()
            }
            val profile = Profile(name = InVpnConfig.MANAGED_PROFILE_NAME, typed = typed).apply {
                userOrder = ProfileManager.nextOrder()
            }
            ProfileManager.create(profile, andSelect = true)
        }
    }
}
