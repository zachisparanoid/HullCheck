package com.hullcheck.ui.projectdetail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.ReportProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hullcheck.R
import com.hullcheck.data.model.CategoryWithProgress
import com.hullcheck.data.model.Project
import com.hullcheck.data.model.ProjectProgress
import com.hullcheck.data.model.ProjectStatus
import com.hullcheck.ui.camera.CameraCaptureScreen
import com.hullcheck.ui.camera.hasCamera
import com.hullcheck.ui.components.ConfirmDialog
import com.hullcheck.ui.components.FullScreenLoading
import com.hullcheck.ui.components.HullCheckTopBar
import com.hullcheck.ui.components.ProgressRing
import com.hullcheck.ui.components.SectionHeader
import com.hullcheck.ui.components.StatTile
import com.hullcheck.ui.components.categoryIcon
import com.hullcheck.ui.theme.StatusDefect
import com.hullcheck.ui.theme.StatusNa
import com.hullcheck.ui.theme.StatusNeedsService
import com.hullcheck.ui.theme.StatusOk
import com.hullcheck.util.DateFormatters
import com.hullcheck.util.ShareUtils
import java.io.File

@Composable
fun ProjectDetailScreen(
    onBack: () -> Unit,
    onEditProject: (String) -> Unit,
    onOpenChecklist: (projectId: String, categoryId: String) -> Unit,
    onViewDefects: (String) -> Unit,
    onGenerateReport: (String) -> Unit,
    viewModel: ProjectDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var showCamera by remember { mutableStateOf(false) }
    var coverMenuOpen by remember { mutableStateOf(false) }
    var overflowOpen by remember { mutableStateOf(false) }
    var showAddCategory by remember { mutableStateOf(false) }
    var showSaveTemplate by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showIncompleteConfirm by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::setCoverFromUri)
    }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is ProjectDetailEvent.Share -> ShareUtils.shareFile(context, event.uri, event.mime, event.name)
                is ProjectDetailEvent.Toast -> snackbarHostState.showSnackbar(event.message)
                ProjectDetailEvent.TemplateSaved -> snackbarHostState.showSnackbar(context.getString(R.string.template_saved))
                ProjectDetailEvent.Deleted -> onBack()
            }
        }
    }

    if (showCamera) {
        CameraCaptureScreen(
            onCaptured = { file -> viewModel.setCoverFromCapture(file); showCamera = false },
            onClose = { showCamera = false },
        )
        return
    }

    val project = state.project
    Scaffold(
        topBar = {
            HullCheckTopBar(
                title = project?.boatName ?: stringResource(R.string.loading),
                subtitle = project?.makeModelYear?.takeIf { it.isNotBlank() },
                onBack = onBack,
                actions = {
                    IconButton(onClick = { overflowOpen = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.action_more))
                    }
                    DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_edit)) },
                            leadingIcon = { Icon(Icons.Outlined.Edit, null) },
                            onClick = { overflowOpen = false; project?.let { onEditProject(it.id) } },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.add_custom_category)) },
                            leadingIcon = { Icon(Icons.Outlined.CreateNewFolder, null) },
                            onClick = { overflowOpen = false; showAddCategory = true },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.save_as_template)) },
                            leadingIcon = { Icon(Icons.Outlined.Bookmark, null) },
                            onClick = { overflowOpen = false; showSaveTemplate = true },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.export_json)) },
                            leadingIcon = { Icon(Icons.Outlined.FileUpload, null) },
                            onClick = { overflowOpen = false; viewModel.exportJson() },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(if (project?.status == ProjectStatus.ARCHIVED) R.string.action_unarchive else R.string.action_archive)) },
                            leadingIcon = { Icon(Icons.Outlined.Archive, null) },
                            onClick = {
                                overflowOpen = false
                                if (project?.status == ProjectStatus.ARCHIVED) viewModel.reopen() else viewModel.archive()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { overflowOpen = false; showDelete = true },
                        )
                    }
                },
            )
        },
        bottomBar = {
            if (project != null) {
                BottomActions(
                    project = project,
                    progress = state.progress,
                    onViewDefects = { onViewDefects(project.id) },
                    onReport = { onGenerateReport(project.id) },
                    onMarkCompleted = {
                        if (state.progress.isComplete) viewModel.markCompleted() else showIncompleteConfirm = true
                    },
                    onReopen = viewModel::reopen,
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (project == null) {
            if (state.loading) FullScreenLoading(Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                HeaderCard(
                    project = project,
                    progress = state.progress,
                    onChangeCover = { coverMenuOpen = true },
                    coverMenuOpen = coverMenuOpen,
                    onDismissCoverMenu = { coverMenuOpen = false },
                    showCameraOption = context.hasCamera(),
                    onPickCamera = { coverMenuOpen = false; showCamera = true },
                    onPickGallery = {
                        coverMenuOpen = false
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onViewDefects = { onViewDefects(project.id) },
                )
            }

            val engine = state.categories.filter { it.category.isEngineRelated }
            val others = state.categories.filter { !it.category.isEngineRelated }

            if (engine.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.group_engine), Modifier.padding(top = 4.dp)) }
                items(engine, key = { it.category.id }) { cat ->
                    CategoryCard(cat) { onOpenChecklist(project.id, cat.category.id) }
                }
            }
            if (others.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.group_systems), Modifier.padding(top = 4.dp)) }
                items(others, key = { it.category.id }) { cat ->
                    CategoryCard(cat) { onOpenChecklist(project.id, cat.category.id) }
                }
            }
        }
    }

    if (showAddCategory) {
        TextInputDialog(
            title = stringResource(R.string.add_custom_category_title),
            hint = stringResource(R.string.category_name_hint),
            confirmLabel = stringResource(R.string.action_add),
            onConfirm = { viewModel.addCustomCategory(it); showAddCategory = false },
            onDismiss = { showAddCategory = false },
        )
    }
    if (showSaveTemplate) {
        TextInputDialog(
            title = stringResource(R.string.save_as_template),
            hint = stringResource(R.string.save_as_template_hint),
            confirmLabel = stringResource(R.string.action_save),
            onConfirm = { viewModel.saveAsTemplate(it); showSaveTemplate = false },
            onDismiss = { showSaveTemplate = false },
        )
    }
    if (showDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_project_title),
            message = stringResource(R.string.delete_project_message),
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = { showDelete = false; viewModel.delete() },
            onDismiss = { showDelete = false },
        )
    }
    if (showIncompleteConfirm) {
        ConfirmDialog(
            title = stringResource(R.string.incomplete_survey_title),
            message = stringResource(R.string.incomplete_survey_message, state.progress.pending),
            confirmLabel = stringResource(R.string.mark_completed),
            onConfirm = { showIncompleteConfirm = false; viewModel.markCompleted() },
            onDismiss = { showIncompleteConfirm = false },
        )
    }
}

