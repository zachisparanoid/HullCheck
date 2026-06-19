package com.hullcheck

import com.hullcheck.data.model.ProjectProgress
import com.hullcheck.data.model.Severity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressTest {

    @Test
    fun `percent is fraction of checked items`() {
        val progress = ProjectProgress(total = 8, completed = 2, ok = 1, defects = 1, needsService = 0, na = 0, pending = 6)
        assertEquals(0.25f, progress.percent, 0.0001f)
        assertEquals(25, progress.percentInt)
        assertFalse(progress.isComplete)
        assertEquals(1, progress.defectTotal)
    }

    @Test
    fun `empty project is zero percent and not complete`() {
        val progress = ProjectProgress.EMPTY
        assertEquals(0f, progress.percent, 0.0001f)
        assertFalse(progress.isComplete)
    }

    @Test
    fun `all checked is complete`() {
        val progress = ProjectProgress(total = 5, completed = 5, ok = 4, defects = 1, needsService = 0, na = 0, pending = 0)
        assertTrue(progress.isComplete)
        assertEquals(100, progress.percentInt)
    }

    @Test
    fun `severity rank orders safety highest`() {
        val ordered = Severity.entries.sortedByDescending { it.rank }
        assertEquals(listOf(Severity.SAFETY, Severity.MAJOR, Severity.MODERATE, Severity.MINOR), ordered)
    }
}
