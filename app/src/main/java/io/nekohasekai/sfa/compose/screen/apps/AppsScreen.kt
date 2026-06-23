package io.nekohasekai.sfa.compose.screen.apps

import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import io.nekohasekai.sfa.constant.Status
import io.nekohasekai.sfa.invpn.ConfigInstaller
import io.nekohasekai.sfa.invpn.SplitTunnel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class AppRow(val pkg: String, val label: String, val appInfo: ApplicationInfo)

/**
 * Per-app split tunneling. A switch per app: ON = traffic goes through the VPN, OFF = direct.
 * Russian apps default to OFF (direct) so local banking/gov keep working without the foreign exit.
 *
 * The list is built from the launcher query (only user-facing apps — a small Binder payload, so no
 * TransactionTooLargeException from enumerating every package), and each icon is decoded lazily off
 * the main thread, only for the rows the LazyColumn actually shows. That fixes the real cause of the
 * recurring "вкладка Приложения не работает": the old loader enumerated all apps and eagerly decoded
 * hundreds of icons up front → TTLE / OOM. mvp7 only stopped the crash; the list still never loaded.
 */
@Composable
fun AppsScreen(serviceStatus: Status = Status.Stopped) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var apps by remember { mutableStateOf<List<AppRow>?>(null) }
    var excluded by remember { mutableStateOf(SplitTunnel.excluded(context)) }
    var applying by remember { mutableStateOf(false) }
    var dirty by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }

    val ink = MaterialTheme.colorScheme.onBackground
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val hairline = MaterialTheme.colorScheme.outline
    val faint = MaterialTheme.colorScheme.outlineVariant
    val accent = MaterialTheme.colorScheme.primary

    LaunchedEffect(Unit) {
        runCatching { withContext(Dispatchers.IO) { loadApps(context) } }
            .onSuccess { apps = it }
            .onFailure { loadError = it.message ?: it.toString(); apps = emptyList() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            "Приложения",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = ink,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "Включённые идут через VPN, остальные — напрямую.",
            style = MaterialTheme.typography.bodyMedium,
            color = muted,
        )
        Spacer(Modifier.height(16.dp))

        TextButton(
            onClick = {
                applying = true
                scope.launch {
                    runCatching { ConfigInstaller.reapplyExcludes() }
                    applying = false
                    dirty = false
                }
            },
            enabled = dirty && !applying,
            modifier = Modifier.align(Alignment.Start),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 4.dp),
        ) {
            if (applying) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = accent)
            } else {
                Text(
                    if (serviceStatus == Status.Started) "Применить и обновить" else "Применить",
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        val list = apps
        if (loadError != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Не удалось загрузить список приложений: $loadError",
                    style = MaterialTheme.typography.bodyMedium,
                    color = muted,
                )
            }
        } else if (list == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = accent)
            }
        } else if (list.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Приложения не найдены",
                    style = MaterialTheme.typography.bodyMedium,
                    color = muted,
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(list, key = { it.pkg }) { app ->
                    val throughVpn = !excluded.contains(app.pkg)
                    // Decode this row's icon off-main, only when the row is shown; runCatching keeps a
                    // single bad icon from crashing the list.
                    val icon by produceState<ImageBitmap?>(initialValue = null, app.pkg) {
                        value = withContext(Dispatchers.IO) {
                            runCatching {
                                app.appInfo.loadIcon(context.packageManager).toBitmap(96, 96).asImageBitmap()
                            }.getOrNull()
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val bmp = icon
                        if (bmp != null) {
                            Image(
                                bitmap = bmp,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(7.dp)),
                            )
                        } else {
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(7.dp))
                                    .background(faint),
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(
                            app.label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = ink,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (throughVpn) "VPN" else "Напрямую",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (throughVpn) ink else muted,
                        )
                        Spacer(Modifier.width(12.dp))
                        Switch(
                            checked = throughVpn,
                            onCheckedChange = { on ->
                                SplitTunnel.setExcluded(context, app.pkg, bypass = !on)
                                excluded = SplitTunnel.excluded(context)
                                dirty = true
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = accent,
                                checkedBorderColor = accent,
                                uncheckedThumbColor = muted,
                                uncheckedTrackColor = MaterialTheme.colorScheme.background,
                                uncheckedBorderColor = hairline,
                            ),
                        )
                    }
                    HorizontalDivider(thickness = 1.dp, color = hairline)
                }
            }
        }
    }
}

/**
 * Launchable, user-facing apps only (excludes our own package and system services). Built from the
 * launcher intent query — a far smaller result than enumerating every installed package — and labels
 * are resolved here (cheap) for sorting; icons are left to the row to decode lazily.
 */
private fun loadApps(context: android.content.Context): List<AppRow> {
    val pm = context.packageManager
    val resolved = pm.queryIntentActivities(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),
        0,
    )
    val seen = HashSet<String>()
    return resolved.mapNotNull { ri ->
        val ai = ri.activityInfo?.applicationInfo ?: return@mapNotNull null
        if (ai.packageName == context.packageName || !seen.add(ai.packageName)) return@mapNotNull null
        AppRow(ai.packageName, ai.loadLabel(pm).toString(), ai)
    }.sortedBy { it.label.lowercase() }
}
