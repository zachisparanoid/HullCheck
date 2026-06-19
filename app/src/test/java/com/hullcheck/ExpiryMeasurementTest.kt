package com.hullcheck

import com.hullcheck.data.model.ChecklistItem
import com.hullcheck.data.model.ExpiryStatus
import com.hullcheck.data.model.ItemStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class ExpiryMeasurementTest {

    private fun item(
        expiry: LocalDate? = null,
        value: Double? = null,
        unit: String? = null,
    ) = ChecklistItem(
        id = "i",
        categoryId = "c",
        projectId = "p",
        title = "t",
        guidance = null,
        status = ItemStatus.OK,
        condition = null,
        severity = null,
        notes = null,
        expiryDate = expiry,
        measurementValue = value,
        measurementUnit = unit,
        isCustom = false,
        sortOrder = 0,
        checkedAt = null,
        updatedAt = Instant.EPOCH,
    )

    private val today = LocalDate.of(2026, 6, 19)

    @Test
    fun `past date is expired`() {
        assertEquals(ExpiryStatus.EXPIRED, item(today.minusDays(1)).expiryStatus(today))
    }

    @Test
    fun `date within window is soon`() {
        assertEquals(ExpiryStatus.SOON, item(today.plusDays(10)).expiryStatus(today, soonWindowDays = 60))
    }

    @Test
    fun `far date is ok`() {
        assertEquals(ExpiryStatus.OK, item(today.plusDays(200)).expiryStatus(today, soonWindowDays = 60))
    }

    @Test
    fun `no date is none`() {
        assertEquals(ExpiryStatus.NONE, item(null).expiryStatus(today))
    }

    @Test
    fun `measurement formats integer and decimal`() {
        assertEquals("12 mm", item(value = 12.0, unit = "mm").measurementText)
        assertEquals("12.5 %", item(value = 12.5, unit = "%").measurementText)
        assertNull(item().measurementText)
    }
}
