package com.scenein.utils

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.*

object FileUtils {

    // You can keep your old getFileFromUri function if it's used elsewhere,
    // but we will use this new one for uploads.

    fun getMultipartBodyPartFromUri(context: Context, uri: Uri, partName: String): MultipartBody.Part? {
        return try {
            val contentResolver = context.contentResolver

            // 1. Get the MIME type (e.g., "image/jpeg")
            val mimeType = contentResolver.getType(uri) ?: "image/*"

            // 2. Get the file's bytes from the URI
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val fileBytes = inputStream.use { it.readBytes() }

            // 3. Create a RequestBody from the bytes
            val requestBody = fileBytes.toRequestBody(mimeType.toMediaTypeOrNull())

            // 4. Generate a filename. The server needs one, even if it's random.
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            val filename = "${UUID.randomUUID()}.$extension"

            // 5. Create and return the MultipartBody.Part
            MultipartBody.Part.createFormData(partName, filename, requestBody)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}