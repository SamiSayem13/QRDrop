package com.qrdrop.phototransfer.saver

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GallerySaver {

    suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Result<String> = withContext(Dispatchers.IO) {
        try {
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val filename = "QRTransfer_$timestamp.png"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/QRPhotoTransfer")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val resolver = context.contentResolver
                val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext Result.failure(Exception("Failed to create MediaStore record"))

                resolver.openOutputStream(imageUri)?.use { outputStream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                        return@withContext Result.failure(Exception("Failed to encode image to PNG"))
                    }
                }

                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)

                Result.success(filename)
            } else {
                @Suppress("DEPRECATION")
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val albumDir = File(picturesDir, "QRPhotoTransfer")
                if (!albumDir.exists()) {
                    albumDir.mkdirs()
                }

                val imageFile = File(albumDir, filename)
                FileOutputStream(imageFile).use { outputStream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)) {
                        return@withContext Result.failure(Exception("Failed to write image file"))
                    }
                }

                @Suppress("DEPRECATION")
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                }
                context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)

                Result.success(filename)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
