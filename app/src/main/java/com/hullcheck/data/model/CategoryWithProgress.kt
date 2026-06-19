package com.hullcheck.data.model

/** A category (metadata only) paired with its aggregate progress for the detail grid. */
data class CategoryWithProgress(
    val category: InspectionCategory,
    val progress: CategoryProgress,
)
