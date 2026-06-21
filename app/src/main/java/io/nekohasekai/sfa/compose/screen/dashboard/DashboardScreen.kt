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

/**
 * InVPN home — glassmorphic, status-centric screen.
 * One Connect orb, a classical status line, and expandable metrics for the curious.
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFF6F3EC), Color(0xFFEAE3D5)))),
    ) {
        // Soft "glass" orbs behind the content (Aegean blue + antique gold haze)
        Box(
            modifier = Modifier
                .size(380.dp)
                .align(Alignment.TopEnd)
                .background(Brush.radialGradient(listOf(Color(0x3013507A), Color(0x0013507A))), CircleShape),
        )
        Box(
            modifier = Modifier
                .size(340.dp)
                .align(Alignment.BottomStart)
                .background(Brush.radialGradient(listOf(Color(0x2EB68A44), Color(0x00B68A44))), CircleShape),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "INVPN",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "· IV ·",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary,
            )

            Spacer(Modifier.height(44.dp))

            ConnectOrb(
                connected = connected,
                transitioning = transitioning,
                enabled = hasProfile || connected || transitioning,
                onClick = { viewModel.toggleService() },
            )

            Spacer(Modifier.height(28.dp))

            Text(
                text = when (serviceStatus) {
                    Status.Started -> "Защищено"
                    Status.Starting -> "Подключение…"
                    Status.Stopping -> "Отключение…"
                    else -> "Не защищено"
                },
                style = MaterialTheme.typography.headlineSmall,
                color = if (connected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = uiState.selectedProfileName ?: "Профиль не выбран",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(30.dp))

            if (connected) {
                GlassPanel(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.fillMaxWidth().padding(18.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { metricsExpanded = !metricsExpanded },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            MetricInline("↓", uiState.downlink)
                            MetricInline("↑", uiState.uplink)
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.rotate(if (metricsExpanded) 180f else 0f),
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
            } else if (!hasProfile) {
                Text(
                    text = "Добавьте профиль, чтобы подключиться",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable { onOpenNewProfile(NewProfileArgs()) },
                )
            }

            if (isBrilliant) {
                Spacer(Modifier.height(18.dp))
                TextButton(onClick = { showCreateInvite = true }) {
                    Text("Создать приглашение", color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }

    if (showCreateInvite) {
        CreateInviteDialog(onDismiss = { showCreateInvite = false })
    }
}

@Composable
private fun ConnectOrb(connected: Boolean, transitioning: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .size(184.dp)
            .clip(CircleShape)
            .background(
                if (connected) {
                    Brush.verticalGradient(listOf(Color(0xFF2E73A4), Color(0xFF12476C)))
                } else {
                    Brush.verticalGradient(listOf(Color(0xCCFFFFFF), Color(0x8FFFFFFF)))
                },
            )
            .border(1.5.dp, if (connected) Color(0x55FFFFFF) else Color(0x70FFFFFF), CircleShape)
            .clickable(enabled = enabled && !transitioning) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        if (transitioning) {
            CircularProgressIndicator(
                color = if (connected) Color.White else primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(52.dp),
            )
        } else {
            Icon(
                imageVector = if (connected) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = null,
                tint = if (connected) Color.White else primary,
                modifier = Modifier.size(68.dp),
            )
        }
    }
}

@Composable
private fun GlassPanel(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xB8FFFFFF))
            .border(1.dp, Color(0x70FFFFFF), RoundedCornerShape(24.dp)),
    ) { content() }
}

@Composable
private fun MetricInline(arrow: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(arrow, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
        Spacer(Modifier.width(6.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
        )
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
