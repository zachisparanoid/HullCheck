package com.hullcheck.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Centralized java.time formatting (desugared on API 26). */
object DateFormatters {

    private val displayDate: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())

    private val displayDateTime: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.getDefault())

    private val fileStamp: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.US)

    fun formatDate(date: LocalDate): String = date.format(displayDate)

    fun formatDate(instant: Instant): String =
        instant.atZone(ZoneId.systemDefault()).toLocalDate().format(displayDate)

    fun formatDateTime(instant: Instant): String =
        instant.atZone(ZoneId.systemDefault()).format(displayDateTime)

    fun fileTimestamp(instant: Instant): String =
        instant.atZone(ZoneId.systemDefault()).format(fileStamp)
}
