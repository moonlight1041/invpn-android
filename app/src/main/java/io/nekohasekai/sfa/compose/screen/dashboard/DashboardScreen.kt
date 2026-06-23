package io.nekohasekai.sfa.compose.screen.dashboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PowerSettingsNew
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.nekohasekai.sfa.compose.navigation.NewProfileArgs
import io.nekohasekai.sfa.compose.screen.auth.CreateInviteDialog
import io.nekohasekai.sfa.compose.theme.JetBrainsMono
import io.nekohasekai.sfa.constant.Status
import io.nekohasekai.sfa.invpn.AuthRepository

data class CardRenderItem(val cards: List<CardGroup>, val isRow: Boolean)

/**
 * InVPN home — minimal: a server selector, one tap-to-connect disc inside a progress ring,
 * a status line, and live down/up speeds. All connection wiring is unchanged.
 */
@Composable
fun DashboardScreen(
    serviceStatus: Status = Status.Stopped,
    showStartFab: Boolean = false,
    showStatusBar: Boolean = false,
    onOpenNewProfile: (NewProfileArgs) -> Unit = {},
    onOpenServers: () -> Unit = {},
    viewModel: DashboardViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(serviceStatus) { viewModel.updateServiceStatus(serviceStatus) }

    val connected = serviceStatus == Status.Started
    val transitioning = serviceStatus == Status.Starting || serviceStatus == Status.Stopping
    val hasProfile = uiState.selectedProfileId != -1L
    var showCreateInvite by remember { mutableStateOf(false) }
    val isBrilliant = remember { AuthRepository.isBrilliant() }

    val serverLabel = uiState.selectedProfileName ?: "Серверы"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Server selector (tap to choose the exit)
        Row(
            modifier = Modifier
                .align(Alignment.Start)
                .padding(top = 14.dp, bottom = 6.dp)
                .clip(CircleShape)
                .clickable { onOpenServers() }
                .padding(vertical = 6.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                serverLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(Modifier.weight(1f))

        ConnectDisc(
            connected = connected,
            transitioning = transitioning,
            enabled = hasProfile || connected || transitioning,
            onClick = { viewModel.toggleService() },
        )

        Spacer(Modifier.height(30.dp))
        StatusLine(serviceStatus, serverLabel)

        Spacer(Modifier.weight(1f))

        SpeedBar(
            down = uiState.downlink,
            up = uiState.uplink,
            active = connected,
        )

        if (isBrilliant) {
            TextButton(onClick = { showCreateInvite = true }, modifier = Modifier.padding(top = 6.dp)) {
                Text(
                    "Создать приглашение",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
        Spacer(Modifier.height(20.dp))
    }

    if (showCreateInvite) {
        CreateInviteDialog(onDismiss = { showCreateInvite = false })
    }
}

@Composable
private fun ConnectDisc(connected: Boolean, transitioning: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val track = MaterialTheme.colorScheme.outlineVariant
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val ink = MaterialTheme.colorScheme.onBackground

    val inf = rememberInfiniteTransition(label = "connect")
    val sweep by inf.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart),
        label = "sweep",
    )
    val pulseScale by inf.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.28f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "pulseScale",
    )
    val pulseAlpha by inf.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "pulseAlpha",
    )

    Box(modifier = Modifier.size(210.dp), contentAlignment = Alignment.Center) {
        if (connected) {
            Box(
                modifier = Modifier
                    .size(184.dp)
                    .scale(pulseScale)
                    .alpha(pulseAlpha)
                    .clip(CircleShape)
                    .border(1.dp, accent, CircleShape),
            )
        }

        Canvas(modifier = Modifier.size(200.dp)) {
            val sw = 1.5.dp.toPx()
            val d = size.minDimension - sw
            val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
            drawCircle(color = track, radius = d / 2f, style = Stroke(sw))
            when {
                connected -> drawCircle(color = accent, radius = d / 2f, style = Stroke(sw))
                transitioning -> drawArc(
                    color = accent,
                    startAngle = sweep,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(d, d),
                    style = Stroke(width = sw, cap = StrokeCap.Round),
                )
            }
        }

        Box(
            modifier = Modifier
                .size(158.dp)
                .clip(CircleShape)
                .background(if (connected) accent else Color.Transparent)
                .border(1.dp, if (connected) accent else MaterialTheme.colorScheme.outline, CircleShape)
                .clickable(enabled = enabled && !transitioning) { onClick() },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.PowerSettingsNew,
                    contentDescription = null,
                    tint = if (connected) onAccent else ink,
                    modifier = Modifier.size(26.dp),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = when {
                        connected -> "Подключено"
                        transitioning -> "Связь…"
                        else -> "Подключить"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (connected) onAccent else ink,
                )
            }
        }
    }
}

