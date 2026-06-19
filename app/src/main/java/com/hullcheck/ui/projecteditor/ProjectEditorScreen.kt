package com.hullcheck.ui.projecteditor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hullcheck.R
import com.hullcheck.data.model.BoatType
import com.hullcheck.data.model.LengthUnit
import com.hullcheck.data.model.SurveyType
import com.hullcheck.ui.components.ConfirmDialog
import com.hullcheck.ui.components.HullCheckTopBar
import com.hullcheck.ui.components.SectionHeader
import com.hullcheck.ui.components.labelText
import com.hullcheck.util.DateFormatters
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun ProjectEditorScreen(
    onSaved: (projectId: String, wasEditing: Boolean) -> Unit,
    onBack: () -> Unit,
    viewModel: ProjectEditorViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDiscard by remember { mutableStateOf(false) }

    LaunchedEffect(state.savedProjectId) {
        state.savedProjectId?.let { onSaved(it, state.isEditing) }
    }

    val backWithCheck = {
        if (state.dirty) showDiscard = true else onBack()
    }
    BackHandler(enabled = state.dirty) { showDiscard = true }

    Scaffold(
        topBar = {
            HullCheckTopBar(
                title = stringResource(if (state.isEditing) R.string.edit_project_title else R.string.new_project_title),
                onBack = backWithCheck,
                actions = {
                    IconButton(onClick = viewModel::save) {
                        Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.action_save))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val form = state.form

            SectionHeader(stringResource(R.string.section_vessel))
            OutlinedTextField(
                value = form.boatName,
                onValueChange = viewModel::setBoatName,
                label = { Text(stringResource(R.string.field_boat_name)) },
                isError = state.boatNameError,
                supportingText = { if (state.boatNameError) Text(stringResource(R.string.error_boat_name_required)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            EnumDropdownField(
                label = stringResource(R.string.field_boat_type),
                value = form.boatType,
                options = BoatType.entries,
                optionLabel = { it.labelText() },
                onSelect = viewModel::setBoatType,
            )
            if (!state.isEditing && state.templates.size > 1) {
                val selectedTemplate = state.templates.firstOrNull { it.id == state.selectedTemplateId }
                    ?: state.templates.first()
                EnumDropdownField(
                    label = stringResource(R.string.template_label),
                    value = selectedTemplate,
                    options = state.templates,
                    optionLabel = { it.name },
                    onSelect = { viewModel.setTemplate(it.id) },
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = form.make,
                    onValueChange = viewModel::setMake,
                    label = { Text(stringResource(R.string.field_make)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = form.model,
                    onValueChange = viewModel::setModel,
                    label = { Text(stringResource(R.string.field_model)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = form.yearBuilt,
                    onValueChange = viewModel::setYear,
                    label = { Text(stringResource(R.string.field_year_built)) },
                    isError = state.yearError,
                    supportingText = { if (state.yearError) Text(stringResource(R.string.error_year_range, viewModel.maxYear)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                val unitAbbr = stringResource(if (state.lengthUnit == LengthUnit.METERS) R.string.unit_meters_abbr else R.string.unit_feet_abbr)
                OutlinedTextField(
                    value = form.length,
                    onValueChange = viewModel::setLength,
                    label = { Text(stringResource(R.string.field_length, unitAbbr)) },
                    isError = state.lengthError,
                    supportingText = { if (state.lengthError) Text(stringResource(R.string.error_length_range)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            OutlinedTextField(
                value = form.hullId,
                onValueChange = viewModel::setHullId,
                label = { Text(stringResource(R.string.field_hull_id)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.registrationNumber,
                onValueChange = viewModel::setRegistration,
                label = { Text(stringResource(R.string.field_registration)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionHeader(stringResource(R.string.section_survey))
            EnumDropdownField(
                label = stringResource(R.string.field_survey_type),
                value = form.surveyType,
                options = SurveyType.entries,
                optionLabel = { it.labelText() },
                onSelect = viewModel::setSurveyType,
            )
            DateField(
                label = stringResource(R.string.field_survey_date),
                date = form.surveyDate,
                onDate = viewModel::setSurveyDate,
            )
            OutlinedTextField(
                value = form.location,
                onValueChange = viewModel::setLocation,
                label = { Text(stringResource(R.string.field_location)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.clientName,
                onValueChange = viewModel::setClientName,
                label = { Text(stringResource(R.string.field_client_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = form.surveyorName,
                onValueChange = viewModel::setSurveyorName,
                label = { Text(stringResource(R.string.field_surveyor_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionHeader(stringResource(R.string.location_section))
            LocationCaptureRow(
                coords = form.latitude?.let { lat -> form.longitude?.let { lon -> "%.5f, %.5f".format(lat, lon) } },
                onCaptured = viewModel::setLocation,
                onClear = viewModel::clearLocation,
            )

            SectionHeader(stringResource(R.string.section_notes))
            OutlinedTextField(
                value = form.generalNotes,
                onValueChange = viewModel::setGeneralNotes,
                label = { Text(stringResource(R.string.field_general_notes)) },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (showDiscard) {
        ConfirmDialog(
            title = stringResource(R.string.discard_changes_title),
            message = stringResource(R.string.discard_changes_message),
            confirmLabel = stringResource(R.string.action_discard),
            dismissLabel = stringResource(R.string.action_keep_editing),
            destructive = true,
            onConfirm = { showDiscard = false; onBack() },
            onDismiss = { showDiscard = false },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> EnumDropdownField(
    label: String,
    value: T,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = optionLabel(value),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(androidx.compose.material3.MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = { onSelect(option); expanded = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(label: String, date: LocalDate, onDate: (LocalDate) -> Unit) {
    var show by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = DateFormatters.formatDate(date),
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        trailingIcon = {
            IconButton(onClick = { show = true }) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = stringResource(R.string.field_survey_date))
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
    if (show) {
        val initialMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        onDate(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    show = false
                }) { Text(stringResource(R.string.action_done)) }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
