package com.hullcheck.util

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.hullcheck.data.model.PhotoQuality
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileInputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns all on-device file management for photos and exports. Photos live in app-internal
 * storage under files/photos/<projectId>/; exports go to the app's external Documents dir so
 * they survive uninstall-share flows and need no storage permission.
 */
@Singleton
class FileStorage @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val appContext = context.applicationContext

    private val photosRoot: File
        get() = File(appContext.filesDir, "photos").apply { mkdirs() }

    val exportsDir: File
        get() = (appContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: File(appContext.filesDir, "exports")).apply { mkdirs() }

    private fun projectPhotoDir(projectId: String): File =
        File(photosRoot, projectId).apply { mkdirs() }

    fun newPhotoFile(projectId: String): File =
        File(projectPhotoDir(projectId), "${UUID.randomUUID()}.jpg")

    fun tempCaptureFile(): File =
        File(appContext.cacheDir, "capture_${UUID.randomUUID()}.jpg").apply { parentFile?.mkdirs() }

    // ---- Saving photos (always downscaled + EXIF corrected) ----

    /** Persist a captured temp file into a project photo, returning the final file. */
    fun savePhotoFromFile(source: File, projectId: String, quality: PhotoQuality): File {
        val dest = newPhotoFile(projectId)
        ImageScaler.saveScaledJpeg({ FileInputStream(source) }, dest, quality)
        source.delete()
        return dest
    }

    /** Persist a gallery / imported content Uri into a project photo. */
    fun savePhotoFromUri(uri: Uri, projectId: String, quality: PhotoQuality): File {
        val dest = newPhotoFile(projectId)
        ImageScaler.saveScaledJpeg(
            open = { appContext.contentResolver.openInputStream(uri) ?: error("Cannot open $uri") },
            dest = dest,
            quality = quality,
        )
        return dest
    }

    /** Copy an already-prepared image file (e.g. from a JSON import bundle) into a project. */
    fun copyIntoProject(source: File, projectId: String): File {
        val dest = newPhotoFile(projectId)
        source.copyTo(dest, overwrite = true)
        return dest
    }

    // ---- Report branding (logo / signature) ----

    fun brandingFile(key: String): File =
        File(File(appContext.filesDir, "branding").apply { mkdirs() }, "$key.jpg")

    fun saveBrandingImage(uri: Uri, key: String): File {
        val dest = brandingFile(key)
        ImageScaler.saveScaledJpeg(
            open = { appContext.contentResolver.openInputStream(uri) ?: error("Cannot open $uri") },
            dest = dest,
            quality = PhotoQuality.STANDARD,
        )
        return dest
    }

    fun deleteBranding(key: String) {
        runCatching { brandingFile(key).takeIf { it.exists() }?.delete() }
    }

    // ---- Sharing ----

    fun uriForFile(file: File): Uri =
        FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file)

    // ---- Deletion / cleanup ----

    fun deleteFile(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }

    fun deleteFiles(paths: Collection<String>) = paths.forEach { deleteFile(it) }

    fun deleteProjectPhotoDir(projectId: String) {
        runCatching { projectPhotoDir(projectId).deleteRecursively() }
    }

    /** Remove any photo file on disk that no longer has a database row (orphan pass on launch). */
    fun cleanupOrphans(knownPaths: Set<String>) {
        runCatching {
            photosRoot.walkTopDown()
                .filter { it.isFile }
                .forEach { if (it.absolutePath !in knownPaths) it.delete() }
        }
    }
}
