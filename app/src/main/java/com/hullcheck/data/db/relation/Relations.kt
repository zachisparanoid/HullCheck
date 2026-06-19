package com.hullcheck.data.db.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.hullcheck.data.db.entity.CategoryEntity
import com.hullcheck.data.db.entity.ItemEntity
import com.hullcheck.data.db.entity.PhotoEntity
import com.hullcheck.data.db.entity.ProjectEntity

/** An item together with its photos. */
data class ItemWithPhotos(
    @Embedded val item: ItemEntity,
    @Relation(parentColumn = "id", entityColumn = "itemId")
    val photos: List<PhotoEntity>,
)

/** A category together with its items (each item carrying its photos). */
data class CategoryWithItems(
    @Embedded val category: CategoryEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "categoryId",
        entity = ItemEntity::class,
    )
    val items: List<ItemWithPhotos>,
)

/**
 * The full project tree in a single (transactional) read: project → categories →
 * items → photos. Used by the report generator, JSON exporter, and duplicate flow.
 */
data class ProjectWithCategoriesAndItems(
    @Embedded val project: ProjectEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "projectId",
        entity = CategoryEntity::class,
    )
    val categories: List<CategoryWithItems>,
)
