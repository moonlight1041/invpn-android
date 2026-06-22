package io.nekohasekai.sfa.compose.screen.dashboard

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.nekohasekai.sfa.compose.navigation.NewProfileArgs
import io.nekohasekai.sfa.compose.screen.auth.CreateInviteDialog
import io.nekohasekai.sfa.constant.Status
import io.nekohasekai.sfa.invpn.AuthRepository

data class CardRenderItem(val cards: List<CardGroup>, val isRow: Boolean)

// Palette (marble temple): readable ink/sea type, bronze reserved as the accent.
private val Marble = Color(0xFFFBF9F4)
private val Ink = Color(0xFF23201A)
private val InkSoft = Color(0xFF6E685C)
private val SeaDark = Color(0xFF0E3A58)
private val Sea = Color(0xFF13507A)
private val Bronze = Color(0xFFB68A44)
private val Protected = Color(0xFF2F7D55)

/**
 * InVPN home — a marble panel with a bronze "aegis" Connect disc and a laurel tier badge.
 * One tap protects; the access tier and live status are always visible.
 */
@Composable
fun DashboardScreen(
    serviceStatus: Status = Status.Stopped,
    showStartFab: Boolean = false,
    showStatusBar: Boolean = false,
    onOpenNewProfile: (NewProfileArgs) -> Unit = {},
    viewModel: DashboardViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    LaunchedEffect(serviceStatus) { viewModel.updateServiceStatus(serviceStatus) }

    val connected = serviceStatus == Status.Started
    val transitioning = serviceStatus == Status.Starting || serviceStatus == Status.Stopping
    val hasProfile = uiState.selectedProfileId != -1L
    var metricsExpanded by remember { mutableStateOf(false) }
    var showCreateInvite by remember { mutableStateOf(false) }
    val isBrilliant = remember { AuthRepository.isBrilliant() }
    val level = remember { AuthRepository.level() }
    val displayName = remember { AuthRepository.displayName() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFF7F3EA), Color(0xFFEAE1CF)))),
    ) {
        // Soft marble haze (Aegean + bronze) behind the content
        Box(
            Modifier.size(360.dp).align(Alignment.TopEnd)
                .background(Brush.radialGradient(listOf(Color(0x2213507A), Color(0x0013507A))), CircleShape),
        )
        Box(
            Modifier.size(320.dp).align(Alignment.BottomStart)
                .background(Brush.radialGradient(listOf(Color(0x24B68A44), Color(0x00B68A44))), CircleShape),
        )

        TierBadge(level, displayName, Modifier.align(Alignment.TopCenter).padding(top = 20.dp))

        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("INVPN", style = MaterialTheme.typography.displayMedium, color = SeaDark, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("—  ❖  —", style = MaterialTheme.typography.titleMedium, color = Bronze)

            Spacer(Modifier.height(40.dp))
            ConnectAegis(
                connected = connected,
                transitioning = transitioning,
                enabled = hasProfile || connected || transitioning,
                onClick = { viewModel.toggleService() },
            )
            Spacer(Modifier.height(30.dp))

            Text(
                text = when (serviceStatus) {
                    Status.Started -> "Защищено"
                    Status.Starting -> "Подключение…"
                    Status.Stopping -> "Отключение…"
                    else -> "Не защищено"
                },
                style = MaterialTheme.typography.headlineSmall,
                color = if (connected) Protected else InkSoft,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (hasProfile) (uiState.selectedProfileName ?: "Сеть INVPN") else "Нажмите, чтобы подключиться",
                style = MaterialTheme.typography.bodyMedium,
                color = InkSoft,
                textAlign = TextAlign.Center,
            )

            if (connected) {
                Spacer(Modifier.height(28.dp))
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { metricsExpanded = !metricsExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MetricInline("↓", uiState.downlink)
                            MetricInline("↑", uiState.uplink)
                            Icon(
                                Icons.Default.KeyboardArrowDown, null,
                                tint = InkSoft, modifier = Modifier.rotate(if (metricsExpanded) 180f else 0f),
                            )
                        }
                        AnimatedVisibility(visible = metricsExpanded) {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
                                MetricRow("Соединения", "${uiState.connectionsIn} вх · ${uiState.connectionsOut} исх")
                                MetricRow("Отправлено", uiState.uplinkTotal)
                                MetricRow("Получено", uiState.downlinkTotal)
                                if (uiState.memory.isNotEmpty()) MetricRow("Память", uiState.memory)
                            }
                        }
                    }
                }
            }

            if (isBrilliant) {
                Spacer(Modifier.height(22.dp))
                TextButton(onClick = { showCreateInvite = true }) {
                    Text("Создать приглашение", color = Bronze, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showCreateInvite) {
        CreateInviteDialog(onDismiss = { showCreateInvite = false })
    }
}

@Composable
private fun TierBadge(level: String, name: String?, modifier: Modifier = Modifier) {
    val color = when (level) {
        "BRILLIANT" -> Color(0xFF2E7DA6)
        "SILVER" -> Color(0xFF8A8F98)
        else -> Bronze
    }
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xCCFBF9F4))
            .border(1.5.dp, color, RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("❖", color = color, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (!name.isNullOrBlank()) "$name · $level" else level,
            color = Ink,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ConnectAegis(connected: Boolean, transitioning: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(200.dp)
            .clip(CircleShape)
            .background(Marble)
            .border(6.dp, Bronze, CircleShape) // bronze aegis ring
            .clickable(enabled = enabled && !transitioning) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(148.dp)
                .clip(CircleShape)
                .background(
                    if (connected) {
                        Brush.verticalGradient(listOf(Color(0xFF1C6FA0), SeaDark))
                    } else {
                        Brush.verticalGradient(listOf(Color(0xFFF1EADB), Color(0xFFE6DBC4)))
                    },
                )
                .border(2.dp, if (connected) Color(0x55FFFFFF) else Color(0x66B68A44), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (transitioning) {
                CircularProgressIndicator(
                    color = if (connected) Marble else Sea, strokeWidth = 3.dp, modifier = Modifier.size(48.dp),
                )
            } else {
                Icon(
                    imageVector = if (connected) Icons.Default.Lock else Icons.Default.LockOpen,
                    contentDescription = null,
                    tint = if (connected) Marble else Sea,
                    modifier = Modifier.size(62.dp),
                )
            }
        }
    }
}

@Composable
private fun GlassPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xCCFBF9F4))
            .border(1.dp, Color(0x66B68A44), RoundedCornerShape(24.dp)),
    ) { content() }
}

@Composable
private fun MetricInline(arrow: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(arrow, style = MaterialTheme.typography.titleMedium, color = Sea)
        Spacer(Modifier.width(6.dp))
        Text(value, style = MaterialTheme.typography.titleMedium, color = Ink, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = InkSoft)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = Ink, fontWeight = FontWeight.Medium)
    }
}

// --- Retained utility (used by the legacy card renderer, kept for compatibility) ---

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
