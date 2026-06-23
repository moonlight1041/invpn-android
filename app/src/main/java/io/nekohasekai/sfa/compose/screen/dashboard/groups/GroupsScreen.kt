package io.nekohasekai.sfa.compose.screen.dashboard.groups

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.nekohasekai.sfa.R
import io.nekohasekai.sfa.compose.model.Group
import io.nekohasekai.sfa.compose.model.GroupItem
import io.nekohasekai.sfa.compose.theme.JetBrainsMono
import io.nekohasekai.sfa.constant.Status

@Composable
fun GroupsScreen(
    serviceStatus: Status,
    viewModel: GroupsViewModel = viewModel(),
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onToggleAllGroups: () -> Unit = { viewModel.toggleAllGroups() },
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Stable callbacks to prevent recomposition
    val onToggleExpanded =
        remember<(String) -> Unit> {
            { groupTag -> viewModel.toggleGroupExpand(groupTag) }
        }
    val onItemSelected =
        remember<(String, String) -> Unit> {
            { groupTag, itemTag -> viewModel.selectGroupItem(groupTag, itemTag) }
        }
    val onUrlTest =
        remember<(String) -> Unit> {
            { groupTag -> viewModel.urlTest(groupTag) }
        }

    LaunchedEffect(serviceStatus, viewModel) {
        viewModel.updateServiceStatus(serviceStatus)
    }

    // Show snackbar when needed
    LaunchedEffect(uiState.showCloseConnectionsSnackbar) {
        if (uiState.showCloseConnectionsSnackbar) {
            val message = context.getString(R.string.close_connections_confirm)
            val actionLabel = context.getString(R.string.close)
            val result =
                snackbarHostState.showSnackbar(
                    message = message,
                    actionLabel = actionLabel,
                    duration = androidx.compose.material3.SnackbarDuration.Indefinite,
                    withDismissAction = true,
                )
            when (result) {
                androidx.compose.material3.SnackbarResult.ActionPerformed -> {
                    viewModel.closeConnections()
                }
                androidx.compose.material3.SnackbarResult.Dismissed -> {
                    viewModel.dismissCloseConnectionsSnackbar()
                }
            }
        }
    }

    if (uiState.isLoading) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding =
            PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = 8.dp,
                bottom = 24.dp,
            ),
        ) {
            items(
                items = uiState.groups,
                key = { it.tag },
                contentType = { "GroupCard" },
            ) { group ->
                ProxyGroup(
                    group = group,
                    isExpanded = uiState.expandedGroups.contains(group.tag),
                    onToggleExpanded = remember { { onToggleExpanded(group.tag) } },
                    onItemSelected = remember { { itemTag -> onItemSelected(group.tag, itemTag) } },
                    onUrlTest = remember { { onUrlTest(group.tag) } },
                )
            }
        }
    }
}

@Composable
private fun ProxyGroup(
    group: Group,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    onItemSelected: (String) -> Unit,
    onUrlTest: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Group header — small muted uppercase label, tap to expand/collapse
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpanded)
                .padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.tag.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // Selected outbound, shown when collapsed
                AnimatedVisibility(
                    visible = !isExpanded && group.selected.isNotEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut(),
                ) {
                    Text(
                        text = group.selected,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            // URL test action
            AnimatedVisibility(
                visible = group.selectable,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                IconButton(
                    onClick = onUrlTest,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = stringResource(R.string.url_test),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Expand / collapse chevron
            val rotationAngle by animateFloatAsState(
                targetValue = if (isExpanded) 180f else 0f,
                animationSpec = tween(300),
                label = "ExpandIcon",
            )
            val expandContentDescription = stringResource(R.string.expand)
            val collapseContentDescription = stringResource(R.string.collapse)
            Icon(
                imageVector = Icons.Default.ExpandMore,
                contentDescription = if (isExpanded) collapseContentDescription else expandContentDescription,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(20.dp)
                    .graphicsLayer { rotationZ = rotationAngle },
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Outbound rows
        AnimatedVisibility(
            visible = isExpanded && group.items.isNotEmpty(),
            enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
            exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300)),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                group.items.forEach { item ->
                    ProxyRow(
                        item = item,
                        isSelected = item.tag == group.selected,
                        isSelectable = group.selectable,
                        onClick = remember(item.tag) { { onItemSelected(item.tag) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProxyRow(item: GroupItem, isSelected: Boolean, isSelectable: Boolean, onClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outline,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (isSelectable) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Name
            Text(
                text = item.tag,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            // Latency in monospaced numerals
            AnimatedVisibility(
                visible = item.urlTestTime > 0,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Text(
                    text = "${item.urlTestDelay} ms",
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = JetBrainsMono,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Selection dot
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    ),
            )
        }
    }
}
