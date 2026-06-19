package com.hullcheck.ui.checklist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hullcheck.data.model.ChecklistItem
import com.hullcheck.data.model.InspectionCategory
import com.hullcheck.data.model.ItemStatus
import com.hullcheck.data.repository.ChecklistRepository
import com.hullcheck.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ChecklistFilter { ALL, PENDING, DEFECTS, NA }

data class ChecklistUiState(
    val loading: Boolean = true,
    val category: InspectionCategory? = null,
    val items: List<ChecklistItem> = emptyList(),
    val total: Int = 0,
    val checked: Int = 0,
    val filter: ChecklistFilter = ChecklistFilter.ALL,
)

@HiltViewModel
class ChecklistViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val checklistRepository: ChecklistRepository,
) : ViewModel() {

    val projectId: String = checkNotNull(savedStateHandle[Routes.ARG_PROJECT_ID])
    private val categoryId: String = checkNotNull(savedStateHandle[Routes.ARG_CATEGORY_ID])

    private val filter = MutableStateFlow(ChecklistFilter.ALL)

    val uiState: StateFlow<ChecklistUiState> = combine(
        checklistRepository.observeCategory(categoryId),
        checklistRepository.observeItems(categoryId),
        filter,
    ) { category, items, activeFilter ->
        val filtered = when (activeFilter) {
            ChecklistFilter.ALL -> items
            ChecklistFilter.PENDING -> items.filter { it.status == ItemStatus.PENDING }
            ChecklistFilter.DEFECTS -> items.filter { it.status.isDefectLike }
            ChecklistFilter.NA -> items.filter { it.status == ItemStatus.NOT_APPLICABLE }
        }
        ChecklistUiState(
            loading = false,
            category = category,
            items = filtered,
            total = items.size,
            checked = items.count { it.status.isChecked },
            filter = activeFilter,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ChecklistUiState())

    fun setFilter(value: ChecklistFilter) { filter.value = value }

    fun setStatus(item: ChecklistItem, status: ItemStatus) = viewModelScope.launch {
        checklistRepository.setStatus(item.id, projectId, status)
    }

    fun resetItem(item: ChecklistItem) = viewModelScope.launch {
        checklistRepository.resetItem(item.id, projectId)
    }

    fun deleteItem(item: ChecklistItem) = viewModelScope.launch {
        checklistRepository.deleteCustomItem(item)
    }

    fun markAllNa() = viewModelScope.launch {
        checklistRepository.markAllNa(categoryId, projectId)
    }

    fun addCustomItem(title: String, guidance: String?) = viewModelScope.launch {
        if (title.isNotBlank()) checklistRepository.addCustomItem(categoryId, projectId, title, guidance)
    }
}
