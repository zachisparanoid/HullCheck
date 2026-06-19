package com.hullcheck.ui.checklist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hullcheck.R
import com.hullcheck.data.model.ChecklistItem
import com.hullcheck.data.model.ExpiryStatus
import com.hullcheck.data.model.ItemStatus
import com.hullcheck.data.model.Severity
import com.hullcheck.ui.components.HullCheckTopBar
import com.hullcheck.ui.components.HullFilterChip
import com.hullcheck.ui.components.InlineStatusButtons
import com.hullcheck.ui.theme.StatusDefect
import com.hullcheck.ui.theme.StatusNeedsService
import com.hullcheck.ui.theme.severityColor
import com.hullcheck.util.performStatusTick

@Composable
fun ChecklistScreen(
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
    viewModel: ChecklistViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val view = LocalView.current
    var overflowOpen by remember { mutableStateOf(false) }
    var showAddItem by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            HullCheckTopBar(
                title = state.category?.name ?: stringResource(R.string.loading),
                subtitle = stringResource(R.string.short_checked_of_total, state.checked, state.total),
                onBack = onBack,
                actions = {
                    IconButton(onClick = { overflowOpen = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.action_more))
                    }
                    DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.mark_all_na)) },
                            onClick = { overflowOpen = false; viewModel.markAllNa() },
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxWidth()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                listOf(
                    ChecklistFilter.ALL to R.string.filter_all,
                    ChecklistFilter.PENDING to R.string.filter_pending,
                    ChecklistFilter.DEFECTS to R.string.filter_defects,
                    ChecklistFilter.NA to R.string.filter_na,
                ).forEach { (filter, labelRes) ->
                    HullFilterChip(
                        selected = state.filter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(stringResource(labelRes)) },
                    )
                }
            }

            LazyColumn(
                Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(state.items, key = { it.id }) { item ->
                    ChecklistItemRow(
                        item = item,
                        onOpen = { onOpenItem(item.id) },
                        onSetStatus = { status ->
                            view.performStatusTick()
                            viewModel.setStatus(item, status)
                        },
                        onReset = { viewModel.resetItem(item) },
                        onDelete = { viewModel.deleteItem(item) },
                    )
                }
                item {
                    OutlinedButton(
                        onClick = { showAddItem = true },
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    ) {
                        Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.add_item))
                    }
                }
            }
        }
    }

    if (showAddItem) {
        AddItemDialog(
            onConfirm = { title, guidance -> viewModel.addCustomItem(title, guidance); showAddItem = false },
            onDismiss = { showAddItem = false },
        )
    }
}

@Composable
private fun ChecklistItemRow(
    item: ChecklistItem,
    onOpen: () -> Unit,
    onSetStatus: (ItemStatus) -> Unit,
    onReset: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val stripeColor = if (item.isDefect) severityColor(item.severity ?: Severity.MODERATE) else Color.Transparent

    Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(stripeColor),
            )
            Column(Modifier.weight(1f).clickable(onClick = onOpen).padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val expiry = item.expiryStatus()
                    if (expiry == ExpiryStatus.EXPIRED || expiry == ExpiryStatus.SOON) {
                        val ec = if (expiry == ExpiryStatus.EXPIRED) StatusDefect else StatusNeedsService
                        Surface(shape = RoundedCornerShape(6.dp), color = ec.copy(alpha = 0.18f), contentColor = ec) {
                            Text(
                                stringResource(if (expiry == ExpiryStatus.EXPIRED) R.string.expiry_expired else R.string.expiry_soon),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                    if (item.photoCount > 0) {
                        Icon(Icons.Outlined.PhotoLibrary, stringResource(R.string.cd_item_has_photos), Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(item.photoCount.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(8.dp))
                    }
                    if (item.hasNotes) {
                        Icon(Icons.Outlined.Description, stringResource(R.string.cd_item_has_notes), Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Outlined.MoreVert, stringResource(R.string.action_more), Modifier.size(18.dp))
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.reset_to_pending)) },
                                onClick = { menuOpen = false; onReset() },
                            )
                            if (item.isCustom) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                                    onClick = { menuOpen = false; onDelete() },
                                )
                            }
                        }
                    }
                }
                item.guidance?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(12.dp))
                InlineStatusButtons(current = item.status, onSet = onSetStatus, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun AddItemDialog(onConfirm: (String, String?) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var guidance by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_custom_item_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.item_title_hint)) },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = guidance,
                    onValueChange = { guidance = it },
                    label = { Text(stringResource(R.string.label_guidance)) },
                    minLines = 2,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { if (title.isNotBlank()) onConfirm(title, guidance.ifBlank { null }) }, enabled = title.isNotBlank()) {
                Text(stringResource(R.string.action_add))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
