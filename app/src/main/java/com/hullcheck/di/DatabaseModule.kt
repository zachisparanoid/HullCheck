package com.hullcheck.di

import android.content.Context
import androidx.room.Room
import com.hullcheck.BuildConfig
import com.hullcheck.data.db.HullCheckDatabase
import com.hullcheck.data.db.dao.CategoryDao
import com.hullcheck.data.db.dao.ItemDao
import com.hullcheck.data.db.dao.PhotoDao
import com.hullcheck.data.db.dao.ProjectDao
import com.hullcheck.data.db.dao.TemplateDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): HullCheckDatabase =
        Room.databaseBuilder(context, HullCheckDatabase::class.java, HullCheckDatabase.NAME)
            .apply {
                addMigrations(*HullCheckDatabase.MIGRATIONS)
                // Destructive fallback only in debug; release builds require real migrations.
                if (BuildConfig.DEBUG) {
                    fallbackToDestructiveMigration()
                }
            }
            .build()

    @Provides fun provideProjectDao(db: HullCheckDatabase): ProjectDao = db.projectDao()
    @Provides fun provideCategoryDao(db: HullCheckDatabase): CategoryDao = db.categoryDao()
    @Provides fun provideItemDao(db: HullCheckDatabase): ItemDao = db.itemDao()
    @Provides fun providePhotoDao(db: HullCheckDatabase): PhotoDao = db.photoDao()
    @Provides fun provideTemplateDao(db: HullCheckDatabase): TemplateDao = db.templateDao()

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
        isLenient = true
    }
}
