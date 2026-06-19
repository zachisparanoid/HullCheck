package com.hullcheck.ui.theme

import androidx.compose.ui.graphics.Color
import com.hullcheck.data.model.ItemStatus
import com.hullcheck.data.model.Severity

// ---------- Brand / dark palette ----------
val HullBackground = Color(0xFF0B1117)
val HullSurface = Color(0xFF121A22)
val HullSurfaceElevated = Color(0xFF18222C)
val HullSurfaceHigh = Color(0xFF1E2A35)
val HullSurfaceHighest = Color(0xFF23303B)
val HullTeal = Color(0xFF13C2C2)
val HullTealContainer = Color(0xFF0E4F50)
val HullTealOnContainer = Color(0xFFA7F3F0)
val HullAmber = Color(0xFFF2A93B)
val HullTextPrimary = Color(0xFFE6EDF3)
val HullTextSecondary = Color(0xFF9FB0BF)
val HullDisabled = Color(0xFF5C6B78)
val HullOutline = Color(0xFF2B3A47)
val HullOutlineVariant = Color(0xFF23303B)

// ---------- Light palette ----------
val HullLightBackground = Color(0xFFF5F7FA)
val HullLightSurface = Color(0xFFFFFFFF)
val HullLightSurfaceVariant = Color(0xFFE6ECF1)
val HullLightPrimary = Color(0xFF0E8F8F)
val HullLightPrimaryContainer = Color(0xFFB8EEEC)
val HullLightOnPrimaryContainer = Color(0xFF073B3B)
val HullLightTextPrimary = Color(0xFF0B1117)
val HullLightTextSecondary = Color(0xFF4A5A66)
val HullLightOutline = Color(0xFFC3CDD6)
val HullLightOutlineVariant = Color(0xFFDCE3E9)

// ---------- Status colors (legible on both themes) ----------
val StatusOk = Color(0xFF2FBF71)
val StatusPending = Color(0xFF7A8896)
val StatusNa = Color(0xFF6B7A87)
val StatusNeedsService = Color(0xFFF2A93B)
val StatusDefect = Color(0xFFFF5D5D)

fun statusColor(status: ItemStatus): Color = when (status) {
    ItemStatus.PENDING -> StatusPending
    ItemStatus.OK -> StatusOk
    ItemStatus.DEFECT -> StatusDefect
    ItemStatus.NOT_APPLICABLE -> StatusNa
    ItemStatus.NEEDS_SERVICE -> StatusNeedsService
}

// ---------- Severity colors ----------
val SeverityMinor = Color(0xFFF2C94C)
val SeverityModerate = Color(0xFFF2994A)
val SeverityMajor = Color(0xFFFF5D5D)
val SeveritySafety = Color(0xFFFF3B7B)

fun severityColor(severity: Severity): Color = when (severity) {
    Severity.MINOR -> SeverityMinor
    Severity.MODERATE -> SeverityModerate
    Severity.MAJOR -> SeverityMajor
    Severity.SAFETY -> SeveritySafety
}