@Composable
private fun HeaderCard(
    project: Project,
    progress: ProjectProgress,
    onChangeCover: () -> Unit,
    coverMenuOpen: Boolean,
    onDismissCoverMenu: () -> Unit,
    showCameraOption: Boolean,
    onPickCamera: () -> Unit,
    onPickGallery: () -> Unit,
    onViewDefects: () -> Unit,
) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column {
            Box(Modifier.fillMaxWidth().height(170.dp)) {
                if (project.coverPhotoPath != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(File(project.coverPhotoPath)).crossfade(true).build(),
                        contentDescription = stringResource(R.string.cd_project_cover),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerHighest),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            com.hullcheck.ui.components.boatTypeIcon(project.boatType),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp),
                        )
                    }
                }
                Box(Modifier.align(Alignment.TopEnd).padding(8.dp)) {
                    Surface(color = Color.Black.copy(alpha = 0.4f), shape = RoundedCornerShape(10.dp)) {
                        IconButton(onClick = onChangeCover) {
                            Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = stringResource(R.string.set_cover_photo), tint = Color.White)
                        }
                    }
                    DropdownMenu(expanded = coverMenuOpen, onDismissRequest = onDismissCoverMenu) {
                        if (showCameraOption) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.add_photo_camera)) }, onClick = onPickCamera)
                        }
                        DropdownMenuItem(text = { Text(stringResource(R.string.add_photo_gallery)) }, onClick = onPickGallery)
                    }
                }
            }

            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                ProgressRing(progress.percent, size = 84.dp, contentDescriptionText = stringResource(R.string.cd_progress_ring))
                Spacer(Modifier.size(16.dp))
                Column {
                    Text(
                        stringResource(R.string.checked_of_total, progress.completed, progress.total),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    project.location?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(
                        DateFormatters.formatDate(project.surveyDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    project.coordinatesText?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatTile(progress.ok, stringResource(R.string.quick_stat_ok), StatusOk, Modifier.weight(1f))
                StatTile(progress.defectTotal, stringResource(R.string.quick_stat_defects), StatusDefect, Modifier.weight(1f), onClick = onViewDefects)
                StatTile(progress.needsService, stringResource(R.string.quick_stat_service), StatusNeedsService, Modifier.weight(1f))
                StatTile(progress.na, stringResource(R.string.quick_stat_na), StatusNa, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CategoryCard(item: CategoryWithProgress, onClick: () -> Unit) {
    val category = item.category
    val progress = item.progress
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                Icon(
                    categoryIcon(category.iconKey),
                    contentDescription = stringResource(R.string.cd_category_icon),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp).size(24.dp),
                )
            }
            Spacer(Modifier.size(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(category.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (progress.hasDefects) {
                        Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(StatusDefect))
                        Spacer(Modifier.size(6.dp))
                    }
                    Text(
                        stringResource(R.string.short_checked_of_total, progress.completed, progress.total),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.size(8.dp))
                LinearProgressIndicator(
                    progress = { progress.percent },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
            }
        }
    }
}

@Composable
private fun BottomActions(
    project: Project,
    progress: ProjectProgress,
    onViewDefects: () -> Unit,
    onReport: () -> Unit,
    onMarkCompleted: () -> Unit,
    onReopen: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(onClick = onViewDefects, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.ReportProblem, null, Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(if (progress.defectTotal > 0) "${progress.defectTotal}" else stringResource(R.string.quick_stat_defects), maxLines = 1)
            }
            FilledTonalButton(onClick = onReport, modifier = Modifier.weight(1f)) {
                Icon(Icons.Outlined.PictureAsPdf, null, Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.action_report), maxLines = 1)
            }
            if (project.status == ProjectStatus.COMPLETED) {
                Button(onClick = onReopen, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.mark_in_progress), maxLines = 1)
                }
            } else {
                Button(onClick = onMarkCompleted, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.CheckCircle, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.action_done), maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun TextInputDialog(
    title: String,
    hint: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(hint) },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text) }, enabled = text.isNotBlank()) {
                Text(confirmLabel)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
