package com.drummer.speed.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object FileHelper {
    fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Boolean {
        val filename = "drum_stroke_${System.currentTimeMillis()}.jpg"
        var fos: OutputStream? = null
        var success = false

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/DrumStrokeCounter")
                }
                val contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                val uri = context.contentResolver.insert(contentUri, contentValues)
                if (uri != null) {
                    fos = context.contentResolver.openOutputStream(uri)
                }
            } else {
                val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString()
                val folder = File(imagesDir, "DrumStrokeCounter")
                folder.mkdirs()
                val imageFile = File(folder, filename)
                fos = FileOutputStream(imageFile)
                
                // Pemicu scan media agar muncul di galeri untuk versi lama
                val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                mediaScanIntent.data = Uri.fromFile(imageFile)
                context.sendBroadcast(mediaScanIntent)
            }

            fos?.use {
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
                success = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return success
    }

    fun saveBitmapToCache(context: Context, bitmap: Bitmap): File? {
        return try {
            val folder = context.externalCacheDir ?: context.cacheDir
            val imagesFolder = File(folder, "images")
            imagesFolder.mkdirs()
            val file = File(imagesFolder, "drum_result.jpg")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
