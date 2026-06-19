package com.hullcheck.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "photos",
    foreignKeys = [
        ForeignKey(
            entity = ItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("itemId"), Index("projectId")],
)
data class PhotoEntity(
    @PrimaryKey val id: String,
    /** Null for project-level photos (e.g. the cover); otherwise the owning item. */
    val itemId: String?,
    val projectId: String,
    val filePath: String,
    val caption: String?,
    val createdAt: Instant,
)
