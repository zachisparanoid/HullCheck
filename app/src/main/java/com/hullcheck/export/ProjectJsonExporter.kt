package com.hullcheck.export

import android.util.Base64
import androidx.room.withTransaction
import com.hullcheck.data.db.HullCheckDatabase
import com.hullcheck.data.db.dao.CategoryDao
import com.hullcheck.data.db.dao.ItemDao
import com.hullcheck.data.db.dao.PhotoDao
import com.hullcheck.data.db.dao.ProjectDao
import com.hullcheck.data.db.entity.CategoryEntity
import com.hullcheck.data.db.entity.ItemEntity
import com.hullcheck.data.db.entity.PhotoEntity
import com.hullcheck.data.db.entity.ProjectEntity
import com.hullcheck.di.IoDispatcher
import com.hullcheck.export.dto.CategoryDto
import com.hullcheck.export.dto.ImportSummary
import com.hullcheck.export.dto.ItemDto
import com.hullcheck.export.dto.PhotoDto
import com.hullcheck.export.dto.ProjectBundle
import com.hullcheck.export.dto.ProjectDto
import com.hullcheck.util.DateFormatters
import com.hullcheck.util.FileStorage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.io.InputStream
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectJsonExporter @Inject constructor(
    private val db: HullCheckDatabase,
    private val projectDao: ProjectDao,
    private val categoryDao: CategoryDao,
    private val itemDao: ItemDao,
    private val photoDao: PhotoDao,
    private val fileStorage: FileStorage,
    private val json: Json,
    @IoDispatcher private val io: CoroutineDispatcher,
) {

    // ---------------- Export ----------------

    suspend fun exportToFile(projectId: String): File = withContext(io) {
        val tree = projectDao.getProjectTree(projectId) ?: error("Project not found")
        val project = tree.project
        val projectPhotos = photoDao.getByProject(projectId).filter { it.itemId == null }

        val itemPhotoEntities = tree.categories.flatMap { it.items }.flatMap { it.photos }
        val coverRef = project.coverPhotoPath?.let { cover ->
            (itemPhotoEntities + projectPhotos).firstOrNull { it.filePath == cover }?.id
        }

        val categories = tree.categories.sortedBy { it.category.sortOrder }.map { cw ->
            CategoryDto(
                name = cw.category.name,
                iconKey = cw.category.iconKey,
                sortOrder = cw.category.sortOrder,
                isEngineRelated = cw.category.isEngineRelated,
                isSkippable = cw.category.isSkippable,
                items = cw.items.sortedBy { it.item.sortOrder }.map { iw ->
                    ItemDto(
                        title = iw.item.title,
                        guidance = iw.item.guidance,
                        status = iw.item.status,
                        condition = iw.item.condition,
                        severity = iw.item.severity,
                        notes = iw.item.notes,
                        expiryDateEpochDay = iw.item.expiryDate?.toEpochDay(),
                        measurementValue = iw.item.measurementValue,
                        measurementUnit = iw.item.measurementUnit,
                        isCustom = iw.item.isCustom,
                        sortOrder = iw.item.sortOrder,
                        checkedAtEpochMilli = iw.item.checkedAt?.toEpochMilli(),
                        photos = iw.photos.sortedBy { it.createdAt }.map { it.toDto() },
                    )
                },
            )
        }

        val dto = ProjectDto(
            boatName = project.boatName,
            boatType = project.boatType,
            make = project.make,
            model = project.model,
            yearBuilt = project.yearBuilt,
            hullId = project.hullId,
            lengthFeet = project.lengthFeet,
            registrationNumber = project.registrationNumber,
            clientName = project.clientName,
            surveyorName = project.surveyorName,
            location = project.location,
            surveyDateEpochDay = project.surveyDate.toEpochDay(),
            surveyType = project.surveyType,
            status = project.status,
            generalNotes = project.generalNotes,
            reportSummary = project.reportSummary,
            latitude = project.latitude,
            longitude = project.longitude,
            coverPhotoRef = coverRef,
            createdAtEpochMilli = project.createdAt.toEpochMilli(),
            updatedAtEpochMilli = project.updatedAt.toEpochMilli(),
            categories = categories,
            projectPhotos = projectPhotos.map { it.toDto() },
        )

        val bundle = ProjectBundle(exportedAtEpochMilli = Instant.now().toEpochMilli(), project = dto)
        val fileName = "${project.boatName.toFileSlug()}_${DateFormatters.fileTimestamp(Instant.now())}.json"
        val out = File(fileStorage.exportsDir, fileName)
        out.writeText(json.encodeToString(ProjectBundle.serializer(), bundle))
        out
    }

    private fun PhotoEntity.toDto(): PhotoDto {
        val file = File(filePath)
        val data = if (file.exists()) {
            runCatching { Base64.encodeToString(file.readBytes(), Base64.NO_WRAP) }.getOrNull()
        } else {
            null
        }
        return PhotoDto(
            localId = id,
            fileName = file.name,
            caption = caption,
            createdAtEpochMilli = createdAt.toEpochMilli(),
            dataBase64 = data,
        )
    }

    // ---------------- Import ----------------

    suspend fun import(input: InputStream): ImportSummary = withContext(io) {
        val raw = input.bufferedReader().use { it.readText() }
        val bundle = runCatching { json.decodeFromString(ProjectBundle.serializer(), raw) }
            .getOrElse { throw IllegalArgumentException("Not a valid HullCheck export") }
        require(bundle.format == ProjectBundle.FORMAT) { "Unrecognized file format" }
        require(bundle.version <= ProjectBundle.VERSION) { "Export was made with a newer app version" }

        val dto = bundle.project
        val now = Instant.now()
        val newProjectId = UUID.randomUUID().toString()

        val categoryEntities = mutableListOf<CategoryEntity>()
        val itemEntities = mutableListOf<ItemEntity>()
        val photoEntities = mutableListOf<PhotoEntity>()
        val writtenFiles = mutableListOf<File>()
        val pathByLocalId = mutableMapOf<String, String>()
        var restored = 0
        var missing = 0

        fun materialize(photo: PhotoDto, itemId: String?) {
            val bytes = photo.dataBase64?.let { runCatching { Base64.decode(it, Base64.NO_WRAP) }.getOrNull() }
            if (bytes == null) {
                missing++
                return
            }
            val dest = fileStorage.newPhotoFile(newProjectId)
            dest.writeBytes(bytes)
            writtenFiles += dest
            pathByLocalId[photo.localId] = dest.absolutePath
            photoEntities += PhotoEntity(
                id = UUID.randomUUID().toString(),
                itemId = itemId,
                projectId = newProjectId,
                filePath = dest.absolutePath,
                caption = photo.caption,
                createdAt = Instant.ofEpochMilli(photo.createdAtEpochMilli),
            )
            restored++
        }

        try {
            dto.categories.forEachIndexed { ci, cat ->
                val categoryId = UUID.randomUUID().toString()
                categoryEntities += CategoryEntity(
                    id = categoryId,
                    projectId = newProjectId,
                    name = cat.name,
                    iconKey = cat.iconKey,
                    sortOrder = cat.sortOrder.takeIf { it >= 0 } ?: ci,
                    isEngineRelated = cat.isEngineRelated,
                    isSkippable = cat.isSkippable,
                )
                cat.items.forEachIndexed { ii, item ->
                    val itemId = UUID.randomUUID().toString()
                    itemEntities += ItemEntity(
                        id = itemId,
                        categoryId = categoryId,
                        projectId = newProjectId,
                        title = item.title,
                        guidance = item.guidance,
                        status = item.status,
                        condition = item.condition,
                        severity = item.severity,
                        notes = item.notes,
                        expiryDate = item.expiryDateEpochDay?.let { LocalDate.ofEpochDay(it) },
                        measurementValue = item.measurementValue,
                        measurementUnit = item.measurementUnit,
                        isCustom = item.isCustom,
                        sortOrder = item.sortOrder.takeIf { it >= 0 } ?: ii,
                        checkedAt = item.checkedAtEpochMilli?.let { Instant.ofEpochMilli(it) },
                        updatedAt = now,
                    )
                    item.photos.forEach { materialize(it, itemId) }
                }
            }
            dto.projectPhotos.forEach { materialize(it, null) }

            val projectEntity = ProjectEntity(
                id = newProjectId,
                boatName = dto.boatName,
                boatType = dto.boatType,
                make = dto.make,
                model = dto.model,
                yearBuilt = dto.yearBuilt,
                hullId = dto.hullId,
                lengthFeet = dto.lengthFeet,
                registrationNumber = dto.registrationNumber,
                clientName = dto.clientName,
                surveyorName = dto.surveyorName,
                location = dto.location,
                surveyDate = LocalDate.ofEpochDay(dto.surveyDateEpochDay),
                surveyType = dto.surveyType,
                status = dto.status,
                generalNotes = dto.generalNotes,
                reportSummary = dto.reportSummary,
                latitude = dto.latitude,
                longitude = dto.longitude,
                coverPhotoPath = dto.coverPhotoRef?.let { pathByLocalId[it] },
                createdAt = Instant.ofEpochMilli(dto.createdAtEpochMilli),
                updatedAt = now,
            )

            db.withTransaction {
                projectDao.insert(projectEntity)
                categoryDao.insertAll(categoryEntities)
                itemDao.insertAll(itemEntities)
                if (photoEntities.isNotEmpty()) photoDao.insertAll(photoEntities)
            }
        } catch (e: Exception) {
            // Roll back the files we wrote so a failed import leaves nothing behind.
            writtenFiles.forEach { runCatching { it.delete() } }
            throw e
        }

        ImportSummary(
            projectId = newProjectId,
            boatName = dto.boatName,
            photosRestored = restored,
            photosMissing = missing,
        )
    }

    private fun String.toFileSlug(): String =
        trim().lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').ifBlank { "project" }
}
