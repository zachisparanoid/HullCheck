package com.hullcheck.ui.projects

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hullcheck.data.model.ProjectFilter
import com.hullcheck.data.model.ProjectSort
import com.hullcheck.data.model.ProjectStatus
import com.hullcheck.data.model.ProjectSummary
import com.hullcheck.data.repository.ProjectRepository
import com.hullcheck.data.repository.ReportRepository
import com.hullcheck.data.repository.SettingsRepository
import com.hullcheck.util.FileStorage
import com.hullcheck.util.OpResult
import com.hullcheck.util.ShareUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProjectListUiState(
    val loading: Boolean = true,
    val projects: List<ProjectSummary> = emptyList(),
    val search: String = "",
    val filter: ProjectFilter = ProjectFilter.ALL,
    val sort: ProjectSort = ProjectSort.DATE,
)

sealed interface ProjectListEvent {
    data class Share(val uri: Uri, val mime: String, val name: String) : ProjectListEvent
    data class Toast(val message: String) : ProjectListEvent
}

@HiltViewModel
class ProjectListViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val projectRepository: ProjectRepository,
    private val reportRepository: ReportRepository,
    private val settingsRepository: SettingsRepository,
    private val fileStorage: FileStorage,
) : ViewModel() {

    private val search = MutableStateFlow("")
    private val filter = MutableStateFlow(ProjectFilter.ALL)

    private val events = Channel<ProjectListEvent>(Channel.BUFFERED)
    val eventFlow = events.receiveAsFlow()

    val uiState: StateFlow<ProjectListUiState> = combine(
        projectRepository.observeProjectSummaries(),
        search,
        filter,
        settingsRepository.settings,
    ) { all, query, activeFilter, settings ->
        ProjectListUiState(
            loading = false,
            projects = applyFilters(all, query, activeFilter, settings.projectSort),
            search = query,
            filter = activeFilter,
            sort = settings.projectSort,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProjectListUiState())

    private fun applyFilters(
        all: List<ProjectSummary>,
        query: String,
        filter: ProjectFilter,
        sort: ProjectSort,
    ): List<ProjectSummary> {
        val q = query.trim().lowercase()
        val filtered = all.filter { summary ->
            val statusOk = when (filter) {
                ProjectFilter.ALL -> summary.project.status != ProjectStatus.ARCHIVED
                ProjectFilter.IN_PROGRESS -> summary.project.status == ProjectStatus.IN_PROGRESS
                ProjectFilter.COMPLETED -> summary.project.status == ProjectStatus.COMPLETED
                ProjectFilter.ARCHIVED -> summary.project.status == ProjectStatus.ARCHIVED
            }
            val searchOk = q.isBlank() || listOfNotNull(
                summary.project.boatName,
                summary.project.make,
                summary.project.model,
                summary.project.clientName,
                summary.project.hullId,
            ).any { it.lowercase().contains(q) }
            statusOk && searchOk
        }
        return when (sort) {
            ProjectSort.DATE -> filtered.sortedByDescending { it.project.surveyDate }
            ProjectSort.NAME -> filtered.sortedBy { it.project.boatName.lowercase() }
            ProjectSort.PROGRESS -> filtered.sortedByDescending { it.percent }
            ProjectSort.STATUS -> filtered.sortedBy { it.project.status.ordinal }
        }
    }

    fun setSearch(value: String) = search.update { value }
    fun setFilter(value: ProjectFilter) = filter.update { value }

    fun setSort(value: ProjectSort) {
        viewModelScope.launch { settingsRepository.setProjectSort(value) }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch { projectRepository.deleteProject(projectId) }
    }

    fun archive(projectId: String, archived: Boolean) {
        viewModelScope.launch {
            if (archived) projectRepository.archive(projectId) else projectRepository.reopen(projectId)
        }
    }

    fun duplicate(projectId: String, copyResults: Boolean, copyPhotos: Boolean) {
        viewModelScope.launch { projectRepository.duplicateProject(projectId, copyResults, copyPhotos) }
    }

    fun exportJson(projectId: String) {
        viewModelScope.launch {
            when (val result = reportRepository.exportJson(projectId)) {
                is OpResult.Success -> events.send(
                    ProjectListEvent.Share(
                        fileStorage.uriForFile(result.data),
                        ShareUtils.MIME_JSON,
                        result.data.name,
                    ),
                )
                is OpResult.Failure -> events.send(ProjectListEvent.Toast(result.message))
            }
        }
    }

    fun importProject(uri: Uri) {
        viewModelScope.launch {
            val stream = runCatching { context.contentResolver.openInputStream(uri) }.getOrNull()
            if (stream == null) {
                events.send(ProjectListEvent.Toast(context.getString(com.hullcheck.R.string.import_invalid)))
                return@launch
            }
            when (val result = reportRepository.importJson(stream)) {
                is OpResult.Success -> {
                    val summary = result.data
                    val message = if (summary.photosMissing > 0) {
                        context.getString(com.hullcheck.R.string.import_photos_missing, summary.photosMissing)
                    } else {
                        context.getString(com.hullcheck.R.string.import_success, summary.boatName)
                    }
                    events.send(ProjectListEvent.Toast(message))
                }
                is OpResult.Failure -> events.send(
                    ProjectListEvent.Toast(context.getString(com.hullcheck.R.string.import_failed, result.message)),
                )
            }
        }
    }
}
