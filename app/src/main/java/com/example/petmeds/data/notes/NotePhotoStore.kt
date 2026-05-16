package com.example.petmeds.data.notes

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persists user-supplied note images into app-private storage so the app keeps
 * working even after the original gallery URI is revoked or rotated. Stores
 * everything as JPEG q=85 with a 1600 px max edge to keep file sizes sane and
 * avoid OutOfMemory in the PDF renderer downstream.
 */
@Singleton
class NotePhotoStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val dir: File
        get() = File(context.filesDir, DIR_NAME).apply { mkdirs() }

    suspend fun save(uri: Uri): String? = withContext(Dispatchers.IO) {
        val bitmap = decodeDownsampled(uri, MAX_EDGE) ?: return@withContext null
        val rotated = applyExifRotation(uri, bitmap)
        val outFile = File(dir, "${UUID.randomUUID()}.jpg")
        FileOutputStream(outFile).use { os ->
            rotated.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, os)
        }
        if (rotated !== bitmap) bitmap.recycle()
        rotated.recycle()
        outFile.absolutePath
    }

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }

    private fun decodeDownsampled(uri: Uri, maxEdge: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > maxEdge || bounds.outHeight / sample > maxEdge) {
            sample *= 2
        }
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, opts)
        }
    }

    private fun applyExifRotation(uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            } ?: ExifInterface.ORIENTATION_NORMAL
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val rotation = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        if (rotation == 0f) return bitmap
        val matrix = Matrix().apply { postRotate(rotation) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    companion object {
        const val DIR_NAME = "course_notes"
        private const val MAX_EDGE = 1600
        private const val JPEG_QUALITY = 85
    }
}
