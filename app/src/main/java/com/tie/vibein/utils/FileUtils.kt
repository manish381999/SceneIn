package com.tie.vibein.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object FileUtils {
    // This is now the updated, robust function
    fun getFileFromUri(context: Context, uri: Uri): File? {
        // --- NEW: Try to get a real filename and extension from the content provider ---
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)

        cursor?.moveToFirst()
        val originalFileName = nameIndex?.let { cursor.getString(it) } ?: "upload_file"
        cursor?.close()

        val tempFile = File(context.cacheDir, originalFileName)

        return try {
            // Copy the file content from the URI to our temporary file
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(tempFile)
            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}