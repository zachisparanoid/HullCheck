package com.hullcheck.ui.itemdetail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.hullcheck.R
import com.hullcheck.data.model.ChecklistItem
import com.hullcheck.data.model.Photo
import com.hullcheck.ui.camera.CameraCaptureScreen
import com.hullcheck.ui.camera.hasCamera
import com.hullcheck.ui.components.ConditionSelector
import com.hullcheck.ui.components.FullScreenLoading
import com.hullcheck.ui.components.HullCheckTopBar
import com.hullcheck.ui.components.PhotoThumbnailRow
import com.hullcheck.ui.components.SectionHeader
import com.hullcheck.ui.components.SeveritySelector
import com.hullcheck.ui.components.StatusSelector
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ItemDetailScreen(
    onBack: () -> Unit,
    viewModel: ItemDetailViewModel = hiltViewModel(),
) {
    val item by viewModel.item.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showCamera by remember { mutableStateOf(false) }
    var viewerPhoto by remember { mutableStateOf<Photo?>(null) }
    var annotatingPhoto by remember { mutableStateOf<Photo?>(null) }
    var editTitle by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(viewModel::addPhotoFromUri)
    }

    LaunchedEffect(Unit) {
        viewModel.eventFlow.collect { event ->
            when (event) {
                ItemDetailEvent.PhotoAdded -> snackbarHostState.showSnackbar(context.getString(R.string.photo_added))
                is ItemDetailEvent.Error ->
                    snackbarHostState.showSnackbar(event.message.ifBlank { context.getString(R.string.photo_save_failed) })
            }
        }
    }

    fun deletePhoto(photo: Photo) {
        viewModel.softDeletePhoto(photo)
        scope.launch {
            val result = snackbarHostState.showSnackbar(
                message = context.getString(R.string.photo_removed),
                actionLabel = context.getString(R.string.action_undo),
            )
            if (result == SnackbarResult.ActionPerformed) viewModel.restorePhoto(photo) else viewModel.finalizeDelete(photo)
        }
    }

    if (showCamera) {
        CameraCaptureScreen(
            onCaptured = { file -> viewModel.addPhotoFromCapture(file); showCamera = false },
            onClose = { showCamera = false },
        )
        return
    }

    val current = item
    Scaffold(
        topBar = {
            HullCheckTopBar(
                title = stringResource(R.string.item_detail_title),
                onBack = onBack,
                actions = {
                    if (current != null && !current.isCustom) {
                        IconButton(onClick = { editTitle = !editTitle }) {
                            Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.edit_title))
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (current == null) {
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
            if (current.isCustom || editTitle) {
                AutoSaveTextField(
                    key = current.id + "-title",
                    initial = current.title,
                    label = stringResource(R.string.item_title_hint),
                    onSave = viewModel::setTitle,
                    singleLine = true,
                )
            } else {
                Text(current.title, style = MaterialTheme.typography.headlineSmall)
            }

            current.guidance?.takeIf { it.isNotBlank() }?.let { guidance ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(10.dp))
                        Column {
                            Text(stringResource(R.string.label_guidance), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(guidance, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(stringResource(R.string.label_status))
                StatusSelector(current.status, viewModel::setStatus)
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(stringResource(R.string.label_condition))
                ConditionSelector(current.condition, viewModel::setCondition)
            }
            if (current.status.isDefectLike) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionHeader(stringResource(R.string.label_severity))
                    SeveritySelector(current.severity, viewModel::setSeverity)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(stringResource(R.string.label_notes))
                AutoSaveTextField(
                    key = current.id + "-notes",
                    initial = current.notes.orEmpty(),
                    label = stringResource(R.string.notes_hint),
                    onSave = viewModel::setNotes,
                    singleLine = false,
                    minLines = 3,
                )
            }

            MeasurementSection(current, viewModel::setMeasurement)
            ExpirySection(current, viewModel::setExpiry)

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(stringResource(R.string.label_photos))
                PhotoThumbnailRow(
                    photos = current.photos,
                    onPhotoClick = { index -> viewerPhoto = current.photos.getOrNull(index) },
                    onAddCamera = if (context.hasCamera()) ({ showCamera = true }) else null,
                    onAddGallery = {
                        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                )
            }
            Spacer(Modifier.size(24.dp))
        }
    }

    viewerPhoto?.let { photo ->
        PhotoViewer(
            photo = photo,
            onClose = { viewerPhoto = null },
            onCaptionSave = { caption -> viewModel.setCaption(photo.id, caption) },
            onDelete = { viewerPhoto = null; deletePhoto(photo) },
            onAnnotate = { annotatingPhoto = photo; viewerPhoto = null },
        )
    }

    annotatingPhoto?.let { photo ->
        PhotoAnnotationScreen(
            photo = photo,
            onSaved = { bitmap -> viewModel.addAnnotatedPhoto(bitmap); annotatingPhoto = null },
            onClose = { annotatingPhoto = null },
        )
    }
}

/** OutlinedTextField that debounces (500 ms) and autosaves; re-seeds when [key] changes. */
@Composable
private fun AutoSaveTextField(
    key: String,
    initial: String,
    label: String,
    onSave: (String) -> Unit,
    singleLine: Boolean,
    minLines: Int = 1,
) {
    var text by rememberSaveable(key) { mutableStateOf(initial) }
    val lastSaved = remember(key) { mutableStateOf(initial) }
    LaunchedEffect(text, key) {
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
private fun PhotoViewer(
    photo: Photo,
    onClose: () -> Unit,
    onCaptionSave: (String) -> Unit,
    onDelete: () -> Unit,
    onAnnotate: () -> Unit,
) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            var scale by remember { mutableStateOf(1f) }
            var offset by remember { mutableStateOf(Offset.Zero) }
            val transformState = rememberTransformableState { zoomChange, panChange, _ ->
                scale = (scale * zoomChange).coerceIn(1f, 5f)
                offset += panChange
            }
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(File(photo.filePath)).build(),
                contentDescription = stringResource(R.string.cd_full_screen_photo),
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)
                    .transformable(transformState),
            )

            Row(
                Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_close), tint = Color.White)
                }
                Row {
                    IconButton(onClick = onAnnotate) {
                        Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.annotate), tint = Color.White)
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Outlined.Delete, contentDescription = stringResource(R.string.action_delete), tint = Color.White)
                    }
                }
            }

            CaptionEditor(
                key = photo.id,
                initial = photo.caption.orEmpty(),
                onSave = onCaptionSave,
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp),
            )
        }
    }
}

@Composable
private fun CaptionEditor(
    key: String,
    initial: String,
    onSave: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by rememberSaveable(key) { mutableStateOf(initial) }
    val lastSaved = remember(key) { mutableStateOf(initial) }
    LaunchedEffect(text, key) {
        if (text != lastSaved.value) {
            delay(500)
            onSave(text)
            lastSaved.value = text
        }
    }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text(stringResource(R.string.photo_caption_hint)) },
        singleLine = true,
        modifier = modifier,
    )
}
