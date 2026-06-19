package com.hullcheck.data.seed

import android.content.Context
import com.hullcheck.data.db.entity.CategoryEntity
import com.hullcheck.data.db.entity.ItemEntity
import com.hullcheck.data.model.ItemStatus
import com.hullcheck.data.seed.dto.ChecklistTemplateDto
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Categories + items freshly instantiated for one project (all items PENDING, new UUIDs). */
data class InstantiatedChecklist(
    val categories: List<CategoryEntity>,
    val items: List<ItemEntity>,
)

/**
 * Loads the bundled default checklist from assets and instantiates a per-project copy so
 * edits to one boat's checklist never affect another's.
 */
@Singleton
class ChecklistSeed @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
) {
    companion object {
        const val ASSET_NAME = "checklist_default.json"
    }

    fun loadDefaultJson(): String =
        context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }

    fun parse(raw: String): ChecklistTemplateDto = json.decodeFromString(raw)

    fun loadDefaultTemplate(): ChecklistTemplateDto = parse(loadDefaultJson())

    /** Build Room entities for [template] bound to [projectId]. */
    fun instantiate(
        template: ChecklistTemplateDto,
        projectId: String,
        now: Instant,
    ): InstantiatedChecklist {
        val categories = mutableListOf<CategoryEntity>()
        val items = mutableListOf<ItemEntity>()

        template.categories.forEachIndexed { categoryIndex, categoryDto ->
            val categoryId = UUID.randomUUID().toString()
            categories += CategoryEntity(
                id = categoryId,
                projectId = projectId,
                name = categoryDto.name,
                iconKey = categoryDto.iconKey,
                sortOrder = categoryIndex,
                isEngineRelated = categoryDto.isEngineRelated,
                isSkippable = categoryDto.isSkippable,
            )
            categoryDto.items.forEachIndexed { itemIndex, itemDto ->
                items += ItemEntity(
                    id = UUID.randomUUID().toString(),
                    categoryId = categoryId,
                    projectId = projectId,
                    title = itemDto.title,
                    guidance = itemDto.guidance,
                    status = ItemStatus.PENDING,
                    condition = null,
                    severity = null,
                    notes = null,
                    isCustom = false,
                    sortOrder = itemIndex,
                    checkedAt = null,
                    updatedAt = now,
                )
            }
        }
        return InstantiatedChecklist(categories, items)
    }
}
