package com.hullcheck.ui.defects

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hullcheck.R
import com.hullcheck.data.model.Severity
import com.hullcheck.ui.components.EmptyState
import com.hullcheck.ui.components.HullCheckTopBar
import com.hullcheck.ui.components.PhotoThumbnailRow
import com.hullcheck.ui.components.labelText
import com.hullcheck.ui.theme.severityColor

@Composable
fun DefectsScreen(
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
    viewModel: DefectsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    androidx.compose.material3.Scaffold(
        topBar = { HullCheckTopBar(title = stringResource(R.string.defects_title), onBack = onBack) },
    ) { padding ->
        if (!state.loading && state.total == 0) {
            EmptyState(
                icon = Icons.Outlined.CheckCircle,
                title = stringResource(R.string.empty_defects_title),
                subtitle = stringResource(R.string.empty_defects_subtitle),
                modifier = Modifier.padding(padding),
            )
            return@Scaffold
        }

        LazyColumn(
            Modifier.padding(padding).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                    Text(
                        stringResource(R.string.defects_summary, state.total, state.safety, state.major, state.moderate, state.minor),
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            state.groups.forEach { group ->
                item(key = "header-${group.severity}") {
                    Row(
                        Modifier.padding(top = 8.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(severityColor(group.severity)))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "${group.severity.labelText()} · ${group.entries.size}",
                            style = MaterialTheme.typography.titleSmall,
                            color = severityColor(group.severity),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                items(group.entries, key = { it.item.id }) { entry ->
                    DefectCard(
                        title = entry.item.title,
                        category = entry.categoryName,
                        severity = group.severity,
                        condition = entry.item.condition?.labelText(),
                        notes = entry.item.notes,
                        photos = entry.item.photos,
                        onClick = { onOpenItem(entry.item.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DefectCard(
    title: String,
    category: String,
    severity: Severity,
    condition: String?,
    notes: String?,
    photos: List<com.hullcheck.data.model.Photo>,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            Box(Modifier.width(4.dp).fillMaxHeight().background(severityColor(severity)))
            Column(Modifier.padding(14.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.size(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (category.isNotBlank()) {
                        Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                            Text(category, Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    if (condition != null) {
                        Text(condition, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                notes?.takeIf { it.isNotBlank() }?.let {
                    Spacer(Modifier.size(6.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (photos.isNotEmpty()) {
                    Spacer(Modifier.size(10.dp))
                    PhotoThumbnailRow(photos = photos, thumbSize = 64.dp, onPhotoClick = { onClick() })
                }
            }
        }
    }
}
