package com.hullcheck.data.model

/** Domain model for a per-project inspection category and its items. */
data class InspectionCategory(
    val id: String,
    val projectId: String,
    val name: String,
    val iconKey: String,
    val sortOrder: Int,
    val isEngineRelated: Boolean,
    val isSkippable: Boolean,
    val items: List<ChecklistItem> = emptyList(),
) {
    val progress: CategoryProgress
        get() = CategoryProgress(
            total = items.size,
            completed = items.count { it.status.isChecked },
            defects = items.count { it.status.isDefectLike },
            na = items.count { it.status == ItemStatus.NOT_APPLICABLE },
        )
}
