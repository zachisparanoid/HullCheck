package com.hullcheck.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.hullcheck.data.model.PhotoQuality
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Decodes, EXIF-rotates, and downscales an image to a manageable JPEG. Uses inSampleSize so
 * full-resolution camera frames are never fully decoded into memory (guards against OOM).
 */
object ImageScaler {

    /**
     * @param open supplies a *fresh* InputStream each call (used up to three times: bounds,
     *             pixels, EXIF). For a content Uri pass `{ resolver.openInputStream(uri)!! }`,
     *             for a File pass `{ FileInputStream(file) }`.
     */
    fun saveScaledJpeg(open: () -> InputStream, dest: File, quality: PhotoQuality): File {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open().use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("Unable to decode image bounds")
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = computeInSampleSize(bounds.outWidth, bounds.outHeight, quality.maxLongEdgePx)
        }
        val decoded = open().use { BitmapFactory.decodeStream(it, null, decodeOptions) }
            ?: throw IOException("Unable to decode image")

        val orientation = runCatching {
            open().use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val finished = transform(decoded, orientation, quality.maxLongEdgePx)

        dest.parentFile?.mkdirs()
        FileOutputStream(dest).use { out ->
            finished.compress(Bitmap.CompressFormat.JPEG, quality.jpegQuality, out)
            out.flush()
        }
        finished.recycle()
        return dest
    }

    private fun computeInSampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        var w = width
        var h = height
        while (maxOf(w, h) / 2 >= maxEdge) {
            w /= 2
            h /= 2
            sample *= 2
        }
        return sample
    }

    private fun transform(src: Bitmap, orientation: Int, maxEdge: Int): Bitmap {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
        }
        val maxDim = maxOf(src.width, src.height)
        if (maxDim > maxEdge) {
            val scale = maxEdge.toFloat() / maxDim
            matrix.postScale(scale, scale)
        }
        if (matrix.isIdentity) return src
        val result = Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        if (result !== src) src.recycle()
        return result
    }
}
