package com.hullcheck.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hullcheck.R
import com.hullcheck.data.repository.AppSettings
import com.hullcheck.ui.components.SectionHeader
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun BrandingSection(settings: AppSettings, viewModel: SettingsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(stringResource(R.string.settings_section_branding))
        AutoSaveField(stringResource(R.string.settings_firm_name), settings.firmName, viewModel::setFirmName)
        AutoSaveField(stringResource(R.string.settings_license_number), settings.licenseNumber, viewModel::setLicenseNumber)
        AutoSaveField(stringResource(R.string.settings_disclaimer), settings.reportDisclaimer, viewModel::setReportDisclaimer, singleLine = false, minLines = 2)
        ImagePickerRow(stringResource(R.string.settings_logo), settings.logoPath, viewModel::setLogo, viewModel::clearLogo)
        ImagePickerRow(stringResource(R.string.settings_signature), settings.signaturePath, viewModel::setSignature, viewModel::clearSignature)
    }
}

@Composable
private fun AutoSaveField(
    label: String,
    initial: String,
    onSave: (String) -> Unit,
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    var text by rememberSaveable(label) { mutableStateOf(initial) }
    val lastSaved = remember(label) { mutableStateOf(initial) }
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
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ImagePickerRow(
    label: String,
    path: String?,
    onPick: (android.net.Uri) -> Unit,
    onClear: () -> Unit,
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(onPick)
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            if (path != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(File(path)).build(),
                    contentDescription = label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Icon(Icons.Outlined.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Row {
                TextButton(onClick = {
                    picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) { Text(stringResource(R.string.settings_set_image)) }
                if (path != null) {
                    TextButton(onClick = onClear) { Text(stringResource(R.string.settings_clear_image)) }
                }
            }
        }
    }
}
