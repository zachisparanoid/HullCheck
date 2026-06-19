package com.hullcheck.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hullcheck.data.model.ConditionRating
import com.hullcheck.data.model.ItemStatus
import com.hullcheck.data.model.Severity
import com.hullcheck.ui.theme.SeverityMinor
import com.hullcheck.ui.theme.StatusOk
import com.hullcheck.ui.theme.severityColor
import com.hullcheck.ui.theme.statusColor

private fun contentColorFor(accent: Color): Color =
    if (accent.luminance() > 0.55f) Color(0xFF0B1117) else Color.White

private fun conditionColor(c: ConditionRating): Color = when (c) {
    ConditionRating.EXCELLENT -> StatusOk
    ConditionRating.GOOD -> Color(0xFF13C2C2)
    ConditionRating.FAIR -> SeverityMinor
    ConditionRating.POOR -> Color(0xFFF2994A)
    ConditionRating.FAILED -> Color(0xFFFF5D5D)
}

/** A tappable, color-coded chip with an animated selected state. */
@Composable
fun SelectableChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    fillContent: Boolean = false,
) {
    val container by animateColorAsState(
        targetValue = if (selected) accent else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = tween(150),
        label = "chipContainer",
    )
    val content by animateColorAsState(
        targetValue = if (selected) contentColorFor(accent) else MaterialTheme.colorScheme.onSurface,
        animationSpec = tween(150),
        label = "chipContent",
    )
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = container,
        contentColor = content,
        border = if (selected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.heightIn(min = 44.dp),
    ) {
        Row(
            modifier = (if (fillContent) Modifier.fillMaxWidth() else Modifier)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1)
        }
    }
}

/** Read-only colored status pill for list rows and headers. */
@Composable
fun StatusBadge(status: ItemStatus, modifier: Modifier = Modifier) {
    val c = statusColor(status)
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = c.copy(alpha = 0.18f),
        contentColor = c,
        modifier = modifier,
    ) {
        Text(
            text = status.labelText(),
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** The big inline OK / DEFECT / N/A control used directly in checklist rows. */
@Composable
fun InlineStatusButtons(
    current: ItemStatus,
    onSet: (ItemStatus) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(ItemStatus.OK, ItemStatus.DEFECT, ItemStatus.NOT_APPLICABLE).forEach { status ->
            val selected = current == status
            SelectableChip(
                label = status.labelText(),
                selected = selected,
                accent = statusColor(status),
                onClick = { onSet(if (selected) ItemStatus.PENDING else status) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp),
                fillContent = true,
            )
        }
    }
}

/** All five statuses (item detail). */
@Composable
fun StatusSelector(current: ItemStatus, onSelect: (ItemStatus) -> Unit, modifier: Modifier = Modifier) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ItemStatus.entries.forEach { status ->
            SelectableChip(status.labelText(), current == status, statusColor(status), { onSelect(status) })
        }
    }
}

/** Five-point condition rating; tapping the selected chip clears it (null). */
@Composable
fun ConditionSelector(
    current: ConditionRating?,
    onSelect: (ConditionRating?) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ConditionRating.entries.forEach { condition ->
            SelectableChip(
                label = condition.labelText(),
                selected = current == condition,
                accent = conditionColor(condition),
                onClick = { onSelect(if (current == condition) null else condition) },
            )
        }
    }
}

/** Severity (only shown for defect / needs-service); tapping selected clears it. */
@Composable
fun SeveritySelector(
    current: Severity?,
    onSelect: (Severity?) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Severity.entries.forEach { severity ->
            SelectableChip(
                label = severity.labelText(),
                selected = current == severity,
                accent = severityColor(severity),
                onClick = { onSelect(if (current == severity) null else severity) },
            )
        }
    }
}
