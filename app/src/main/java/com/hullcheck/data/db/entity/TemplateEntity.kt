package com.hullcheck.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey val id: String,
    val name: String,
    /** Serialized category/item structure (see ChecklistTemplateDto). */
    val json: String,
    val isBuiltIn: Boolean,
)
