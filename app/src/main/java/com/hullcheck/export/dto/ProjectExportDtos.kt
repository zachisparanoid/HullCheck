package com.hullcheck.export.dto

import com.hullcheck.data.model.BoatType
import com.hullcheck.data.model.ConditionRating
import com.hullcheck.data.model.ItemStatus
import com.hullcheck.data.model.ProjectStatus
import com.hullcheck.data.model.Severity
import com.hullcheck.data.model.SurveyType
import kotlinx.serialization.Serializable

/** Top-level export envelope. [format]/[version] gate import validation. */
@Serializable
data class ProjectBundle(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val exportedAtEpochMilli: Long,
    val project: ProjectDto,
) {
    companion object {
        const val FORMAT = "hullcheck-project"
        const val VERSION = 1
    }
}

@Serializable
data class ProjectDto(
    val boatName: String,
    val boatType: BoatType,
    val make: String? = null,
    val model: String? = null,
    val yearBuilt: Int? = null,
    val hullId: String? = null,
    val lengthFeet: Double? = null,
    val registrationNumber: String? = null,
    val clientName: String? = null,
    val surveyorName: String? = null,
    val location: String? = null,
    val surveyDateEpochDay: Long,
    val surveyType: SurveyType,
    val status: ProjectStatus,
    val generalNotes: String? = null,
    val reportSummary: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    /** localId of the photo (anywhere in the bundle) used as the cover. */
    val coverPhotoRef: String? = null,
    val createdAtEpochMilli: Long,
    val updatedAtEpochMilli: Long,
    val categories: List<CategoryDto>,
    val projectPhotos: List<PhotoDto> = emptyList(),
)

@Serializable
data class CategoryDto(
    val name: String,
    val iconKey: String,
    val sortOrder: Int,
    val isEngineRelated: Boolean,
    val isSkippable: Boolean,
    val items: List<ItemDto>,
)

@Serializable
data class ItemDto(
    val title: String,
    val guidance: String? = null,
    val status: ItemStatus,
    val condition: ConditionRating? = null,
    val severity: Severity? = null,
    val notes: String? = null,
    val expiryDateEpochDay: Long? = null,
    val measurementValue: Double? = null,
    val measurementUnit: String? = null,
    val isCustom: Boolean = false,
    val sortOrder: Int,
    val checkedAtEpochMilli: Long? = null,
    val photos: List<PhotoDto> = emptyList(),
)

@Serializable
data class PhotoDto(
    val localId: String,
    val fileName: String,
    val caption: String? = null,
    val createdAtEpochMilli: Long,
    /** Base64 (NO_WRAP) JPEG bytes; null when the source file was missing at export. */
    val dataBase64: String? = null,
)

/** Outcome of an import (counts let the UI report partially-restored photos). */
data class ImportSummary(
    val projectId: String,
    val boatName: String,
    val photosRestored: Int,
    val photosMissing: Int,
)
