package com.hullcheck.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.hullcheck.data.model.BoatType
import com.hullcheck.data.model.ProjectStatus
import com.hullcheck.data.model.SurveyType
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
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
    /** Dedicated executive-summary / overall-condition statement shown on the report. */
    val reportSummary: String?,
    val coverPhotoPath: String?,
    /** Optional GPS coordinates of the survey location (captured offline). */
    val latitude: Double? = null,
    val longitude: Double? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)
