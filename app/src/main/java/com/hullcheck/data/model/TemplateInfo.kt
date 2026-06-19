package com.hullcheck.data.model

/** Lightweight template descriptor for the templates list (parsed counts). */
data class TemplateInfo(
    val id: String,
    val name: String,
    val isBuiltIn: Boolean,
    val categoryCount: Int,
    val itemCount: Int,
)
