package com.hullcheck.data.model

import java.time.Instant
import java.time.LocalDate

/** Domain model for a boat project (a single vessel survey). */
data class Project(
    val id: String,
    val boatName: String,
    val boatType: BoatType,
    val make: String?,
    val model: String?,
    val yearBuilt: Int?,
    val hullId: String?,
    val lengthFeet: Double?,
    val registrationNumber: String?,
    val clientName: String?,
    val surveyorName: String?,
    val location: String?,
    val surveyDate: LocalDate,
    val surveyType: SurveyType,
    val status: ProjectStatus,
    val generalNotes: String?,
    val reportSummary: String?,
    val coverPhotoPath: String?,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    val categories: List<InspectionCategory> = emptyList(),
) {
    val hasLocation: Boolean get() = latitude != null && longitude != null

    val coordinatesText: String?
        get() = if (hasLocation) "%.5f, %.5f".format(latitude, longitude) else null

    /** "Make Model (Year)" style subtitle; null parts are omitted. */
    val makeModelYear: String
        get() = listOfNotNull(make?.takeIf { it.isNotBlank() }, model?.takeIf { it.isNotBlank() }, yearBuilt?.toString())
            .joinToString(" ")
            .trim()

    val allItems: List<ChecklistItem> get() = categories.flatMap { it.items }
}
