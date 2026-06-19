package com.hullcheck.data.model

import kotlinx.serialization.Serializable

/**
 * Core domain enums. They are shared across the Room layer (persisted by [name] via
 * TypeConverters), the domain models, and the JSON export DTOs (kotlinx.serialization
 * serializes enums by their constant name by default, so the wire format is stable).
 */

@Serializable
enum class BoatType {
    SAIL_MONOHULL,
    SAIL_CATAMARAN,
    POWER_INBOARD,
    POWER_OUTBOARD,
    POWER_STERNDRIVE,
    TRAWLER,
    PWC,
    DINGHY,
    OTHER,
}

@Serializable
enum class SurveyType {
    PRE_PURCHASE,
    INSURANCE,
    DAMAGE,
    APPRAISAL,
    COMPLIANCE,
}

@Serializable
enum class ProjectStatus {
    DRAFT,
    IN_PROGRESS,
    COMPLETED,
    ARCHIVED,
}

@Serializable
enum class ItemStatus {
    PENDING,
    OK,
    DEFECT,
    NOT_APPLICABLE,
    NEEDS_SERVICE;

    /** A status counts toward "checked" progress once it leaves PENDING. */
    val isChecked: Boolean get() = this != PENDING

    val isDefectLike: Boolean get() = this == DEFECT || this == NEEDS_SERVICE
}

@Serializable
enum class ConditionRating {
    EXCELLENT,
    GOOD,
    FAIR,
    POOR,
    FAILED,
}

@Serializable
enum class Severity {
    MINOR,
    MODERATE,
    MAJOR,
    SAFETY;

    /** Higher rank = more urgent; drives sort order in the defects view and report. */
    val rank: Int get() = when (this) {
        SAFETY -> 4
        MAJOR -> 3
        MODERATE -> 2
        MINOR -> 1
    }
}

// ----- App-level (non-persisted-by-Room) preference enums -----

enum class ThemeMode { SYSTEM, DARK, LIGHT }

enum class LengthUnit { FEET, METERS }

enum class PhotoQuality(val maxLongEdgePx: Int, val jpegQuality: Int) {
    STANDARD(maxLongEdgePx = 1600, jpegQuality = 80),
    HIGH(maxLongEdgePx = 2600, jpegQuality = 90),
}

enum class ProjectSort { DATE, NAME, PROGRESS, STATUS }

enum class ProjectFilter { ALL, IN_PROGRESS, COMPLETED, ARCHIVED }

/** Derived state for a dated item (flares, extinguishers, EPIRB, life-raft service…). */
enum class ExpiryStatus { NONE, OK, SOON, EXPIRED }
