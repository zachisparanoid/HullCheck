package com.hullcheck.ui.projects

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Anchor
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Unarchive
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.hullcheck.data.model.ProjectFilter
import com.hullcheck.data.model.ProjectSort
import com.hullcheck.data.model.ProjectStatus
import com.hullcheck.data.model.ProjectSummary
import com.hullcheck.ui.components.ConfirmDialog
import com.hullcheck.ui.components.EmptyState
import com.hullcheck.ui.components.HullCheckTopBar
import com.hullcheck.ui.components.HullFilterChip
import com.hullcheck.ui.components.ProgressRing
import com.hullcheck.ui.components.SearchField
import com.hullcheck.ui.components.boatTypeIcon
import com.hullcheck.ui.components.labelText
import com.hullcheck.ui.components.projectStatusColor
import com.hullcheck.util.DateFormatters
import com.hullcheck.util.ShareUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProjectListScreen(
    onOpenProject: (String) -> Unit,
    onNewProject: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: ProjectListViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    var overflowOpen by remember { mutableStateOf(false) }
    var sortOpen by remember { mutableStateOf(false) }
    var actionTarget by remember { mutableStateOf<ProjectSummary?>(null) }
    var deleteTarget by remember { mutableStateOf<ProjectSummary?>(null) }
    var duplicateTarget by remember { mutableStateOf<ProjectSummary?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::importProject) }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                is ProjectListEvent.Share -> ShareUtils.shareFile(context, event.uri, event.mime, event.name)
                is ProjectListEvent.Toast -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            HullCheckTopBar(
                title = stringResource(R.string.projects_title),
                actions = {
                    IconButton(onClick = { overflowOpen = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.action_more))
                    }
                    DropdownMenu(expanded = overflowOpen, onDismissRequest = { overflowOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.menu_import_project)) },
                            leadingIcon = { Icon(Icons.Outlined.FileUpload, null) },
                            onClick = {
                                overflowOpen = false
                                importLauncher.launch(arrayOf("application/json", "application/octet-stream"))
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_settings)) },
                            leadingIcon = { Icon(Icons.Outlined.Settings, null) },
                            onClick = { overflowOpen = false; onOpenSettings() },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(R.string.new_project)) },
                icon = { Icon(Icons.Outlined.Anchor, contentDescription = null) },
                onClick = onNewProject,
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            SearchField(
                value = state.search,
                onValueChange = viewModel::setSearch,
                placeholder = stringResource(R.string.search_projects_hint),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )

            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChips(state.filter, viewModel::setFilter)
                }
                Box {
                    TextButton(onClick = { sortOpen = true }) {
                        Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.sort_label))
                    }
                    SortMenu(sortOpen, state.sort, onDismiss = { sortOpen = false }, onSelect = {
                        viewModel.setSort(it); sortOpen = false
                    })
                }
            }

            if (!state.loading && state.projects.isEmpty()) {
                val searching = state.search.isNotBlank() || state.filter != ProjectFilter.ALL
                EmptyState(
                    icon = Icons.Outlined.Anchor,
                    title = stringResource(if (searching) R.string.empty_search_title else R.string.empty_projects_title),
                    subtitle = stringResource(if (searching) R.string.empty_search_subtitle else R.string.empty_projects_subtitle),
                    ctaLabel = if (searching) null else stringResource(R.string.empty_projects_cta),
                    onCta = if (searching) null else onNewProject,
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 380.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(state.projects, key = { it.project.id }) { summary ->
                        ProjectCard(
                            summary = summary,
                            onClick = { onOpenProject(summary.project.id) },
                            onLongClick = { actionTarget = summary },
                        )
                    }
                }
            }
        }
    }

    // Long-press action sheet
    actionTarget?.let { target ->
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(onDismissRequest = { actionTarget = null }, sheetState = sheetState) {
            val archived = target.project.status == ProjectStatus.ARCHIVED
            ActionSheetItem(Icons.Outlined.ContentCopy, stringResource(R.string.action_duplicate)) {
                duplicateTarget = target; actionTarget = null
            }
            ActionSheetItem(
                if (archived) Icons.Outlined.Unarchive else Icons.Outlined.Archive,
                stringResource(if (archived) R.string.action_unarchive else R.string.action_archive),
            ) {
                viewModel.archive(target.project.id, !archived); actionTarget = null
            }
            ActionSheetItem(Icons.Outlined.FileUpload, stringResource(R.string.action_export)) {
                viewModel.exportJson(target.project.id); actionTarget = null
            }
            ActionSheetItem(Icons.Outlined.Delete, stringResource(R.string.action_delete), destructive = true) {
                deleteTarget = target; actionTarget = null
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    deleteTarget?.let { target ->
        ConfirmDialog(
            title = stringResource(R.string.delete_project_title),
            message = stringResource(R.string.delete_project_message),
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = { viewModel.deleteProject(target.project.id); deleteTarget = null },
            onDismiss = { deleteTarget = null },
        )
    }

    duplicateTarget?.let { target ->
        DuplicateDialog(
            onDismiss = { duplicateTarget = null },
            onConfirm = { copyResults, copyPhotos ->
                viewModel.duplicate(target.project.id, copyResults, copyPhotos)
                duplicateTarget = null
            },
        )
    }
}

@Composable
private fun FilterChips(current: ProjectFilter, onSelect: (ProjectFilter) -> Unit) {
    data class Opt(val filter: ProjectFilter, val labelRes: Int)
    val options = listOf(
        Opt(ProjectFilter.ALL, R.string.filter_all),
        Opt(ProjectFilter.IN_PROGRESS, R.string.filter_in_progress),
        Opt(ProjectFilter.COMPLETED, R.string.filter_completed),
        Opt(ProjectFilter.ARCHIVED, R.string.filter_archived),
    )
    options.forEach { opt ->
        HullFilterChip(
            selected = current == opt.filter,
            onClick = { onSelect(opt.filter) },
            label = { Text(stringResource(opt.labelRes)) },
        )
    }
}

@Composable
private fun SortMenu(
    expanded: Boolean,
    current: ProjectSort,
    onDismiss: () -> Unit,
    onSelect: (ProjectSort) -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        listOf(
            ProjectSort.DATE to R.string.sort_by_date,
            ProjectSort.NAME to R.string.sort_by_name,
            ProjectSort.PROGRESS to R.string.sort_by_progress,
            ProjectSort.STATUS to R.string.sort_by_status,
        ).forEach { (sort, labelRes) ->
            DropdownMenuItem(
                text = { Text(stringResource(labelRes)) },
                trailingIcon = { if (current == sort) Icon(Icons.Filled.Check, null, Modifier.size(16.dp)) },
                onClick = { onSelect(sort) },
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProjectCard(
    summary: ProjectSummary,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val project = summary.project
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                if (project.coverPhotoPath != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current).data(File(project.coverPhotoPath)).crossfade(true).build(),
                        contentDescription = stringResource(R.string.cd_project_cover),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        boatTypeIcon(project.boatType),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    project.boatName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val subtitle = project.makeModelYear.ifBlank { project.boatType.labelText() }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    DateFormatters.formatDate(project.surveyDate),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Pill(project.status.labelText(), projectStatusColor(project.status))
                    if (summary.defects > 0) {
                        Pill(
                            if (summary.defects == 1) stringResource(R.string.defects_count_one)
                            else stringResource(R.string.defects_count, summary.defects),
                            MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Spacer(Modifier.width(10.dp))
            ProgressRing(
                progress = summary.percent,
                size = 56.dp,
                strokeWidth = 6.dp,
                contentDescriptionText = stringResource(R.string.cd_progress_ring),
            )
        }
    }
}

@Composable
private fun Pill(text: String, color: androidx.compose.ui.graphics.Color) {
    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.18f), contentColor = color) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ActionSheetItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    val tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Spacer(Modifier.width(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = tint)
    }
}

@Composable
private fun DuplicateDialog(
    onDismiss: () -> Unit,
    onConfirm: (copyResults: Boolean, copyPhotos: Boolean) -> Unit,
) {
    var copyResults by remember { mutableStateOf(true) }
    var copyPhotos by remember { mutableStateOf(true) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.duplicate_title)) },
        text = {
            Column {
                ToggleRow(stringResource(R.string.duplicate_with_results), copyResults) { copyResults = it }
                ToggleRow(stringResource(R.string.duplicate_copy_photos), copyPhotos) { copyPhotos = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(copyResults, copyPhotos) }) {
                Text(stringResource(R.string.action_duplicate))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(Modifier.width(8.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
