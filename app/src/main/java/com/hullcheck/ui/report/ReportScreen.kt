package com.hullcheck.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hullcheck.R
import com.hullcheck.data.model.Project
import com.hullcheck.data.model.ProjectProgress
import com.hullcheck.data.model.Severity
import com.hullcheck.ui.components.FullScreenLoading
import com.hullcheck.ui.components.HullCheckTopBar
import com.hullcheck.ui.components.SectionHeader
import com.hullcheck.ui.components.StatusBadge
import com.hullcheck.ui.components.labelText
import com.hullcheck.ui.theme.severityColor
import com.hullcheck.util.DateFormatters
import com.hullcheck.util.ShareUtils
import kotlinx.coroutines.delay
import java.io.File

@Composable
fun ReportScreen(
    onBack: () -> Unit,
    viewModel: ReportViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is ReportEvent.ShareFile -> ShareUtils.shareFile(context, event.uri, event.mime, event.name)
                is ReportEvent.PdfReady -> {
                    ShareUtils.openFile(context, event.uri, ShareUtils.MIME_PDF)
                    val result = snackbarHostState.showSnackbar(
                        message = context.getString(R.string.report_ready),
                        actionLabel = context.getString(R.string.action_share),
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        ShareUtils.shareFile(context, event.uri, ShareUtils.MIME_PDF, event.name)
                    }
                }
                is ReportEvent.Toast -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    val project = state.project
    Scaffold(
        topBar = { HullCheckTopBar(title = stringResource(R.string.report_title), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Button(
                        onClick = viewModel::exportPdf,
                        enabled = !state.generating && project != null,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (state.generating) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(Icons.Outlined.PictureAsPdf, null, Modifier.size(18.dp))
                        }
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.export_pdf))
                    }
                    OutlinedButton(
                        onClick = viewModel::exportJson,
                        enabled = project != null,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Outlined.FileUpload, null, Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.export_json))
                    }
                }
            }
        },
    ) { padding ->
        if (project == null) {
            FullScreenLoading(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CoverPreview(project)
            SummaryEditor(project, viewModel::setSummary)
            SummaryStats(state.progress)
            DefectPreview(project)
            ChecklistPreview(project)
            PhotoAppendixPreview(project)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CoverPreview(project: Project) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(16.dp)) {
            Text(
                stringResource(R.string.report_title).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(project.boatName, style = MaterialTheme.typography.headlineSmall)
            project.coverPhotoPath?.let { path ->
                Spacer(Modifier.height(12.dp))
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(File(path)).crossfade(true).build(),
                    contentDescription = stringResource(R.string.cd_project_cover),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(180.dp).clip(RoundedCornerShape(12.dp)),
                )
            }
            Spacer(Modifier.height(12.dp))
            MetaLine(stringResource(R.string.field_survey_type), project.surveyType.labelText())
            MetaLine(stringResource(R.string.field_survey_date), DateFormatters.formatDate(project.surveyDate))
            project.surveyorName?.let { MetaLine(stringResource(R.string.field_surveyor_name), it) }
            project.clientName?.let { MetaLine(stringResource(R.string.field_client_name), it) }
            project.makeModelYear.takeIf { it.isNotBlank() }?.let { MetaLine(stringResource(R.string.section_vessel), it) }
            project.location?.let { MetaLine(stringResource(R.string.field_location), it) }
            project.coordinatesText?.let { MetaLine(stringResource(R.string.coordinates), it) }
            project.hullId?.let { MetaLine(stringResource(R.string.field_hull_id), it) }
        }
    }
}

@Composable
private fun MetaLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.4f))
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(0.6f))
    }
}

@Composable
private fun SummaryEditor(project: Project, onSave: (String) -> Unit) {
    var text by rememberSaveable(project.id) { mutableStateOf(project.reportSummary.orEmpty()) }
    val lastSaved = remember(project.id) { mutableStateOf(project.reportSummary.orEmpty()) }
    LaunchedEffect(text, project.id) {
        if (text != lastSaved.value) {
            delay(500)
            onSave(text)
            lastSaved.value = text
        }
    }
    Column {
        SectionHeader(stringResource(R.string.executive_summary))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            placeholder = { Text(stringResource(R.string.executive_summary_hint)) },
            minLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun SummaryStats(progress: ProjectProgress) {
    Column {
        SectionHeader(stringResource(R.string.report_section_summary))
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.checked_of_total, progress.completed, progress.total) + "  ·  ${progress.percentInt}%",
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            "OK ${progress.ok}   ·   ${stringResource(R.string.quick_stat_defects)} ${progress.defectTotal}   ·   N/A ${progress.na}   ·   ${stringResource(R.string.quick_stat_pending)} ${progress.pending}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DefectPreview(project: Project) {
    val defects = project.allItems.filter { it.status.isDefectLike }
    Column {
        SectionHeader(stringResource(R.string.report_section_defects))
        Spacer(Modifier.height(8.dp))
        if (defects.isEmpty()) {
            Text(stringResource(R.string.report_no_defects), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            return
        }
        Severity.entries.sortedByDescending { it.rank }.forEach { severity ->
            val group = defects.filter { (it.severity ?: Severity.MODERATE) == severity }
            if (group.isEmpty()) return@forEach
            Text(
                "${severity.labelText()} · ${group.size}",
                style = MaterialTheme.typography.titleSmall,
                color = severityColor(severity),
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 6.dp),
            )
            group.forEach { item ->
                Text("• ${item.title}", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(start = 4.dp, top = 2.dp))
            }
        }
    }
}

@Composable
private fun ChecklistPreview(project: Project) {
    Column {
        SectionHeader(stringResource(R.string.report_section_checklist))
        Spacer(Modifier.height(4.dp))
        project.categories.sortedBy { it.sortOrder }.forEach { category ->
            Text(category.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 10.dp, bottom = 4.dp))
            category.items.sortedBy { it.sortOrder }.forEach { item ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    StatusBadge(item.status)
                    Spacer(Modifier.size(8.dp))
                    Text(item.title, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    val extra = listOfNotNull(
                        item.measurementText,
                        item.expiryDate?.let { DateFormatters.formatDate(it) },
                    ).joinToString(" · ")
                    if (extra.isNotBlank()) {
                        Text(extra, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoAppendixPreview(project: Project) {
    val photos = project.allItems.flatMap { it.photos }
    if (photos.isEmpty()) return
    Column {
        SectionHeader(stringResource(R.string.report_section_photos))
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            photos.take(30).forEach { photo ->
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(File(photo.filePath)).crossfade(true).build(),
                    contentDescription = photo.caption,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(88.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest),
                )
            }
        }
    }
}
