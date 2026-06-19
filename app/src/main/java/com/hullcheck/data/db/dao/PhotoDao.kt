package com.hullcheck.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hullcheck.data.db.entity.PhotoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PhotoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(photo: PhotoEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(photos: List<PhotoEntity>)

    @Delete
    suspend fun delete(photo: PhotoEntity)

    @Query("DELETE FROM photos WHERE id = :photoId")
    suspend fun deleteById(photoId: String)

    @Query("SELECT * FROM photos WHERE itemId = :itemId ORDER BY createdAt ASC")
    fun observeByItem(itemId: String): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE projectId = :projectId ORDER BY createdAt ASC")
    fun observeByProject(projectId: String): Flow<List<PhotoEntity>>

    @Query("SELECT * FROM photos WHERE id = :photoId")
    suspend fun getById(photoId: String): PhotoEntity?

    @Query("SELECT * FROM photos WHERE itemId = :itemId")
    suspend fun getByItem(itemId: String): List<PhotoEntity>

    @Query("SELECT * FROM photos WHERE projectId = :projectId")
    suspend fun getByProject(projectId: String): List<PhotoEntity>

    @Query("UPDATE photos SET caption = :caption WHERE id = :photoId")
    suspend fun setCaption(photoId: String, caption: String?)

    /** All known photo file paths — used by the orphan-file cleanup pass on launch. */
    @Query("SELECT filePath FROM photos")
    suspend fun getAllFilePaths(): List<String>

    @Query("SELECT filePath FROM photos WHERE projectId = :projectId")
    suspend fun getFilePathsByProject(projectId: String): List<String>
}
