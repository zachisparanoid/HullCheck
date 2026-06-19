package com.hullcheck.ui.report

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hullcheck.data.model.Project
import com.hullcheck.data.model.ProjectProgress
import com.hullcheck.data.repository.ProjectRepository
import com.hullcheck.data.repository.ReportRepository
import com.hullcheck.ui.navigation.Routes
import com.hullcheck.util.FileStorage
import com.hullcheck.util.OpResult
import com.hullcheck.util.ShareUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportUiState(
    val loading: Boolean = true,
    val project: Project? = null,
    val progress: ProjectProgress = ProjectProgress.EMPTY,
    val generating: Boolean = false,
)

sealed interface ReportEvent {
    data class ShareFile(val uri: Uri, val mime: String, val name: String) : ReportEvent
    data class PdfReady(val uri: Uri, val name: String) : ReportEvent
    data class Toast(val message: String) : ReportEvent
}

@HiltViewModel
class ReportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val projectRepository: ProjectRepository,
    private val reportRepository: ReportRepository,
    private val fileStorage: FileStorage,
) : ViewModel() {

    private val projectId: String = checkNotNull(savedStateHandle[Routes.ARG_PROJECT_ID])
    private val generating = MutableStateFlow(false)

    private val events = Channel<ReportEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    val uiState: StateFlow<ReportUiState> = combine(
        projectRepository.observeProject(projectId),
        projectRepository.observeProgress(projectId),
        generating,
    ) { project, progress, isGenerating ->
        ReportUiState(loading = false, project = project, progress = progress, generating = isGenerating)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportUiState())

    fun setSummary(text: String) = viewModelScope.launch {
        projectRepository.setReportSummary(projectId, text.ifBlank { null })
    }

    fun exportPdf() = viewModelScope.launch {
        generating.value = true
        when (val result = reportRepository.generatePdf(projectId)) {
            is OpResult.Success -> events.send(
                ReportEvent.PdfReady(fileStorage.uriForFile(result.data), result.data.name),
            )
            is OpResult.Failure -> events.send(ReportEvent.Toast(result.message))
        }
        generating.value = false
    }

    fun exportJson() = viewModelScope.launch {
        when (val result = reportRepository.exportJson(projectId)) {
            is OpResult.Success -> events.send(
                ReportEvent.ShareFile(fileStorage.uriForFile(result.data), ShareUtils.MIME_JSON, result.data.name),
            )
            is OpResult.Failure -> events.send(ReportEvent.Toast(result.message))
        }
    }
}
