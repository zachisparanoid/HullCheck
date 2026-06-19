package com.hullcheck.data.model

import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Domain model for a single checklist item plus any attached photos. */
data class ChecklistItem(
    val id: String,
    val categoryId: String,
    val projectId: String,
    val title: String,
    val guidance: String?,
    val status: ItemStatus,
    val condition: ConditionRating?,
    val severity: Severity?,
    val notes: String?,
    val expiryDate: LocalDate? = null,
    val measurementValue: Double? = null,
    val measurementUnit: String? = null,
    val isCustom: Boolean,
    val sortOrder: Int,
    val checkedAt: Instant?,
    val updatedAt: Instant,
    val photos: List<Photo> = emptyList(),
) {
    val hasNotes: Boolean get() = !notes.isNullOrBlank()
    val photoCount: Int get() = photos.size
    val isDefect: Boolean get() = status.isDefectLike

    /** Items within this many days of expiry are flagged "expiring soon". */
    fun expiryStatus(today: LocalDate = LocalDate.now(), soonWindowDays: Long = 60): ExpiryStatus {
        val date = expiryDate ?: return ExpiryStatus.NONE
        return when {
            date.isBefore(today) -> ExpiryStatus.EXPIRED
            ChronoUnit.DAYS.between(today, date) <= soonWindowDays -> ExpiryStatus.SOON
            else -> ExpiryStatus.OK
        }
    }

    val measurementText: String?
        get() = measurementValue?.let { v ->
            val num = if (v % 1.0 == 0.0) v.toInt().toString() else v.toString()
            listOfNotNull(num, measurementUnit?.takeIf { it.isNotBlank() }).joinToString(" ")
        }
}
