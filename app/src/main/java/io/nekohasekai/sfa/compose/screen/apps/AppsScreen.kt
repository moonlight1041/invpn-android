package io.nekohasekai.sfa.compose.screen.apps

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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

private data class AppRow(val pkg: String, val label: String, val icon: ImageBitmap?)

private val Marble = Color(0xFFF7F3EA)
private val Ink = Color(0xFF23201A)
private val InkSoft = Color(0xFF6E685C)
private val Bronze = Color(0xFFB68A44)

/**
 * Per-app split tunneling. A switch per app: ON = traffic goes through the VPN, OFF = direct.
 * Russian apps default to OFF (direct) so local banking/gov keep working without the foreign exit.
 */
@Composable
fun AppsScreen(serviceStatus: Status = Status.Stopped) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var apps by remember { mutableStateOf<List<AppRow>?>(null) }
    var excluded by remember { mutableStateOf(SplitTunnel.excluded(context)) }
    var applying by remember { mutableStateOf(false) }
    var dirty by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { loadApps(context) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFF7F3EA), Color(0xFFEFE7D6))))
            .padding(horizontal = 18.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            "VPN идёт только через включённые приложения. Российские приложения по умолчанию идут напрямую, мимо VPN.",
            style = MaterialTheme.typography.bodyMedium,
            color = InkSoft,
        )
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                applying = true
                scope.launch {
                    runCatching { ConfigInstaller.reapplyExcludes() }
                    applying = false
                    dirty = false
                }
            },
            enabled = dirty && !applying,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (applying) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Marble)
            } else {
                Text(if (serviceStatus == Status.Started) "Применить и обновить" else "Применить")
            }
        }
        Spacer(Modifier.height(12.dp))

        val list = apps
        if (list == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Bronze)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(list) { app ->
                    val throughVpn = !excluded.contains(app.pkg)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (app.icon != null) {
                            Image(
                                bitmap = app.icon,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(9.dp)),
                            )
                        } else {
                            Box(Modifier.size(40.dp).clip(RoundedCornerShape(9.dp)).background(Color(0x22B68A44)))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(app.label, style = MaterialTheme.typography.bodyLarge, color = Ink, fontWeight = FontWeight.Medium, maxLines = 1)
                            Text(
                                if (throughVpn) "через VPN" else "напрямую",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (throughVpn) Bronze else InkSoft,
                            )
                        }
                        Switch(
                            checked = throughVpn,
                            onCheckedChange = { on ->
                                SplitTunnel.setExcluded(context, app.pkg, bypass = !on)
                                excluded = SplitTunnel.excluded(context)
                                dirty = true
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun loadApps(context: android.content.Context): List<AppRow> {
    val pm = context.packageManager
    return pm.getInstalledApplications(0)
        .filter { pm.getLaunchIntentForPackage(it.packageName) != null && it.packageName != context.packageName }
        .map { ai ->
            AppRow(
                pkg = ai.packageName,
                label = pm.getApplicationLabel(ai).toString(),
                icon = runCatching { pm.getApplicationIcon(ai).toBitmap(96, 96).asImageBitmap() }.getOrNull(),
            )
        }
        .sortedBy { it.label.lowercase() }
}
