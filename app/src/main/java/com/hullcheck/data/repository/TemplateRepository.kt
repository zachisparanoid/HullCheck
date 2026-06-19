package com.hullcheck.data.repository

import com.hullcheck.data.db.dao.ProjectDao
import com.hullcheck.data.db.dao.TemplateDao
import com.hullcheck.data.db.entity.TemplateEntity
import com.hullcheck.data.model.TemplateInfo
import com.hullcheck.data.seed.ChecklistSeed
import com.hullcheck.data.seed.dto.CategoryDto
import com.hullcheck.data.seed.dto.ChecklistTemplateDto
import com.hullcheck.data.seed.dto.ItemDto
import com.hullcheck.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TemplateRepository @Inject constructor(
    private val templateDao: TemplateDao,
    private val projectDao: ProjectDao,
    private val checklistSeed: ChecklistSeed,
    private val json: Json,
    @IoDispatcher private val io: CoroutineDispatcher,
) {

    fun observeTemplateInfos(): Flow<List<TemplateInfo>> =
        templateDao.observeAll().map { list -> list.map { it.toInfo() } }

    suspend fun getAll(): List<TemplateEntity> = withContext(io) { templateDao.getAll() }

    /** Serialize a project's current checklist structure into a reusable template. */
    suspend fun saveProjectAsTemplate(projectId: String, name: String): String? = withContext(io) {
        val tree = projectDao.getProjectTree(projectId) ?: return@withContext null
        val dto = ChecklistTemplateDto(
            templateId = "user_${UUID.randomUUID()}",
            name = name.trim(),
            version = 1,
            categories = tree.categories.sortedBy { it.category.sortOrder }.map { cw ->
                CategoryDto(
                    name = cw.category.name,
                    iconKey = cw.category.iconKey,
                    isEngineRelated = cw.category.isEngineRelated,
                    isSkippable = cw.category.isSkippable,
                    items = cw.items.sortedBy { it.item.sortOrder }.map { ItemDto(it.item.title, it.item.guidance) },
                )
            },
        )
        val id = UUID.randomUUID().toString()
        templateDao.insert(
            TemplateEntity(
                id = id,
                name = name.trim(),
                json = json.encodeToString(ChecklistTemplateDto.serializer(), dto),
                isBuiltIn = false,
            ),
        )
        id
    }

    suspend fun deleteTemplate(id: String) = withContext(io) { templateDao.deleteUserTemplate(id) }

    private fun TemplateEntity.toInfo(): TemplateInfo {
        val dto = runCatching { checklistSeed.parse(json) }.getOrNull()
        return TemplateInfo(
            id = id,
            name = name,
            isBuiltIn = isBuiltIn,
            categoryCount = dto?.categories?.size ?: 0,
            itemCount = dto?.categories?.sumOf { it.items.size } ?: 0,
        )
    }
}
