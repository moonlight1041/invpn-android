package io.nekohasekai.sfa.compose.screen.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.nekohasekai.sfa.R
import io.nekohasekai.sfa.compose.navigation.Screen
import io.nekohasekai.sfa.compose.topbar.OverrideTopBar
import io.nekohasekai.sfa.invpn.AuthRepository
import io.nekohasekai.sfa.update.UpdateState
import io.nekohasekai.sfa.utils.HookModuleUpdateNotifier
import io.nekohasekai.sfa.utils.HookStatusClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    // Minimal screen owns its own in-content title; clear any inherited top bar.
    OverrideTopBar {}

    val context = LocalContext.current
    val hasUpdate by UpdateState.hasUpdate
    val hookStatus by HookStatusClient.status.collectAsState()
    val hasPendingPrivilegeDowngrade = HookModuleUpdateNotifier.isDowngrade(hookStatus)
    val hasPendingPrivilegeUpdate = HookModuleUpdateNotifier.isUpgrade(hookStatus)
    LaunchedEffect(Unit) {
        HookStatusClient.refresh()
    }
    var showLogoutConfirm by remember { mutableStateOf(false) }

    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier =
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(R.string.title_settings),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = ink,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 12.dp),
        )

        // Account header — flat, separated by hairlines.
        AccountHeader()

        // General settings.
        SettingRow(
            title = stringResource(R.string.title_app_settings),
            onClick = { navController.navigate("settings/app") },
            trailing = { Trailing(showBadge = hasUpdate) },
        )
        Hairline()
        SettingRow(
            title = stringResource(R.string.core),
            onClick = { navController.navigate("settings/core") },
        )
        Hairline()
        SettingRow(
            title = stringResource(R.string.service),
            onClick = { navController.navigate("settings/service") },
        )
        Hairline()
        SettingRow(
            title = stringResource(R.string.profile_override),
            onClick = { navController.navigate("settings/profile_override") },
        )
        Hairline()
        SettingRow(
            title = stringResource(R.string.privilege_settings),
            onClick = { navController.navigate("settings/privilege") },
            trailing = {
                Trailing(
                    showBadge = hasPendingPrivilegeDowngrade || hasPendingPrivilegeUpdate,
                    badgeColor = if (hasPendingPrivilegeDowngrade) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
            },
        )

        // Technical screens (sing-box Log / Connections / Tools) — relinked here after being
        // dropped from the bottom bar, so they stay reachable without the sing-box look.
        SectionLabel("Технические")
        SettingRow(
            title = "Журнал",
            onClick = { navController.navigate(Screen.Log.route) },
        )
        Hairline()
        SettingRow(
            title = "Соединения",
            onClick = { navController.navigate(Screen.Connections.route) },
        )
        Hairline()
        SettingRow(
            title = "Инструменты",
            onClick = { navController.navigate(Screen.Tools.route) },
        )

        // About.
        SectionLabel(stringResource(R.string.about))
        SettingRow(
            title = stringResource(R.string.error_deprecated_documentation),
            onClick = {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                intent.data = android.net.Uri.parse("https://sing-box.sagernet.org/")
                context.startActivity(intent)
            },
            trailing = { ExternalLinkIcon() },
        )
        Hairline()
        SettingRow(
            title = stringResource(R.string.source_code),
            onClick = {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                intent.data =
                    android.net.Uri.parse("https://github.com/SagerNet/sing-box-for-android")
                context.startActivity(intent)
            },
            trailing = { ExternalLinkIcon() },
        )
        Hairline()
        SettingRow(
            title = stringResource(R.string.sponsor),
            onClick = {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                intent.data = android.net.Uri.parse("https://sekai.icu/sponsors/")
                context.startActivity(intent)
            },
            trailing = { ExternalLinkIcon() },
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Logout — muted text action at the bottom.
        TextButton(
            onClick = { showLogoutConfirm = true },
            modifier = Modifier.padding(start = 12.dp, bottom = 8.dp),
        ) {
            Text(
                text = "Выйти",
                style = MaterialTheme.typography.labelLarge,
                color = muted,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            title = { Text("Выйти из аккаунта?") },
            text = {
                Text(
                    "Доступ к VPN на этом устройстве будет удалён. Если вы входили по ссылке-приглашению " +
                        "(без пароля), повторный вход возможен только по новой ссылке — сохраните её заранее.",
                )
            },
            confirmButton = {
                TextButton(onClick = { showLogoutConfirm = false; AuthRepository.logout() }) {
                    Text("Выйти", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) { Text("Отмена") }
            },
        )
    }
}

@Composable
private fun AccountHeader() {
    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary

    val level = AuthRepository.level()
    val account = AuthRepository.displayName() ?: AuthRepository.username() ?: "—"
    val initial = (account.firstOrNull() ?: level.firstOrNull() ?: '•').uppercaseChar().toString()

    Hairline()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(accent),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = onAccent,
            )
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = level,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = ink,
            )
            Text(
                text = account,
                style = MaterialTheme.typography.bodyMedium,
                color = muted,
            )
        }
    }
    Hairline()
}

/** Flat list row: ink title + optional muted subtitle on the left, custom trailing on the right. */
@Composable
private fun SettingRow(
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    trailing: @Composable () -> Unit = { Chevron() },
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        trailing()
    }
}

/** Navigation trailing with an optional accent badge dot. */
@Composable
private fun Trailing(
    showBadge: Boolean,
    badgeColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (showBadge) {
            Box(
                Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(badgeColor),
            )
        }
        Chevron()
    }
}

@Composable
private fun Chevron() {
    NavIcon(Icons.Default.ChevronRight)
}

@Composable
private fun ExternalLinkIcon() {
    NavIcon(Icons.AutoMirrored.Outlined.OpenInNew)
}

@Composable
private fun NavIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(18.dp),
    )
}

@Composable
private fun SectionLabel(text: String) {
    Spacer(Modifier.height(20.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 4.dp),
    )
    Hairline()
}

@Composable
private fun Hairline() {
    Box(
        Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outline),
    )
}
