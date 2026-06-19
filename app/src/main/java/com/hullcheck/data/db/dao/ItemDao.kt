package com.hullcheck.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.hullcheck.data.db.entity.ItemEntity
import com.hullcheck.data.db.relation.ItemWithPhotos
import com.hullcheck.data.model.ConditionRating
import com.hullcheck.data.model.ItemStatus
import com.hullcheck.data.model.Severity
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

@Dao
interface ItemDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: ItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ItemEntity>)

    @Update
    suspend fun update(item: ItemEntity)

    @Delete
    suspend fun delete(item: ItemEntity)

    @Query("DELETE FROM items WHERE id = :itemId")
    suspend fun deleteById(itemId: String)

    @Query("SELECT * FROM items WHERE categoryId = :categoryId ORDER BY sortOrder ASC")
    fun observeByCategory(categoryId: String): Flow<List<ItemEntity>>

    @Transaction
    @Query("SELECT * FROM items WHERE categoryId = :categoryId ORDER BY sortOrder ASC")
    fun observeByCategoryWithPhotos(categoryId: String): Flow<List<ItemWithPhotos>>

    @Query("SELECT * FROM items WHERE id = :itemId")
    fun observeById(itemId: String): Flow<ItemEntity?>

    @Transaction
    @Query("SELECT * FROM items WHERE id = :itemId")
    fun observeWithPhotos(itemId: String): Flow<ItemWithPhotos?>

    @Query("SELECT * FROM items WHERE id = :itemId")
    suspend fun getById(itemId: String): ItemEntity?

    @Query(
        """
        SELECT * FROM items
        WHERE projectId = :projectId AND status IN ('DEFECT','NEEDS_SERVICE')
        ORDER BY sortOrder ASC
        """,
    )
    fun observeDefects(projectId: String): Flow<List<ItemEntity>>

    @Transaction
    @Query(
        """
        SELECT * FROM items
        WHERE projectId = :projectId AND status IN ('DEFECT','NEEDS_SERVICE')
        """,
    )
    fun observeDefectsWithPhotos(projectId: String): Flow<List<ItemWithPhotos>>

    @Query("SELECT IFNULL(MAX(sortOrder), -1) FROM items WHERE categoryId = :categoryId")
    suspend fun maxSortOrder(categoryId: String): Int

    @Query("SELECT COUNT(*) FROM items WHERE projectId = :projectId AND status != 'PENDING'")
    suspend fun checkedCount(projectId: String): Int

    /** Inline status set from the checklist list (also stamps checkedAt). */
    @Query("UPDATE items SET status = :status, severity = :severity, checkedAt = :checkedAt, updatedAt = :now WHERE id = :itemId")
    suspend fun setStatus(
        itemId: String,
        status: ItemStatus,
        severity: Severity?,
        checkedAt: Instant?,
        now: Instant,
    )

    @Query("UPDATE items SET notes = :notes, updatedAt = :now WHERE id = :itemId")
    suspend fun setNotes(itemId: String, notes: String?, now: Instant)

    @Query("UPDATE items SET condition = :condition, updatedAt = :now WHERE id = :itemId")
    suspend fun setCondition(itemId: String, condition: ConditionRating?, now: Instant)

    @Query("UPDATE items SET title = :title, updatedAt = :now WHERE id = :itemId")
    suspend fun setTitle(itemId: String, title: String, now: Instant)

    @Query("UPDATE items SET expiryDate = :expiryDate, updatedAt = :now WHERE id = :itemId")
    suspend fun setExpiry(itemId: String, expiryDate: LocalDate?, now: Instant)

    @Query("UPDATE items SET measurementValue = :value, measurementUnit = :unit, updatedAt = :now WHERE id = :itemId")
    suspend fun setMeasurement(itemId: String, value: Double?, unit: String?, now: Instant)

    @Query(
        """
        UPDATE items SET status = :status, checkedAt = :now, updatedAt = :now
        WHERE categoryId = :categoryId
        """,
    )
    suspend fun setAllStatusInCategory(categoryId: String, status: ItemStatus, now: Instant)
}
