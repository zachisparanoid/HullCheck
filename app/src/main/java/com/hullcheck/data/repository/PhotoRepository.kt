package com.hullcheck.data.repository

import android.graphics.Bitmap
import android.net.Uri
import com.hullcheck.data.db.dao.PhotoDao
import com.hullcheck.data.db.entity.PhotoEntity
import com.hullcheck.data.model.Photo
import com.hullcheck.di.IoDispatcher
import com.hullcheck.util.FileStorage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PhotoRepository @Inject constructor(
    private val photoDao: PhotoDao,
    private val fileStorage: FileStorage,
    private val settingsRepository: SettingsRepository,
    @IoDispatcher private val io: CoroutineDispatcher,
) {

    fun observePhotosByItem(itemId: String): Flow<List<Photo>> =
        photoDao.observeByItem(itemId).map { list -> list.map { it.toDomain() } }

    fun observePhotosByProject(projectId: String): Flow<List<Photo>> =
        photoDao.observeByProject(projectId).map { list -> list.map { it.toDomain() } }

    /** Import a gallery / picked image: downscale + store, then insert the row. */
    suspend fun addPhotoFromUri(projectId: String, itemId: String?, uri: Uri): Photo = withContext(io) {
        val quality = settingsRepository.current().photoQuality
        val dest = fileStorage.savePhotoFromUri(uri, projectId, quality)
        insertPhoto(projectId, itemId, dest)
    }

    /** Persist an in-app camera capture (temp file) into a project photo. */
    suspend fun addPhotoFromCapture(projectId: String, itemId: String?, tempFile: File): Photo = withContext(io) {
        val quality = settingsRepository.current().photoQuality
        val dest = fileStorage.savePhotoFromFile(tempFile, projectId, quality)
        insertPhoto(projectId, itemId, dest)
    }

    /** Save an annotated (composited) bitmap as a new project photo. */
    suspend fun addPhotoBitmap(projectId: String, itemId: String?, bitmap: Bitmap): Photo = withContext(io) {
        val quality = settingsRepository.current().photoQuality
        val dest = fileStorage.newPhotoFile(projectId)
        FileOutputStream(dest).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, quality.jpegQuality, out) }
        insertPhoto(projectId, itemId, dest)
    }

    private suspend fun insertPhoto(projectId: String, itemId: String?, dest: File): Photo {
        val entity = PhotoEntity(
            id = UUID.randomUUID().toString(),
            itemId = itemId,
            projectId = projectId,
            filePath = dest.absolutePath,
            caption = null,
            createdAt = Instant.now(),
        )
        photoDao.insert(entity)
        return entity.toDomain()
    }

    suspend fun setCaption(photoId: String, caption: String?) = withContext(io) {
        photoDao.setCaption(photoId, caption?.ifBlank { null })
    }

    // ---- Undoable delete: remove the row now, finalize the file after the undo window ----

    suspend fun softDeletePhoto(photo: Photo) = withContext(io) {
        photoDao.deleteById(photo.id)
    }

    suspend fun restorePhoto(photo: Photo) = withContext(io) {
        photoDao.insert(photo.toEntity())
    }

    suspend fun finalizeDelete(photo: Photo) = withContext(io) {
        fileStorage.deleteFile(photo.filePath)
    }
}
