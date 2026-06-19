package com.hullcheck.data.db.dao

import androidx.room.Embedded
import com.hullcheck.data.db.entity.ProjectEntity

/** One row per project for the list screen: the project plus its live aggregate counts. */
data class ProjectListRow(
    @Embedded val project: ProjectEntity,
    val total: Int,
    val completed: Int,
    val defects: Int,
)

/** Full status breakdown for a single project (project detail + report). */
data class ProgressRow(
    val total: Int,
    val completed: Int,
    val ok: Int,
    val defects: Int,
    val needsService: Int,
    val na: Int,
    val pending: Int,
)

/** Per-category aggregate for the project-detail category grid. */
data class CategoryProgressRow(
    val categoryId: String,
    val total: Int,
    val completed: Int,
    val defects: Int,
    val na: Int,
)
