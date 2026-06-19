package com.hullcheck.ui.defects

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hullcheck.data.model.ChecklistItem
import com.hullcheck.data.model.Severity
import com.hullcheck.data.repository.ChecklistRepository
import com.hullcheck.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DefectEntry(val item: ChecklistItem, val categoryName: String)

data class DefectSeverityGroup(val severity: Severity, val entries: List<DefectEntry>)

data class DefectsUiState(
    val loading: Boolean = true,
    val groups: List<DefectSeverityGroup> = emptyList(),
    val total: Int = 0,
    val safety: Int = 0,
    val major: Int = 0,
    val moderate: Int = 0,
    val minor: Int = 0,
)

@HiltViewModel
class DefectsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    checklistRepository: ChecklistRepository,
) : ViewModel() {

    private val projectId: String = checkNotNull(savedStateHandle[Routes.ARG_PROJECT_ID])

    val uiState: StateFlow<DefectsUiState> = combine(
        checklistRepository.observeDefects(projectId),
        checklistRepository.observeCategoryNames(projectId),
    ) { defects, categoryNames ->
        // Items flagged without an explicit severity are treated as MODERATE for grouping.
        val bySeverity = defects.groupBy { it.severity ?: Severity.MODERATE }
        val groups = Severity.entries.sortedByDescending { it.rank }.mapNotNull { severity ->
            val items = bySeverity[severity].orEmpty()
            if (items.isEmpty()) {
                null
            } else {
                DefectSeverityGroup(
                    severity = severity,
                    entries = items
                        .map { DefectEntry(it, categoryNames[it.categoryId].orEmpty()) }
                        .sortedBy { it.categoryName },
                )
            }
        }
        DefectsUiState(
            loading = false,
            groups = groups,
            total = defects.size,
            safety = bySeverity[Severity.SAFETY]?.size ?: 0,
            major = bySeverity[Severity.MAJOR]?.size ?: 0,
            moderate = bySeverity[Severity.MODERATE]?.size ?: 0,
            minor = bySeverity[Severity.MINOR]?.size ?: 0,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DefectsUiState())
}
