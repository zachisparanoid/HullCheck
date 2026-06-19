package com.hullcheck.data.repository

import androidx.room.withTransaction
import com.hullcheck.data.db.HullCheckDatabase
import com.hullcheck.data.db.dao.CategoryDao
import com.hullcheck.data.db.dao.ItemDao
import com.hullcheck.data.db.dao.PhotoDao
import com.hullcheck.data.db.dao.ProjectDao
import com.hullcheck.data.db.dao.TemplateDao
import com.hullcheck.data.db.entity.CategoryEntity
import com.hullcheck.data.db.entity.ItemEntity
import com.hullcheck.data.db.entity.PhotoEntity
import com.hullcheck.data.model.ItemStatus
import com.hullcheck.data.model.Project
import com.hullcheck.data.model.ProjectProgress
import com.hullcheck.data.model.ProjectStatus
import com.hullcheck.data.model.ProjectSummary
import com.hullcheck.data.seed.ChecklistSeed
import com.hullcheck.di.IoDispatcher
import com.hullcheck.util.FileStorage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepository @Inject constructor(
    private val db: HullCheckDatabase,
    private val projectDao: ProjectDao,
    private val categoryDao: CategoryDao,
    private val itemDao: ItemDao,
    private val photoDao: PhotoDao,
    private val templateDao: TemplateDao,
    private val checklistSeed: ChecklistSeed,
    private val fileStorage: FileStorage,
    @IoDispatcher private val io: CoroutineDispatcher,
) {

    fun observeProjectSummaries(): Flow<List<ProjectSummary>> =
        projectDao.observeProjectListRows().map { rows ->
            rows.map { ProjectSummary(it.project.toDomain(), it.total, it.completed, it.defects) }
        }

    fun observeProject(projectId: String): Flow<Project?> =
        projectDao.observeProjectTree(projectId).map { it?.toDomain() }

    fun observeProjectMeta(projectId: String): Flow<Project?> =
        projectDao.observeById(projectId).map { it?.toDomain() }

    fun observeProgress(projectId: String): Flow<ProjectProgress> =
        projectDao.observeProgress(projectId).map { it.toProgress() }

    suspend fun getProjectTree(projectId: String): Project? = withContext(io) {
        projectDao.getProjectTree(projectId)?.toDomain()
    }

    suspend fun getProjectMeta(projectId: String): Project? = withContext(io) {
        projectDao.getById(projectId)?.toDomain()
    }

    /** Insert a new project and instantiate the chosen (or default) checklist for it (atomic). */
    suspend fun createProject(project: Project, templateId: String? = null): String = withContext(io) {
        val now = Instant.now()
        val entity = project.copy(updatedAt = now).toEntity()
        val template = templateId
            ?.let { templateDao.getById(it) }
            ?.let { runCatching { checklistSeed.parse(it.json) }.getOrNull() }
            ?: checklistSeed.loadDefaultTemplate()
        val instantiated = checklistSeed.instantiate(template, project.id, now)
        db.withTransaction {
            projectDao.insert(entity)
            categoryDao.insertAll(instantiated.categories)
            itemDao.insertAll(instantiated.items)
        }
        project.id
    }

    suspend fun updateProject(project: Project) = withContext(io) {
        projectDao.update(project.copy(updatedAt = Instant.now()).toEntity())
    }

    suspend fun deleteProject(projectId: String) = withContext(io) {
        val paths = photoDao.getFilePathsByProject(projectId)
        projectDao.deleteById(projectId) // cascades categories, items, photos rows
        fileStorage.deleteFiles(paths)
        fileStorage.deleteProjectPhotoDir(projectId)
    }

    suspend fun setCoverPhoto(projectId: String, path: String?) = withContext(io) {
        projectDao.updateCoverPhoto(projectId, path, Instant.now())
    }

    suspend fun setReportSummary(projectId: String, summary: String?) = withContext(io) {
        projectDao.updateReportSummary(projectId, summary, Instant.now())
    }

    suspend fun setStatus(projectId: String, status: ProjectStatus) = withContext(io) {
        projectDao.updateStatus(projectId, status, Instant.now())
    }

    suspend fun markCompleted(projectId: String) = setStatus(projectId, ProjectStatus.COMPLETED)

    suspend fun archive(projectId: String) = setStatus(projectId, ProjectStatus.ARCHIVED)

    suspend fun reopen(projectId: String) = withContext(io) {
        val status = if (itemDao.checkedCount(projectId) > 0) ProjectStatus.IN_PROGRESS else ProjectStatus.DRAFT
        projectDao.updateStatus(projectId, status, Instant.now())
    }

    /**
     * Deep-copy a project with fresh UUIDs. [copyResults] keeps item statuses/notes (otherwise
     * resets to PENDING); [copyPhotos] duplicates the JPEG files and photo rows.
     */
    suspend fun duplicateProject(
        sourceId: String,
        copyResults: Boolean,
        copyPhotos: Boolean,
    ): String? = withContext(io) {
        val tree = projectDao.getProjectTree(sourceId) ?: return@withContext null
        val now = Instant.now()
        val newProjectId = UUID.randomUUID().toString()
        val src = tree.project

        val categories = mutableListOf<CategoryEntity>()
        val items = mutableListOf<ItemEntity>()
        val photos = mutableListOf<PhotoEntity>()
        val pathMap = mutableMapOf<String, String>() // old file path -> new file path

        tree.categories.sortedBy { it.category.sortOrder }.forEach { cw ->
            val newCategoryId = UUID.randomUUID().toString()
            categories += cw.category.copy(id = newCategoryId, projectId = newProjectId)
            cw.items.sortedBy { it.item.sortOrder }.forEach { iw ->
                val newItemId = UUID.randomUUID().toString()
                val s = iw.item
                items += if (copyResults) {
                    s.copy(id = newItemId, categoryId = newCategoryId, projectId = newProjectId, updatedAt = now)
                } else {
                    s.copy(
                        id = newItemId,
                        categoryId = newCategoryId,
                        projectId = newProjectId,
                        status = ItemStatus.PENDING,
                        condition = null,
                        severity = null,
                        notes = null,
                        checkedAt = null,
                        updatedAt = now,
                    )
                }
                if (copyPhotos) {
                    iw.photos.forEach { ph ->
                        val srcFile = File(ph.filePath)
                        if (srcFile.exists()) {
                            val dest = fileStorage.copyIntoProject(srcFile, newProjectId)
                            pathMap[ph.filePath] = dest.absolutePath
                            photos += ph.copy(
                                id = UUID.randomUUID().toString(),
                                itemId = newItemId,
                                projectId = newProjectId,
                                filePath = dest.absolutePath,
                                createdAt = now,
                            )
                        }
                    }
                }
            }
        }

        // Project-level photos (itemId == null) are off the relation tree — copy them too.
        if (copyPhotos) {
            photoDao.getByProject(sourceId).filter { it.itemId == null }.forEach { ph ->
                val srcFile = File(ph.filePath)
                if (srcFile.exists()) {
                    val dest = fileStorage.copyIntoProject(srcFile, newProjectId)
                    pathMap[ph.filePath] = dest.absolutePath
                    photos += ph.copy(
                        id = UUID.randomUUID().toString(),
                        itemId = null,
                        projectId = newProjectId,
                        filePath = dest.absolutePath,
                        createdAt = now,
                    )
                }
            }
        }

        val newProject = src.copy(
            id = newProjectId,
            boatName = src.boatName + " (copy)",
            status = ProjectStatus.DRAFT,
            coverPhotoPath = if (copyPhotos) src.coverPhotoPath?.let { pathMap[it] } else null,
            createdAt = now,
            updatedAt = now,
        )

        db.withTransaction {
            projectDao.insert(newProject)
            categoryDao.insertAll(categories)
            itemDao.insertAll(items)
            if (photos.isNotEmpty()) photoDao.insertAll(photos)
        }
        newProjectId
    }

    /** Danger zone: remove every project (cascade) and wipe all photo files. */
    suspend fun deleteAllData() = withContext(io) {
        projectDao.deleteAll()
        fileStorage.cleanupOrphans(emptySet())
    }
}
