package com.hullcheck.data.model

import java.time.Instant

/** Domain model for an inspection photo (decoupled from PhotoEntity). */
data class Photo(
    val id: String,
    val itemId: String?,
    val projectId: String,
    val filePath: String,
    val caption: String?,
    val createdAt: Instant,
)
