package com.hullcheck.data.model

import kotlin.math.roundToInt

/** Project-wide completion + status breakdown. Percent is fraction of non-PENDING items. */
data class ProjectProgress(
    val total: Int,
    val completed: Int,
    val ok: Int,
    val defects: Int,
    val needsService: Int,
    val na: Int,
    val pending: Int,
) {
    /** 0f..1f fraction of items that have been checked (status != PENDING). */
    val percent: Float get() = if (total == 0) 0f else completed.toFloat() / total

    val percentInt: Int get() = (percent * 100f).roundToInt()

    val isComplete: Boolean get() = total > 0 && completed == total

    val defectTotal: Int get() = defects + needsService

    companion object {
        val EMPTY = ProjectProgress(0, 0, 0, 0, 0, 0, 0)
    }
}

/** Per-category completion summary used by the project-detail grid. */
data class CategoryProgress(
    val total: Int,
    val completed: Int,
    val defects: Int,
    val na: Int,
) {
    val percent: Float get() = if (total == 0) 0f else completed.toFloat() / total
    val hasDefects: Boolean get() = defects > 0

    companion object {
        val EMPTY = CategoryProgress(0, 0, 0, 0)
    }
}
