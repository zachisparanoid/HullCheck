package com.hullcheck.data.repository

import com.hullcheck.data.db.dao.ProjectDao
import com.hullcheck.data.model.ItemStatus
import com.hullcheck.data.model.Project
import com.hullcheck.data.model.ProjectProgress
import com.hullcheck.di.IoDispatcher
import com.hullcheck.export.ProjectJsonExporter
import com.hullcheck.export.dto.ImportSummary
import com.hullcheck.pdf.ReportBranding
import com.hullcheck.pdf.ReportPdfGenerator
import com.hullcheck.util.DateFormatters
import com.hullcheck.util.FileStorage
import com.hullcheck.util.OpResult
import com.hullcheck.util.runCatchingResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReportRepository @Inject constructor(
    private val projectDao: ProjectDao,
    private val pdfGenerator: ReportPdfGenerator,
    private val jsonExporter: ProjectJsonExporter,
    private val fileStorage: FileStorage,
    private val settingsRepository: SettingsRepository,
    @IoDispatcher private val io: CoroutineDispatcher,
) {

    suspend fun generatePdf(projectId: String): OpResult<File> = withContext(io) {
        runCatchingResult {
            val tree = projectDao.getProjectTree(projectId) ?: error("Project not found")
            val project = tree.toDomain()
            val progress = computeProgress(project)
            val settings = settingsRepository.current()
            val branding = ReportBranding(
                firmName = settings.firmName.ifBlank { null },
                licenseNumber = settings.licenseNumber.ifBlank { null },
                logoPath = settings.logoPath,
                signaturePath = settings.signaturePath,
                disclaimer = settings.reportDisclaimer.ifBlank { null },
                surveyorName = settings.surveyorName.ifBlank { null },
            )
            val fileName = "${project.boatName.toFileSlug()}_${DateFormatters.fileTimestamp(Instant.now())}.pdf"
            pdfGenerator.generate(project, progress, branding, File(fileStorage.exportsDir, fileName))
        }
    }

    suspend fun exportJson(projectId: String): OpResult<File> = withContext(io) {
        runCatchingResult { jsonExporter.exportToFile(projectId) }
    }

    suspend fun importJson(input: InputStream): OpResult<ImportSummary> = withContext(io) {
        runCatchingResult { input.use { jsonExporter.import(it) } }
    }

    private fun computeProgress(project: Project): ProjectProgress {
        val items = project.allItems
        return ProjectProgress(
            total = items.size,
            completed = items.count { it.status.isChecked },
            ok = items.count { it.status == ItemStatus.OK },
            defects = items.count { it.status == ItemStatus.DEFECT },
            needsService = items.count { it.status == ItemStatus.NEEDS_SERVICE },
            na = items.count { it.status == ItemStatus.NOT_APPLICABLE },
            pending = items.count { it.status == ItemStatus.PENDING },
        )
    }

    private fun String.toFileSlug(): String =
        trim().lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "report" }
}
