package com.hullcheck.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.hullcheck.data.db.entity.ProjectEntity
import com.hullcheck.data.db.relation.ProjectWithCategoriesAndItems
import com.hullcheck.data.model.ProjectStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface ProjectDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: ProjectEntity)

    @Update
    suspend fun update(project: ProjectEntity)

    @Delete
    suspend fun delete(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteById(projectId: String)

    @Query("DELETE FROM projects")
    suspend fun deleteAll()

    @Query("SELECT * FROM projects WHERE id = :projectId")
    fun observeById(projectId: String): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getById(projectId: String): ProjectEntity?

    @Query("SELECT id FROM projects")
    suspend fun getAllIds(): List<String>

    /** Drives the home list: each project with live total/completed/defect counts. */
    @Query(
        """
        SELECT p.*,
            (SELECT COUNT(*) FROM items i WHERE i.projectId = p.id) AS total,
            (SELECT COUNT(*) FROM items i WHERE i.projectId = p.id AND i.status != 'PENDING') AS completed,
            (SELECT COUNT(*) FROM items i WHERE i.projectId = p.id AND i.status IN ('DEFECT','NEEDS_SERVICE')) AS defects
        FROM projects p
        """,
    )
    fun observeProjectListRows(): Flow<List<ProjectListRow>>

    @Query(
        """
        SELECT
            COUNT(*) AS total,
            IFNULL(SUM(CASE WHEN status != 'PENDING' THEN 1 ELSE 0 END), 0) AS completed,
            IFNULL(SUM(CASE WHEN status = 'OK' THEN 1 ELSE 0 END), 0) AS ok,
            IFNULL(SUM(CASE WHEN status = 'DEFECT' THEN 1 ELSE 0 END), 0) AS defects,
            IFNULL(SUM(CASE WHEN status = 'NEEDS_SERVICE' THEN 1 ELSE 0 END), 0) AS needsService,
            IFNULL(SUM(CASE WHEN status = 'NOT_APPLICABLE' THEN 1 ELSE 0 END), 0) AS na,
            IFNULL(SUM(CASE WHEN status = 'PENDING' THEN 1 ELSE 0 END), 0) AS pending
        FROM items WHERE projectId = :projectId
        """,
    )
    fun observeProgress(projectId: String): Flow<ProgressRow>

    @Transaction
    @Query("SELECT * FROM projects WHERE id = :projectId")
    fun observeProjectTree(projectId: String): Flow<ProjectWithCategoriesAndItems?>

    @Transaction
    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProjectTree(projectId: String): ProjectWithCategoriesAndItems?

    @Query("UPDATE projects SET status = :status, updatedAt = :now WHERE id = :projectId")
    suspend fun updateStatus(projectId: String, status: ProjectStatus, now: Instant)

    @Query("UPDATE projects SET coverPhotoPath = :path, updatedAt = :now WHERE id = :projectId")
    suspend fun updateCoverPhoto(projectId: String, path: String?, now: Instant)

    @Query("UPDATE projects SET reportSummary = :summary, updatedAt = :now WHERE id = :projectId")
    suspend fun updateReportSummary(projectId: String, summary: String?, now: Instant)

    @Query("UPDATE projects SET updatedAt = :now WHERE id = :projectId")
    suspend fun touch(projectId: String, now: Instant)
}
