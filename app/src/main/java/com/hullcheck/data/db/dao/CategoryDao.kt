package com.hullcheck.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hullcheck.data.db.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Delete
    suspend fun delete(category: CategoryEntity)

    @Query("SELECT * FROM categories WHERE projectId = :projectId ORDER BY sortOrder ASC")
    fun observeByProject(projectId: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    fun observeById(categoryId: String): Flow<CategoryEntity?>

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getById(categoryId: String): CategoryEntity?

    @Query("SELECT IFNULL(MAX(sortOrder), -1) FROM categories WHERE projectId = :projectId")
    suspend fun maxSortOrder(projectId: String): Int

    @Query(
        """
        SELECT categoryId AS categoryId,
            COUNT(*) AS total,
            IFNULL(SUM(CASE WHEN status != 'PENDING' THEN 1 ELSE 0 END), 0) AS completed,
            IFNULL(SUM(CASE WHEN status IN ('DEFECT','NEEDS_SERVICE') THEN 1 ELSE 0 END), 0) AS defects,
            IFNULL(SUM(CASE WHEN status = 'NOT_APPLICABLE' THEN 1 ELSE 0 END), 0) AS na
        FROM items WHERE projectId = :projectId GROUP BY categoryId
        """,
    )
    fun observeCategoryProgress(projectId: String): Flow<List<CategoryProgressRow>>
}
