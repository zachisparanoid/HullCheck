package com.hullcheck.di

import com.hullcheck.data.db.dao.PhotoDao
import com.hullcheck.data.db.dao.TemplateDao
import com.hullcheck.data.db.entity.TemplateEntity
import com.hullcheck.data.seed.ChecklistSeed
import com.hullcheck.util.FileStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * First-launch / every-launch housekeeping run off the main thread:
 *  - ensures the built-in checklist template row exists,
 *  - deletes orphaned photo files left behind by interrupted deletes.
 */
@Singleton
class AppInitializer @Inject constructor(
    private val templateDao: TemplateDao,
    private val checklistSeed: ChecklistSeed,
    private val photoDao: PhotoDao,
    private val fileStorage: FileStorage,
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun initialize() {
        scope.launch {
            runCatching {
                if (templateDao.getBuiltIn() == null) {
                    val raw = checklistSeed.loadDefaultJson()
                    val dto = checklistSeed.parse(raw)
                    templateDao.insert(
                        TemplateEntity(
                            id = dto.templateId,
                            name = dto.name,
                            json = raw,
                            isBuiltIn = true,
                        ),
                    )
                }
            }
            runCatching {
                val known = photoDao.getAllFilePaths().toSet()
                fileStorage.cleanupOrphans(known)
            }
        }
    }
}
