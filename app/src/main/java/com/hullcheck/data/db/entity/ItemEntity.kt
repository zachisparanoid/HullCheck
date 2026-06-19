package com.hullcheck.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.hullcheck.data.model.ConditionRating
import com.hullcheck.data.model.ItemStatus
import com.hullcheck.data.model.Severity
import java.time.Instant
import java.time.LocalDate

@Entity(
    tableName = "items",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("categoryId"), Index("projectId")],
)
data class ItemEntity(
    @PrimaryKey val id: String,
    val categoryId: String,
    /** Denormalized for fast project-wide queries (progress, defects). */
    val projectId: String,
    val title: String,
    val guidance: String?,
    val status: ItemStatus,
    val condition: ConditionRating?,
    val severity: Severity?,
    val notes: String?,
    /** Optional expiry date for dated gear (flares, extinguishers, EPIRB, life raft service). */
    val expiryDate: LocalDate? = null,
    /** Optional structured reading (e.g. moisture %, hull thickness, engine hours). */
    val measurementValue: Double? = null,
    val measurementUnit: String? = null,
    val isCustom: Boolean,
    val sortOrder: Int,
    val checkedAt: Instant?,
    val updatedAt: Instant,
)
