package com.hullcheck.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hullcheck.data.db.entity.TemplateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TemplateDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(template: TemplateEntity)

    @Query("SELECT * FROM templates ORDER BY isBuiltIn DESC, name ASC")
    fun observeAll(): Flow<List<TemplateEntity>>

    @Query("SELECT * FROM templates ORDER BY isBuiltIn DESC, name ASC")
    suspend fun getAll(): List<TemplateEntity>

    @Query("SELECT * FROM templates WHERE id = :templateId")
    suspend fun getById(templateId: String): TemplateEntity?

    @Query("DELETE FROM templates WHERE id = :templateId AND isBuiltIn = 0")
    suspend fun deleteUserTemplate(templateId: String)

    @Query("SELECT * FROM templates WHERE isBuiltIn = 1 LIMIT 1")
    suspend fun getBuiltIn(): TemplateEntity?

    @Query("SELECT COUNT(*) FROM templates")
    suspend fun count(): Int
}