@Composable
private fun StatusLine(status: Status, serverLabel: String) {
    val accent = MaterialTheme.colorScheme.primary
    val sub = MaterialTheme.colorScheme.onSurfaceVariant
    val amber = Color(0xFFE0A020)
    val connected = status == Status.Started
    val dotColor = when (status) {
        Status.Started -> accent
        Status.Starting, Status.Stopping -> amber
        else -> sub
    }
    val text = when (status) {
        Status.Started -> "Защищено · $serverLabel"
        Status.Starting -> "Подключение"
        Status.Stopping -> "Отключение"
        else -> "Не защищено"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(dotColor))
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (connected) MaterialTheme.colorScheme.onBackground else sub,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun SpeedBar(down: String, up: String, active: Boolean) {
    val sub = MaterialTheme.colorScheme.onSurfaceVariant
    val a = if (active) 1f else 0.3f
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SpeedCell("↓", down, sub, a)
        Box(
            Modifier
                .padding(horizontal = 28.dp)
                .height(30.dp)
                .width(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
        SpeedCell("↑", up, sub, a)
    }
}

@Composable
private fun SpeedCell(arrow: String, value: String, sub: Color, alpha: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(alpha)) {
        Text(
            text = value.ifBlank { "0.0" },
            fontFamily = JetBrainsMono,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(2.dp))
        Text("$arrow Mbps", style = MaterialTheme.typography.labelSmall, color = sub)
    }
}

// --- Retained utilities (used by the legacy card renderer elsewhere; do not remove) ---

fun processCardsForRendering(
    cardOrder: List<CardGroup>,
    visibleCards: Set<CardGroup>,
    cardWidths: Map<CardGroup, CardWidth>,
): List<CardRenderItem> {
    val renderItems = mutableListOf<CardRenderItem>()
    val visibleOrderedCards = cardOrder.filter { visibleCards.contains(it) }
    var i = 0
    while (i < visibleOrderedCards.size) {
        val currentCard = visibleOrderedCards[i]
        val currentWidth = cardWidths[currentCard] ?: CardWidth.Full
        if (currentWidth == CardWidth.Half && i + 1 < visibleOrderedCards.size &&
            (cardWidths[visibleOrderedCards[i + 1]] ?: CardWidth.Full) == CardWidth.Half
        ) {
            renderItems.add(CardRenderItem(listOf(currentCard, visibleOrderedCards[i + 1]), true))
            i += 2
            continue
        }
        renderItems.add(CardRenderItem(listOf(currentCard), false))
        i++
    }
    return renderItems
}

fun isCardAvailableWhenServiceRunning(cardGroup: CardGroup, uiState: DashboardUiState): Boolean = when (cardGroup) {
    CardGroup.ClashMode -> uiState.clashModeVisible
    CardGroup.UploadTraffic -> uiState.trafficVisible
    CardGroup.DownloadTraffic -> uiState.trafficVisible
    CardGroup.Debug -> true
    CardGroup.Connections -> uiState.trafficVisible
    CardGroup.SystemProxy -> uiState.systemProxyVisible
    CardGroup.Profiles -> true
}
