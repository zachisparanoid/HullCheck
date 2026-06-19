package com.hullcheck.data.repository

import com.hullcheck.data.db.dao.CategoryProgressRow
import com.hullcheck.data.db.dao.ProgressRow
import com.hullcheck.data.db.entity.CategoryEntity
import com.hullcheck.data.db.entity.ItemEntity
import com.hullcheck.data.db.entity.PhotoEntity
import com.hullcheck.data.db.entity.ProjectEntity
import com.hullcheck.data.db.relation.CategoryWithItems
import com.hullcheck.data.db.relation.ItemWithPhotos
import com.hullcheck.data.db.relation.ProjectWithCategoriesAndItems
import com.hullcheck.data.model.CategoryProgress
import com.hullcheck.data.model.ChecklistItem
import com.hullcheck.data.model.InspectionCategory
import com.hullcheck.data.model.Photo
import com.hullcheck.data.model.Project
import com.hullcheck.data.model.ProjectProgress

// ---------- Entity / relation -> domain ----------

fun PhotoEntity.toDomain(): Photo = Photo(
    id = id,
    itemId = itemId,
    projectId = projectId,
    filePath = filePath,
    caption = caption,
    createdAt = createdAt,
)

fun ItemEntity.toDomain(photos: List<Photo> = emptyList()): ChecklistItem = ChecklistItem(
    id = id,
    categoryId = categoryId,
    projectId = projectId,
    title = title,
    guidance = guidance,
    status = status,
    condition = condition,
    severity = severity,
    notes = notes,
    expiryDate = expiryDate,
    measurementValue = measurementValue,
    measurementUnit = measurementUnit,
    isCustom = isCustom,
    sortOrder = sortOrder,
    checkedAt = checkedAt,
    updatedAt = updatedAt,
    photos = photos,
)

fun ItemWithPhotos.toDomain(): ChecklistItem =
    item.toDomain(photos.sortedBy { it.createdAt }.map { it.toDomain() })

fun CategoryEntity.toDomain(items: List<ChecklistItem> = emptyList()): InspectionCategory =
    InspectionCategory(
        id = id,
        projectId = projectId,
        name = name,
        iconKey = iconKey,
        sortOrder = sortOrder,
        isEngineRelated = isEngineRelated,
        isSkippable = isSkippable,
        items = items,
    )

fun CategoryWithItems.toDomain(): InspectionCategory =
    category.toDomain(items.map { it.toDomain() }.sortedBy { it.sortOrder })

fun ProjectEntity.toDomain(categories: List<InspectionCategory> = emptyList()): Project = Project(
    id = id,
    boatName = boatName,
    boatType = boatType,
    make = make,
    model = model,
    yearBuilt = yearBuilt,
    hullId = hullId,
    lengthFeet = lengthFeet,
    registrationNumber = registrationNumber,
    clientName = clientName,
    surveyorName = surveyorName,
    location = location,
    surveyDate = surveyDate,
    surveyType = surveyType,
    status = status,
    generalNotes = generalNotes,
    reportSummary = reportSummary,
    coverPhotoPath = coverPhotoPath,
    latitude = latitude,
    longitude = longitude,
    createdAt = createdAt,
    updatedAt = updatedAt,
    categories = categories,
)

fun ProjectWithCategoriesAndItems.toDomain(): Project =
    project.toDomain(categories.map { it.toDomain() }.sortedBy { it.sortOrder })

fun ProgressRow.toProgress(): ProjectProgress = ProjectProgress(
    total = total,
    completed = completed,
    ok = ok,
    defects = defects,
    needsService = needsService,
    na = na,
    pending = pending,
)

fun CategoryProgressRow.toCategoryProgress(): CategoryProgress = CategoryProgress(
    total = total,
    completed = completed,
    defects = defects,
    na = na,
)

// ---------- Domain -> entity ----------

fun Project.toEntity(): ProjectEntity = ProjectEntity(
    id = id,
    boatName = boatName,
    boatType = boatType,
    make = make,
    model = model,
    yearBuilt = yearBuilt,
    hullId = hullId,
    lengthFeet = lengthFeet,
    registrationNumber = registrationNumber,
    clientName = clientName,
    surveyorName = surveyorName,
    location = location,
    surveyDate = surveyDate,
    surveyType = surveyType,
    status = status,
    generalNotes = generalNotes,
    reportSummary = reportSummary,
    coverPhotoPath = coverPhotoPath,
    latitude = latitude,
    longitude = longitude,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun InspectionCategory.toEntity(): CategoryEntity = CategoryEntity(
    id = id,
    projectId = projectId,
    name = name,
    iconKey = iconKey,
    sortOrder = sortOrder,
    isEngineRelated = isEngineRelated,
    isSkippable = isSkippable,
)

fun ChecklistItem.toEntity(): ItemEntity = ItemEntity(
    id = id,
    categoryId = categoryId,
    projectId = projectId,
    title = title,
    guidance = guidance,
    status = status,
    condition = condition,
    severity = severity,
    notes = notes,
    expiryDate = expiryDate,
    measurementValue = measurementValue,
    measurementUnit = measurementUnit,
    isCustom = isCustom,
    sortOrder = sortOrder,
    checkedAt = checkedAt,
    updatedAt = updatedAt,
)

fun Photo.toEntity(): PhotoEntity = PhotoEntity(
    id = id,
    itemId = itemId,
    projectId = projectId,
    filePath = filePath,
    caption = caption,
    createdAt = createdAt,
)
