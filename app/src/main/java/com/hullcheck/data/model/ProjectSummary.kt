package com.hullcheck.data.model

import kotlin.math.roundToInt

/** Lightweight project + live counts for the home list (no full item tree loaded). */
data class ProjectSummary(
    val project: Project,
    val total: Int,
    val completed: Int,
    val defects: Int,
) {
    val percent: Float get() = if (total == 0) 0f else completed.toFloat() / total
    val percentInt: Int get() = (percent * 100f).roundToInt()
}
