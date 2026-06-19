package com.hullcheck.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hullcheck.data.db.dao.CategoryDao
import com.hullcheck.data.db.dao.ItemDao
import com.hullcheck.data.db.dao.PhotoDao
import com.hullcheck.data.db.dao.ProjectDao
import com.hullcheck.data.db.dao.TemplateDao
import com.hullcheck.data.db.entity.CategoryEntity
import com.hullcheck.data.db.entity.ItemEntity
import com.hullcheck.data.db.entity.PhotoEntity
import com.hullcheck.data.db.entity.ProjectEntity
import com.hullcheck.data.db.entity.TemplateEntity

@Database(
    entities = [
        ProjectEntity::class,
        CategoryEntity::class,
        ItemEntity::class,
        PhotoEntity::class,
        TemplateEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class HullCheckDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun categoryDao(): CategoryDao
    abstract fun itemDao(): ItemDao
    abstract fun photoDao(): PhotoDao
    abstract fun templateDao(): TemplateDao

    companion object {
        const val NAME = "hullcheck.db"

        /** v1 -> v2: add expiry/measurement to items and GPS coordinates to projects. */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN expiryDate INTEGER")
                db.execSQL("ALTER TABLE items ADD COLUMN measurementValue REAL")
                db.execSQL("ALTER TABLE items ADD COLUMN measurementUnit TEXT")
                db.execSQL("ALTER TABLE projects ADD COLUMN latitude REAL")
                db.execSQL("ALTER TABLE projects ADD COLUMN longitude REAL")
            }
        }

        val MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)
    }
}
