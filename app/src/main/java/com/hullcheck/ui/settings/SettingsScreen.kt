package com.hullcheck.ui.settings

import android.os.Build
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hullcheck.BuildConfig
import com.hullcheck.R
import com.hullcheck.data.model.LengthUnit
import com.hullcheck.data.model.PhotoQuality
import com.hullcheck.data.model.SurveyType
import com.hullcheck.data.model.ThemeMode
import com.hullcheck.ui.components.ConfirmDialog
import com.hullcheck.ui.components.HullCheckTopBar
import com.hullcheck.ui.components.HullFilterChip
import com.hullcheck.ui.components.SectionHeader
import com.hullcheck.ui.components.labelText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenTemplates: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showDelete1 by remember { mutableStateOf(false) }
    var showDelete2 by remember { mutableStateOf(false) }
    val deletedMessage = stringResource(R.string.settings_delete_all_done)

    Scaffold(
        topBar = { HullCheckTopBar(title = stringResource(R.string.settings_title), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ----- Defaults -----
            SectionHeader(stringResource(R.string.settings_section_defaults))
            SurveyorNameField(initial = settings.surveyorName, onSave = viewModel::setSurveyorName)
            SettingBlock(stringResource(R.string.settings_default_survey_type)) {
                ChipRow(
                    options = SurveyType.entries,
                    selected = settings.defaultSurveyType,
                    label = { it.labelText() },
                    onSelect = viewModel::setDefaultSurveyType,
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ----- Appearance -----
            SectionHeader(stringResource(R.string.settings_section_appearance))
            SettingBlock(stringResource(R.string.settings_theme)) {
                ChipRow(
                    options = ThemeMode.entries,
                    selected = settings.themeMode,
                    label = { it.labelText() },
                    onSelect = viewModel::setThemeMode,
                )
            }
            SettingBlock(stringResource(R.string.settings_units)) {
                ChipRow(
                    options = LengthUnit.entries,
                    selected = settings.lengthUnit,
                    label = { it.labelText() },
                    onSelect = viewModel::setLengthUnit,
                )
            }
            SettingBlock(stringResource(R.string.settings_photo_quality)) {
                ChipRow(
                    options = PhotoQuality.entries,
                    selected = settings.photoQuality,
                    label = { it.labelText() },
                    onSelect = viewModel::setPhotoQuality,
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.settings_dynamic_color), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = settings.dynamicColor, onCheckedChange = viewModel::setDynamicColor)
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            BrandingSection(settings, viewModel)

            OutlinedButton(onClick = onOpenTemplates, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.manage_templates))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ----- About -----
            SectionHeader(stringResource(R.string.settings_section_about))
            Text(stringResource(R.string.settings_about_version, BuildConfig.VERSION_NAME), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(R.string.settings_about_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ----- Danger zone -----
            SectionHeader(stringResource(R.string.settings_section_danger))
            Button(
                onClick = { showDelete1 = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.settings_delete_all))
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showDelete1) {
        ConfirmDialog(
            title = stringResource(R.string.settings_delete_all_confirm1_title),
            message = stringResource(R.string.settings_delete_all_confirm1_message),
            confirmLabel = stringResource(R.string.action_continue),
            destructive = true,
            onConfirm = { showDelete1 = false; showDelete2 = true },
            onDismiss = { showDelete1 = false },
        )
    }
    if (showDelete2) {
        ConfirmDialog(
            title = stringResource(R.string.settings_delete_all_confirm2_title),
            message = stringResource(R.string.settings_delete_all_confirm2_message),
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = {
                showDelete2 = false
                viewModel.deleteAllData()
                scope.launch { snackbarHostState.showSnackbar(deletedMessage) }
            },
            onDismiss = { showDelete2 = false },
        )
    }
}

@Composable
private fun SurveyorNameField(initial: String, onSave: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    val lastSaved = remember { mutableStateOf(initial) }
    // Adopt the stored value once it loads, but never clobber active typing.
    LaunchedEffect(initial) {
        if (initial.isNotEmpty() && text.isEmpty() && lastSaved.value.isEmpty()) {
            text = initial
            lastSaved.value = initial
        }
    }
    LaunchedEffect(text) {
        if (text != lastSaved.value) {
            delay(500)
            onSave(text)
            lastSaved.value = text
        }
    }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(stringResource(R.string.settings_default_surveyor)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SettingBlock(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.titleSmall)
        content()
    }
}

@Composable
private fun <T> ChipRow(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { option ->
            HullFilterChip(
                selected = selected == option,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
            )
        }
    }
}
