package com.hullcheck.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Anchor
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Deck
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DirectionsBoat
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Navigation
import androidx.compose.material.icons.outlined.Plumbing
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Sailing
import androidx.compose.material.icons.outlined.SettingsInputAntenna
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.hullcheck.R
import com.hullcheck.data.model.BoatType
import com.hullcheck.data.model.ConditionRating
import com.hullcheck.data.model.ItemStatus
import com.hullcheck.data.model.LengthUnit
import com.hullcheck.data.model.PhotoQuality
import com.hullcheck.data.model.ProjectStatus
import com.hullcheck.data.model.Severity
import com.hullcheck.data.model.SurveyType
import com.hullcheck.data.model.ThemeMode
import com.hullcheck.ui.theme.StatusOk
import com.hullcheck.ui.theme.StatusPending

// ---------- Enum -> localized labels ----------

@Composable
fun ItemStatus.labelText(): String = stringResource(
    when (this) {
        ItemStatus.PENDING -> R.string.status_pending
        ItemStatus.OK -> R.string.status_ok
        ItemStatus.DEFECT -> R.string.status_defect
        ItemStatus.NOT_APPLICABLE -> R.string.status_na
        ItemStatus.NEEDS_SERVICE -> R.string.status_needs_service
    },
)

@Composable
fun ConditionRating.labelText(): String = stringResource(
    when (this) {
        ConditionRating.EXCELLENT -> R.string.condition_excellent
        ConditionRating.GOOD -> R.string.condition_good
        ConditionRating.FAIR -> R.string.condition_fair
        ConditionRating.POOR -> R.string.condition_poor
        ConditionRating.FAILED -> R.string.condition_failed
    },
)

@Composable
fun Severity.labelText(): String = stringResource(
    when (this) {
        Severity.MINOR -> R.string.severity_minor
        Severity.MODERATE -> R.string.severity_moderate
        Severity.MAJOR -> R.string.severity_major
        Severity.SAFETY -> R.string.severity_safety
    },
)

@Composable
fun BoatType.labelText(): String = stringResource(
    when (this) {
        BoatType.SAIL_MONOHULL -> R.string.boat_type_sail_monohull
        BoatType.SAIL_CATAMARAN -> R.string.boat_type_sail_catamaran
        BoatType.POWER_INBOARD -> R.string.boat_type_power_inboard
        BoatType.POWER_OUTBOARD -> R.string.boat_type_power_outboard
        BoatType.POWER_STERNDRIVE -> R.string.boat_type_power_sterndrive
        BoatType.TRAWLER -> R.string.boat_type_trawler
        BoatType.PWC -> R.string.boat_type_pwc
        BoatType.DINGHY -> R.string.boat_type_dinghy
        BoatType.OTHER -> R.string.boat_type_other
    },
)

@Composable
fun SurveyType.labelText(): String = stringResource(
    when (this) {
        SurveyType.PRE_PURCHASE -> R.string.survey_type_pre_purchase
        SurveyType.INSURANCE -> R.string.survey_type_insurance
        SurveyType.DAMAGE -> R.string.survey_type_damage
        SurveyType.APPRAISAL -> R.string.survey_type_appraisal
        SurveyType.COMPLIANCE -> R.string.survey_type_compliance
    },
)

@Composable
fun ProjectStatus.labelText(): String = stringResource(
    when (this) {
        ProjectStatus.DRAFT -> R.string.project_status_draft
        ProjectStatus.IN_PROGRESS -> R.string.project_status_in_progress
        ProjectStatus.COMPLETED -> R.string.project_status_completed
        ProjectStatus.ARCHIVED -> R.string.project_status_archived
    },
)

@Composable
fun ThemeMode.labelText(): String = stringResource(
    when (this) {
        ThemeMode.SYSTEM -> R.string.settings_theme_system
        ThemeMode.DARK -> R.string.settings_theme_dark
        ThemeMode.LIGHT -> R.string.settings_theme_light
    },
)

@Composable
fun LengthUnit.labelText(): String = stringResource(
    when (this) {
        LengthUnit.FEET -> R.string.settings_units_feet
        LengthUnit.METERS -> R.string.settings_units_meters
    },
)

@Composable
fun PhotoQuality.labelText(): String = stringResource(
    when (this) {
        PhotoQuality.STANDARD -> R.string.settings_photo_quality_standard
        PhotoQuality.HIGH -> R.string.settings_photo_quality_high
    },
)

// ---------- Icons ----------

fun categoryIcon(iconKey: String): ImageVector = when (iconKey) {
    "documentation" -> Icons.Outlined.Description
    "hull" -> Icons.Outlined.Anchor
    "deck" -> Icons.Outlined.Deck
    "engine" -> Icons.Outlined.Build
    "fuel" -> Icons.Outlined.LocalGasStation
    "steering" -> Icons.Outlined.Navigation
    "electrical" -> Icons.Outlined.Bolt
    "plumbing" -> Icons.Outlined.Plumbing
    "rigging" -> Icons.Outlined.Sailing
    "safety" -> Icons.Outlined.HealthAndSafety
    "galley" -> Icons.Outlined.Restaurant
    "electronics" -> Icons.Outlined.SettingsInputAntenna
    "trailer" -> Icons.Outlined.DirectionsCar
    else -> Icons.Outlined.Checklist
}

fun boatTypeIcon(type: BoatType): ImageVector = when (type) {
    BoatType.SAIL_MONOHULL, BoatType.SAIL_CATAMARAN -> Icons.Outlined.Sailing
    else -> Icons.Outlined.DirectionsBoat
}

/** Badge color for a project status (used on list cards). */
fun projectStatusColor(status: ProjectStatus): Color = when (status) {
    ProjectStatus.DRAFT -> StatusPending
    ProjectStatus.IN_PROGRESS -> Color(0xFF13C2C2)
    ProjectStatus.COMPLETED -> StatusOk
    ProjectStatus.ARCHIVED -> Color(0xFF6B7A87)
}
