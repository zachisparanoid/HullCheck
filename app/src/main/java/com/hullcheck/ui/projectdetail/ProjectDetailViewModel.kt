package com.hullcheck.ui.projectdetail

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hullcheck.data.model.CategoryWithProgress
import com.hullcheck.data.model.Project
import com.hullcheck.data.model.ProjectProgress
import com.hullcheck.data.repository.ChecklistRepository
import com.hullcheck.data.repository.PhotoRepository
import com.hullcheck.data.repository.ProjectRepository
import com.hullcheck.data.repository.ReportRepository
import com.hullcheck.data.repository.TemplateRepository
import com.hullcheck.ui.navigation.Routes
import com.hullcheck.util.FileStorage
import com.hullcheck.util.OpResult
import com.hullcheck.util.ShareUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

data class ProjectDetailUiState(
    val loading: Boolean = true,
    val project: Project? = null,
    val progress: ProjectProgress = ProjectProgress.EMPTY,
    val categories: List<CategoryWithProgress> = emptyList(),
)

sealed interface ProjectDetailEvent {
    data class Share(val uri: Uri, val mime: String, val name: String) : ProjectDetailEvent
    data class Toast(val message: String) : ProjectDetailEvent
    data object TemplateSaved : ProjectDetailEvent
    data object Deleted : ProjectDetailEvent
}

@HiltViewModel
class ProjectDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projectRepository: ProjectRepository,
    private val checklistRepository: ChecklistRepository,
    private val photoRepository: PhotoRepository,
    private val reportRepository: ReportRepository,
    private val templateRepository: TemplateRepository,
    private val fileStorage: FileStorage,
) : ViewModel() {

    val projectId: String = checkNotNull(savedStateHandle[Routes.ARG_PROJECT_ID])

    private val events = Channel<ProjectDetailEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    val uiState: StateFlow<ProjectDetailUiState> = combine(
        projectRepository.observeProjectMeta(projectId),
        projectRepository.observeProgress(projectId),
        checklistRepository.observeCategoriesWithProgress(projectId),
    ) { project, progress, categories ->
        ProjectDetailUiState(loading = false, project = project, progress = progress, categories = categories)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProjectDetailUiState())

    fun markCompleted() = viewModelScope.launch { projectRepository.markCompleted(projectId) }
    fun reopen() = viewModelScope.launch { projectRepository.reopen(projectId) }
    fun archive() = viewModelScope.launch { projectRepository.archive(projectId) }

    fun delete() = viewModelScope.launch {
        projectRepository.deleteProject(projectId)
        events.send(ProjectDetailEvent.Deleted)
    }

    fun addCustomCategory(name: String) = viewModelScope.launch {
        if (name.isNotBlank()) checklistRepository.addCustomCategory(projectId, name)
    }

    fun saveAsTemplate(name: String) = viewModelScope.launch {
        if (name.isNotBlank()) {
            templateRepository.saveProjectAsTemplate(projectId, name)
            events.send(ProjectDetailEvent.TemplateSaved)
        }
    }

    fun setCoverFromUri(uri: Uri) = viewModelScope.launch {
        val photo = photoRepository.addPhotoFromUri(projectId, null, uri)
        projectRepository.setCoverPhoto(projectId, photo.filePath)
    }

    fun setCoverFromCapture(file: File) = viewModelScope.launch {
        val photo = photoRepository.addPhotoFromCapture(projectId, null, file)
        projectRepository.setCoverPhoto(projectId, photo.filePath)
    }

    fun exportJson() = viewModelScope.launch {
        when (val result = reportRepository.exportJson(projectId)) {
            is OpResult.Success -> events.send(
                ProjectDetailEvent.Share(fileStorage.uriForFile(result.data), ShareUtils.MIME_JSON, result.data.name),
            )
            is OpResult.Failure -> events.send(ProjectDetailEvent.Toast(result.message))
        }
    }
}
