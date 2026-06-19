package com.hullcheck.ui.itemdetail

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hullcheck.data.model.ChecklistItem
import com.hullcheck.data.model.ConditionRating
import com.hullcheck.data.model.ItemStatus
import com.hullcheck.data.model.Photo
import com.hullcheck.data.model.Severity
import com.hullcheck.data.repository.ChecklistRepository
import com.hullcheck.data.repository.PhotoRepository
import com.hullcheck.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface ItemDetailEvent {
    data object PhotoAdded : ItemDetailEvent
    data class Error(val message: String) : ItemDetailEvent
}

@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val checklistRepository: ChecklistRepository,
    private val photoRepository: PhotoRepository,
) : ViewModel() {

    private val itemId: String = checkNotNull(savedStateHandle[Routes.ARG_ITEM_ID])

    val item: StateFlow<ChecklistItem?> = checklistRepository.observeItem(itemId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val events = Channel<ItemDetailEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    fun setStatus(status: ItemStatus) = withItem { checklistRepository.setStatus(itemId, it.projectId, status) }

    fun setCondition(condition: ConditionRating?) = withItem {
        checklistRepository.setCondition(itemId, it.projectId, condition)
    }

    fun setSeverity(severity: Severity?) = withItem {
        // Keep current status, just change severity.
        checklistRepository.setStatus(itemId, it.projectId, it.status, severity)
    }

    fun setNotes(notes: String) = withItem { checklistRepository.setNotes(itemId, it.projectId, notes) }

    fun setTitle(title: String) = withItem { if (title.isNotBlank()) checklistRepository.setTitle(itemId, it.projectId, title) }

    fun setExpiry(date: java.time.LocalDate?) = withItem { checklistRepository.setExpiry(itemId, it.projectId, date) }

    fun setMeasurement(value: Double?, unit: String?) = withItem {
        checklistRepository.setMeasurement(itemId, it.projectId, value, unit)
    }

    fun addPhotoFromUri(uri: Uri) = withItem {
        runCatching { photoRepository.addPhotoFromUri(it.projectId, itemId, uri) }
            .onSuccess { events.send(ItemDetailEvent.PhotoAdded) }
            .onFailure { events.send(ItemDetailEvent.Error(it.message ?: "")) }
    }

    fun addPhotoFromCapture(file: File) = withItem {
        runCatching { photoRepository.addPhotoFromCapture(it.projectId, itemId, file) }
            .onSuccess { events.send(ItemDetailEvent.PhotoAdded) }
            .onFailure { events.send(ItemDetailEvent.Error(it.message ?: "")) }
    }

    fun addAnnotatedPhoto(bitmap: android.graphics.Bitmap) = withItem {
        runCatching { photoRepository.addPhotoBitmap(it.projectId, itemId, bitmap) }
            .onSuccess { events.send(ItemDetailEvent.PhotoAdded) }
            .onFailure { events.send(ItemDetailEvent.Error(it.message ?: "")) }
    }

    fun softDeletePhoto(photo: Photo) = viewModelScope.launch { photoRepository.softDeletePhoto(photo) }
    fun restorePhoto(photo: Photo) = viewModelScope.launch { photoRepository.restorePhoto(photo) }
    fun finalizeDelete(photo: Photo) = viewModelScope.launch { photoRepository.finalizeDelete(photo) }
    fun setCaption(photoId: String, caption: String) = viewModelScope.launch { photoRepository.setCaption(photoId, caption) }

    private inline fun withItem(crossinline block: suspend (ChecklistItem) -> Unit) {
        val current = item.value ?: return
        viewModelScope.launch { block(current) }
    }
}
