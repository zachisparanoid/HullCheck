package com.hullcheck.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("projectId")],
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val name: String,
    /** Maps to a Material icon in ui (see CategoryIcons). */
    val iconKey: String,
    val sortOrder: Int,
    val isEngineRelated: Boolean,
    /** Skippable categories (e.g. Trailer, Rigging on a power boat) support "Mark all N/A". */
    val isSkippable: Boolean,
)
