package com.hullcheck.data.repository

import com.hullcheck.data.db.dao.CategoryDao
import com.hullcheck.data.db.dao.ItemDao
import com.hullcheck.data.db.dao.PhotoDao
import com.hullcheck.data.db.dao.ProjectDao
import com.hullcheck.data.db.entity.CategoryEntity
import com.hullcheck.data.db.entity.ItemEntity
import com.hullcheck.data.model.CategoryProgress
import com.hullcheck.data.model.CategoryWithProgress
import com.hullcheck.data.model.ChecklistItem
import com.hullcheck.data.model.ConditionRating
import com.hullcheck.data.model.InspectionCategory
import com.hullcheck.data.model.ItemStatus
import com.hullcheck.data.model.ProjectStatus
import com.hullcheck.data.model.Severity
import com.hullcheck.di.IoDispatcher
import com.hullcheck.util.FileStorage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChecklistRepository @Inject constructor(
    private val categoryDao: CategoryDao,
    private val itemDao: ItemDao,
    private val photoDao: PhotoDao,
    private val projectDao: ProjectDao,
    private val fileStorage: FileStorage,
    @IoDispatcher private val io: CoroutineDispatcher,
) {

    fun observeCategoriesWithProgress(projectId: String): Flow<List<CategoryWithProgress>> =
        combine(
            categoryDao.observeByProject(projectId),
            categoryDao.observeCategoryProgress(projectId),
        ) { categories, progressRows ->
            val byId = progressRows.associateBy { it.categoryId }
            categories.map { c ->
                CategoryWithProgress(
                    category = c.toDomain(),
                    progress = byId[c.id]?.toCategoryProgress() ?: CategoryProgress.EMPTY,
                )
            }
        }

    fun observeCategory(categoryId: String): Flow<InspectionCategory?> =
        categoryDao.observeById(categoryId).map { it?.toDomain() }

    fun observeItems(categoryId: String): Flow<List<ChecklistItem>> =
        itemDao.observeByCategoryWithPhotos(categoryId).map { list -> list.map { it.toDomain() } }

    fun observeItem(itemId: String): Flow<ChecklistItem?> =
        itemDao.observeWithPhotos(itemId).map { it?.toDomain() }

    fun observeDefects(projectId: String): Flow<List<ChecklistItem>> =
        itemDao.observeDefectsWithPhotos(projectId).map { list -> list.map { it.toDomain() } }

    /** projectId -> map of categoryId to display name (used to label defects). */
    fun observeCategoryNames(projectId: String): Flow<Map<String, String>> =
        categoryDao.observeByProject(projectId).map { cats -> cats.associate { it.id to it.name } }

    // ---- Mutations ----

    /** Quick inline status set from the checklist list. Severity is preserved on re-tap. */
    suspend fun setStatus(
        itemId: String,
        projectId: String,
        status: ItemStatus,
        severity: Severity? = null,
    ) = withContext(io) {
        val now = Instant.now()
        val checkedAt = if (status == ItemStatus.PENDING) null else now
        val resolvedSeverity = when {
            status.isDefectLike -> severity ?: itemDao.getById(itemId)?.severity ?: Severity.MODERATE
            else -> null
        }
        itemDao.setStatus(itemId, status, resolvedSeverity, checkedAt, now)
        touchAndPromote(projectId, now)
    }

    /** Full save from the item-detail editor. */
    suspend fun updateItem(item: ChecklistItem) = withContext(io) {
        val now = Instant.now()
        val checkedAt = if (item.status == ItemStatus.PENDING) null else (item.checkedAt ?: now)
        val severity = if (item.status.isDefectLike) item.severity else null
        itemDao.update(item.copy(checkedAt = checkedAt, severity = severity, updatedAt = now).toEntity())
        touchAndPromote(item.projectId, now)
    }

    suspend fun setNotes(itemId: String, projectId: String, notes: String?) = withContext(io) {
        val now = Instant.now()
        itemDao.setNotes(itemId, notes?.ifBlank { null }, now)
        projectDao.touch(projectId, now)
    }

    suspend fun setCondition(itemId: String, projectId: String, condition: ConditionRating?) = withContext(io) {
        val now = Instant.now()
        itemDao.setCondition(itemId, condition, now)
        projectDao.touch(projectId, now)
    }

    suspend fun setTitle(itemId: String, projectId: String, title: String) = withContext(io) {
        val now = Instant.now()
        itemDao.setTitle(itemId, title.trim(), now)
        projectDao.touch(projectId, now)
    }

    suspend fun setExpiry(itemId: String, projectId: String, expiryDate: LocalDate?) = withContext(io) {
        val now = Instant.now()
        itemDao.setExpiry(itemId, expiryDate, now)
        projectDao.touch(projectId, now)
    }

    suspend fun setMeasurement(itemId: String, projectId: String, value: Double?, unit: String?) = withContext(io) {
        val now = Instant.now()
        itemDao.setMeasurement(itemId, value, unit?.ifBlank { null }, now)
        projectDao.touch(projectId, now)
    }

    suspend fun resetItem(itemId: String, projectId: String) = withContext(io) {
        val now = Instant.now()
        val existing = itemDao.getById(itemId) ?: return@withContext
        itemDao.update(
            existing.copy(
                status = ItemStatus.PENDING,
                condition = null,
                severity = null,
                checkedAt = null,
                updatedAt = now,
            ),
        )
        projectDao.touch(projectId, now)
    }

    suspend fun markAllNa(categoryId: String, projectId: String) = withContext(io) {
        val now = Instant.now()
        itemDao.setAllStatusInCategory(categoryId, ItemStatus.NOT_APPLICABLE, now)
        touchAndPromote(projectId, now)
    }

    suspend fun addCustomItem(
        categoryId: String,
        projectId: String,
        title: String,
        guidance: String?,
    ): String = withContext(io) {
        val now = Instant.now()
        val id = UUID.randomUUID().toString()
        val sortOrder = itemDao.maxSortOrder(categoryId) + 1
        itemDao.insert(
            ItemEntity(
                id = id,
                categoryId = categoryId,
                projectId = projectId,
                title = title.trim(),
                guidance = guidance?.ifBlank { null },
                status = ItemStatus.PENDING,
                condition = null,
                severity = null,
                notes = null,
                isCustom = true,
                sortOrder = sortOrder,
                checkedAt = null,
                updatedAt = now,
            ),
        )
        projectDao.touch(projectId, now)
        id
    }

    /** Only custom items can be deleted; template items must be set N/A instead. */
    suspend fun deleteCustomItem(item: ChecklistItem) = withContext(io) {
        if (!item.isCustom) return@withContext
        val paths = photoDao.getByItem(item.id).map { it.filePath }
        itemDao.deleteById(item.id) // cascades photo rows
        fileStorage.deleteFiles(paths)
        projectDao.touch(item.projectId, Instant.now())
    }

    suspend fun addCustomCategory(projectId: String, name: String): String = withContext(io) {
        val now = Instant.now()
        val id = UUID.randomUUID().toString()
        val sortOrder = categoryDao.maxSortOrder(projectId) + 1
        categoryDao.insert(
            CategoryEntity(
                id = id,
                projectId = projectId,
                name = name.trim(),
                iconKey = "custom",
                sortOrder = sortOrder,
                isEngineRelated = false,
                isSkippable = true,
            ),
        )
        projectDao.touch(projectId, now)
        id
    }

    /** Bump project updatedAt and promote DRAFT -> IN_PROGRESS once anything is checked. */
    private suspend fun touchAndPromote(projectId: String, now: Instant) {
        projectDao.touch(projectId, now)
        val project = projectDao.getById(projectId) ?: return
        if (project.status == ProjectStatus.DRAFT && itemDao.checkedCount(projectId) > 0) {
            projectDao.updateStatus(projectId, ProjectStatus.IN_PROGRESS, now)
        }
    }
}
