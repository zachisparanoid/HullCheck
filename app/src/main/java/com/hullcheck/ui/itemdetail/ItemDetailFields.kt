package com.hullcheck.ui.itemdetail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hullcheck.R
import com.hullcheck.data.model.ChecklistItem
import com.hullcheck.data.model.ExpiryStatus
import com.hullcheck.ui.components.SectionHeader
import com.hullcheck.ui.theme.StatusDefect
import com.hullcheck.ui.theme.StatusNeedsService
import com.hullcheck.util.DateFormatters
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Optional structured reading (value + unit) with debounced autosave. */
@Composable
fun MeasurementSection(item: ChecklistItem, onSave: (Double?, String?) -> Unit) {
    var value by rememberSaveable(item.id + "-mval") {
        mutableStateOf(item.measurementValue?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
    }
    var unit by rememberSaveable(item.id + "-munit") { mutableStateOf(item.measurementUnit.orEmpty()) }
    val lastSaved = remember(item.id) { mutableStateOf(item.measurementValue to item.measurementUnit) }

    LaunchedEffect(value, unit, item.id) {
        val v = value.trim().toDoubleOrNull()
        val u = unit.trim().ifBlank { null }
        if ((v to u) != lastSaved.value) {
            delay(500)
            onSave(v, u)
            lastSaved.value = v to u
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(stringResource(R.string.label_measurement))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                label = { Text(stringResource(R.string.measurement_value)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = unit,
                onValueChange = { unit = it },
                label = { Text(stringResource(R.string.measurement_unit)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Optional expiry date with a picker, clear action, and expired/expiring badge. */
@Composable
fun ExpirySection(item: ChecklistItem, onSet: (LocalDate?) -> Unit) {
    var show by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(stringResource(R.string.label_expiry))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                onClick = { show = true },
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.weight(1f),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.size(10.dp))
                    Text(item.expiryDate?.let { DateFormatters.formatDate(it) } ?: stringResource(R.string.set_date))
                }
            }
            if (item.expiryDate != null) {
                IconButton(onClick = { onSet(null) }) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_clear))
                }
            }
        }
        when (item.expiryStatus()) {
            ExpiryStatus.EXPIRED -> Text(stringResource(R.string.expiry_expired), color = StatusDefect, style = MaterialTheme.typography.labelLarge)
            ExpiryStatus.SOON -> Text(stringResource(R.string.expiry_soon), color = StatusNeedsService, style = MaterialTheme.typography.labelLarge)
            else -> Unit
        }
    }

    if (show) {
        val initialMillis = (item.expiryDate ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        onSet(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    show = false
                }) { Text(stringResource(R.string.action_done)) }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) { DatePicker(state = pickerState) }
    }
}
