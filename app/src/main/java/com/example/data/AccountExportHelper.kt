package com.example.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

object AccountExportHelper {
    /**
     * Compiles user data, settings, and stats into a formatted JSON and writes
     * it directly to the system's "Downloads" folder.
     */
    fun exportAccountInfo(
        context: Context, 
        email: String, 
        displayName: String, 
        stats: Map<String, Any>
    ): Result<Uri> {
        return try {
            val root = JSONObject()
            root.put("app_name", "تواصل بلس | Tawasul Plus")
            root.put("export_timestamp", System.currentTimeMillis())
            
            val profile = JSONObject()
            profile.put("email", email)
            profile.put("display_name", displayName)
            root.put("user_profile", profile)
            
            val statistics = JSONObject()
            stats.forEach { (key, value) ->
                statistics.put(key, value)
            }
            root.put("statistics", statistics)

            // Convert to pretty-printed string
            val jsonString = root.toString(4)
            val filename = "tawasul_profile_info_${email.replace("@", "_").replace(".", "_")}.json"
            
            val uri: Uri?
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val resolver = context.contentResolver
                val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                uri = resolver.insert(collection, contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { os ->
                        os.write(jsonString.toByteArray())
                    }
                }
            } else {
                // Fallback for older Android APIs
                @Suppress("DEPRECATION")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val file = File(downloadsDir, filename)
                FileOutputStream(file).use { os ->
                    os.write(jsonString.toByteArray())
                }
                uri = Uri.fromFile(file)
            }
            
            if (uri != null) {
                Result.success(uri)
            } else {
                Result.failure(Exception("فشل في إنشاء الملف في مجلد التنزيلات."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
