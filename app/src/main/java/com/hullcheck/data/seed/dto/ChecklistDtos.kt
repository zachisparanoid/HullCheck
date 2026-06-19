package com.hullcheck.data.seed.dto

import kotlinx.serialization.Serializable

/** Wire format for bundled / stored checklist templates (assets/checklist_default.json). */
@Serializable
data class ChecklistTemplateDto(
    val templateId: String,
    val name: String,
    val version: Int = 1,
    val categories: List<CategoryDto>,
)

@Serializable
data class CategoryDto(
    val name: String,
    val iconKey: String,
    val isEngineRelated: Boolean = false,
    val isSkippable: Boolean = false,
    val items: List<ItemDto>,
)

@Serializable
data class ItemDto(
    val title: String,
    val guidance: String? = null,
)
