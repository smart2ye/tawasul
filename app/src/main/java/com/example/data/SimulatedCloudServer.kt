package com.example.data

import android.content.Context
import android.os.Build
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

object SimulatedCloudServer {
    private const val TAG = "SimulatedCloudServer"
    private const val CLOUD_DIR_NAME = "simulated_cloud_database"

    // Retrieve cloud directory
    private fun getCloudDir(context: Context): File {
        val dir = File(context.filesDir, CLOUD_DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    // Helper to read JSON file
    private fun readJsonFile(context: Context, filename: String): String {
        val file = File(getCloudDir(context), filename)
        if (!file.exists()) return ""
        return try {
            file.readText()
        } catch (e: Exception) {
            Log.e(TAG, "Error reading file $filename: ${e.message}")
            ""
        }
    }

    // Helper to write JSON file
    private fun writeJsonFile(context: Context, filename: String, content: String) {
        val file = File(getCloudDir(context), filename)
        try {
            file.writeText(content)
        } catch (e: Exception) {
            Log.e(TAG, "Error writing file $filename: ${e.message}")
        }
    }

    // Initialize default data (default admin, some demo support tickets, first app version)
    fun initializeDefaultsIfNeeded(context: Context) {
        // 1. Initial App Versions
        val versionsFile = File(getCloudDir(context), "app_versions.json")
        if (!versionsFile.exists()) {
            val arr = JSONArray()
            
            val v1 = JSONObject().apply {
                put("versionCode", 1)
                put("versionName", "1.0.0")
                put("releaseDate", "2026-05-15")
                put("changelog", "الإصدار التجريبي الأول لتطبيق تواصل بلس.\n- دعم المراسلة الفورية عبر البلوتوث.\n- تشفير المحادثات بنظام التشفير التلقائي.\n- وضع توفير الطاقة الذكي.")
                put("downloadUrl", "https://tawasulplus.com/downloads/tawasul-v1.0.apk")
                put("isCritical", false)
            }
            
            val v2 = JSONObject().apply {
                put("versionCode", 2)
                put("versionName", "1.1.0")
                put("releaseDate", "2026-07-01")
                put("changelog", "تحديث الأداء والأمان الجديد:\n- تحسين سرعة اكتشاف الرادار للبلوتوث بنسبة 40%.\n- حل مشكلة انقطاع الصوت في المكالمات.\n- إضافة خيار تشفير النسخ الاحتياطية السحابية بالكامل.")
                put("downloadUrl", "https://tawasulplus.com/downloads/tawasul-v1.1.apk")
                put("isCritical", true)
            }

            arr.put(v1)
            arr.put(v2)
            writeJsonFile(context, "app_versions.json", arr.toString(4))
        }

        // 2. Initial Users (with some mockup users)
        val usersFile = File(getCloudDir(context), "users.json")
        if (!usersFile.exists()) {
            val arr = JSONArray()
            val u1 = JSONObject().apply {
                put("email", "waelmahdly531@gmail.com")
                put("name", "وائل المهدلي")
                put("status", "ACTIVE")
                put("createdTime", System.currentTimeMillis() - 30 * 24 * 3600 * 1000L) // 30 days ago
            }
            val u2 = JSONObject().apply {
                put("email", "ahmed.ali@example.com")
                put("name", "أحمد علي")
                put("status", "ACTIVE")
                put("createdTime", System.currentTimeMillis() - 10 * 24 * 3600 * 1000L)
            }
            val u3 = JSONObject().apply {
                put("email", "developer@tawasul.com")
                put("name", "مطور تواصل بلس")
                put("status", "ACTIVE")
                put("createdTime", System.currentTimeMillis() - 40 * 24 * 3600 * 1000L)
            }
            arr.put(u1)
            arr.put(u2)
            arr.put(u3)
            writeJsonFile(context, "users.json", arr.toString(4))
        }

        // 3. Initial Support Tickets
        val ticketsFile = File(getCloudDir(context), "support_tickets.json")
        if (!ticketsFile.exists()) {
            val arr = JSONArray()
            val t1 = JSONObject().apply {
                put("id", "TCK-1092")
                put("email", "ahmed.ali@example.com")
                put("title", "مشكلة في استعادة النسخة الاحتياطية")
                put("description", "عند محاولة الضغط على استعادة المحادثات، يظهر لي خطأ في فك التشفير. هل يمكنكم المساعدة؟")
                put("category", "BACKUP")
                put("status", "IN_PROGRESS")
                put("createdAt", System.currentTimeMillis() - 2 * 24 * 3600 * 1000L)
                
                val msgs = JSONArray().apply {
                    put(JSONObject().apply {
                        put("sender", "USER")
                        put("text", "عند محاولة الضغط على استعادة المحادثات، يظهر لي خطأ في فك التشفير. هل يمكنكم المساعدة؟")
                        put("timestamp", System.currentTimeMillis() - 2 * 24 * 3600 * 1000L)
                    })
                    put(JSONObject().apply {
                        put("sender", "ADMIN")
                        put("text", "أهلاً بك أحمد، يرجى التأكد من أنك تستخدم نفس البريد الإلكتروني الذي قمت بالنسخ الاحتياطي منه، وتأكد من جودة اتصال الإنترنت.")
                        put("timestamp", System.currentTimeMillis() - 1 * 24 * 3600 * 1000L)
                    })
                }
                put("messages", msgs)
            }

            val t2 = JSONObject().apply {
                put("id", "TCK-1543")
                put("email", "waelmahdly531@gmail.com")
                put("title", "اقتراح لدعم رادار الواي فاي المباشر")
                put("description", "التطبيق رائع جداً، ولكن أرجو إضافة دعم الواي فاي المباشر Wi-Fi Direct لزيادة سرعة نقل الملفات بدلاً من البلوتوث.")
                put("category", "FEATURE_REQUEST")
                put("status", "RESOLVED")
                put("createdAt", System.currentTimeMillis() - 5 * 24 * 3600 * 1000L)
                
                val msgs = JSONArray().apply {
                    put(JSONObject().apply {
                        put("sender", "USER")
                        put("text", "التطبيق رائع جداً، ولكن أرجو إضافة دعم الواي فاي المباشر Wi-Fi Direct لزيادة سرعة نقل الملفات بدلاً من البلوتوث.")
                        put("timestamp", System.currentTimeMillis() - 5 * 24 * 3600 * 1000L)
                    })
                    put(JSONObject().apply {
                        put("sender", "ADMIN")
                        put("text", "مرحباً وائل! اقتراح ممتاز ونعمل حالياً على تطوير ميزة تواصل عبر Wi-Fi LAN والـ Wi-Fi Direct لتكون متوفرة في الإصدار القادم إن شاء الله.")
                        put("timestamp", System.currentTimeMillis() - 4 * 24 * 3600 * 1000L)
                    })
                }
                put("messages", msgs)
            }

            arr.put(t1)
            arr.put(t2)
            writeJsonFile(context, "support_tickets.json", arr.toString(4))
        }

        // 4. Initial Connected Devices/Sessions
        val sessionsFile = File(getCloudDir(context), "sessions.json")
        if (!sessionsFile.exists()) {
            val arr = JSONArray()
            val s1 = JSONObject().apply {
                put("id", UUID.randomUUID().toString().substring(0, 8))
                put("email", "waelmahdly531@gmail.com")
                put("deviceName", "${Build.MANUFACTURER} ${Build.MODEL}")
                put("deviceType", "Android")
                put("loginTime", System.currentTimeMillis() - 2 * 3600 * 1000L)
                put("ipAddress", "192.168.1.45")
                put("isActive", true)
            }
            val s2 = JSONObject().apply {
                put("id", UUID.randomUUID().toString().substring(0, 8))
                put("email", "waelmahdly531@gmail.com")
                put("deviceName", "Chrome 124 (Windows 11)")
                put("deviceType", "Web Companion")
                put("loginTime", System.currentTimeMillis() - 24 * 3600 * 1000L)
                put("ipAddress", "82.145.4.12")
                put("isActive", true)
            }
            arr.put(s1)
            arr.put(s2)
            writeJsonFile(context, "sessions.json", arr.toString(4))
        }
    }

    // ================== USER ACCOUNTS & SESSIONS ==================

    fun registerOrUpdateUserInCloud(context: Context, email: String, name: String) {
        try {
            val content = readJsonFile(context, "users.json")
            val arr = if (content.isEmpty()) JSONArray() else JSONArray(content)
            
            var found = false
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.getString("email").equals(email, ignoreCase = true)) {
                    obj.put("name", name)
                    found = true
                    break
                }
            }
            if (!found) {
                val newUser = JSONObject().apply {
                    put("email", email)
                    put("name", name)
                    put("status", "ACTIVE")
                    put("createdTime", System.currentTimeMillis())
                }
                arr.put(newUser)
            }
            writeJsonFile(context, "users.json", arr.toString(4))
            
            // Add initial active session
            addSession(context, email, "${Build.MANUFACTURER} ${Build.MODEL}", "Android")
        } catch (e: Exception) {
            Log.e(TAG, "Error registering user in cloud: ${e.message}")
        }
    }

    fun getUsers(context: Context): List<CloudUser> {
        val result = mutableListOf<CloudUser>()
        try {
            val content = readJsonFile(context, "users.json")
            if (content.isEmpty()) return result
            val arr = JSONArray(content)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                result.add(
                    CloudUser(
                        email = obj.getString("email"),
                        name = obj.getString("name"),
                        status = obj.optString("status", "ACTIVE"),
                        createdTime = obj.optLong("createdTime", System.currentTimeMillis())
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing users list: ${e.message}")
        }
        return result
    }

    fun updateUserStatus(context: Context, email: String, status: String) {
        try {
            val content = readJsonFile(context, "users.json")
            if (content.isEmpty()) return
            val arr = JSONArray(content)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.getString("email").equals(email, ignoreCase = true)) {
                    obj.put("status", status)
                    break
                }
            }
            writeJsonFile(context, "users.json", arr.toString(4))
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user status: ${e.message}")
        }
    }

    fun addSession(context: Context, email: String, deviceName: String, deviceType: String) {
        try {
            val content = readJsonFile(context, "sessions.json")
            val arr = if (content.isEmpty()) JSONArray() else JSONArray(content)
            
            val newSession = JSONObject().apply {
                put("id", UUID.randomUUID().toString().substring(0, 8))
                put("email", email)
                put("deviceName", deviceName)
                put("deviceType", deviceType)
                put("loginTime", System.currentTimeMillis())
                put("ipAddress", if (deviceType == "Android") "192.168.1." + (10..254).random() else "82.145." + (1..254).random() + "." + (1..254).random())
                put("isActive", true)
            }
            arr.put(newSession)
            writeJsonFile(context, "sessions.json", arr.toString(4))
        } catch (e: Exception) {
            Log.e(TAG, "Error adding session: ${e.message}")
        }
    }

    fun getSessions(context: Context, email: String): List<CloudSession> {
        val result = mutableListOf<CloudSession>()
        try {
            val content = readJsonFile(context, "sessions.json")
            if (content.isEmpty()) return result
            val arr = JSONArray(content)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.getString("email").equals(email, ignoreCase = true)) {
                    result.add(
                        CloudSession(
                            id = obj.getString("id"),
                            email = obj.getString("email"),
                            deviceName = obj.getString("deviceName"),
                            deviceType = obj.getString("deviceType"),
                            loginTime = obj.getLong("loginTime"),
                            ipAddress = obj.getString("ipAddress"),
                            isActive = obj.getBoolean("isActive")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error listing sessions: ${e.message}")
        }
        return result
    }

    fun revokeSession(context: Context, sessionId: String) {
        try {
            val content = readJsonFile(context, "sessions.json")
            if (content.isEmpty()) return
            val arr = JSONArray(content)
            val updatedArr = JSONArray()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.getString("id") == sessionId) {
                    // Mark inactive or skip
                    obj.put("isActive", false)
                }
                updatedArr.put(obj)
            }
            writeJsonFile(context, "sessions.json", updatedArr.toString(4))
        } catch (e: Exception) {
            Log.e(TAG, "Error revoking session: ${e.message}")
        }
    }

    // ================== SUPPORT TICKETS ==================

    fun createSupportTicket(context: Context, email: String, title: String, description: String, category: String): String {
        val ticketId = "TCK-" + (1000..9999).random()
        try {
            val content = readJsonFile(context, "support_tickets.json")
            val arr = if (content.isEmpty()) JSONArray() else JSONArray(content)
            
            val newTicket = JSONObject().apply {
                put("id", ticketId)
                put("email", email)
                put("title", title)
                put("description", description)
                put("category", category)
                put("status", "OPEN")
                put("createdAt", System.currentTimeMillis())
                
                val msgs = JSONArray().apply {
                    put(JSONObject().apply {
                        put("sender", "USER")
                        put("text", description)
                        put("timestamp", System.currentTimeMillis())
                    })
                }
                put("messages", msgs)
            }
            arr.put(newTicket)
            writeJsonFile(context, "support_tickets.json", arr.toString(4))
        } catch (e: Exception) {
            Log.e(TAG, "Error creating support ticket: ${e.message}")
        }
        return ticketId
    }

    fun getTicketsForUser(context: Context, email: String): List<SupportTicket> {
        val result = mutableListOf<SupportTicket>()
        try {
            val content = readJsonFile(context, "support_tickets.json")
            if (content.isEmpty()) return result
            val arr = JSONArray(content)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.getString("email").equals(email, ignoreCase = true)) {
                    result.add(parseTicketJson(obj))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading user tickets: ${e.message}")
        }
        return result
    }

    fun getAllTickets(context: Context): List<SupportTicket> {
        val result = mutableListOf<SupportTicket>()
        try {
            val content = readJsonFile(context, "support_tickets.json")
            if (content.isEmpty()) return result
            val arr = JSONArray(content)
            for (i in 0 until arr.length()) {
                result.add(parseTicketJson(arr.getJSONObject(i)))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading all tickets: ${e.message}")
        }
        return result
    }

    private fun parseTicketJson(obj: JSONObject): SupportTicket {
        val msgsList = mutableListOf<TicketMessage>()
        val msgsArr = obj.getJSONArray("messages")
        for (j in 0 until msgsArr.length()) {
            val m = msgsArr.getJSONObject(j)
            msgsList.add(
                TicketMessage(
                    sender = m.getString("sender"),
                    text = m.getString("text"),
                    timestamp = m.getLong("timestamp")
                )
            )
        }
        return SupportTicket(
            id = obj.getString("id"),
            email = obj.getString("email"),
            title = obj.getString("title"),
            description = obj.getString("description"),
            category = obj.getString("category"),
            status = obj.getString("status"),
            createdAt = obj.getLong("createdAt"),
            messages = msgsList
        )
    }

    fun replyToTicket(context: Context, ticketId: String, sender: String, text: String, newStatus: String? = null) {
        try {
            val content = readJsonFile(context, "support_tickets.json")
            if (content.isEmpty()) return
            val arr = JSONArray(content)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                if (obj.getString("id") == ticketId) {
                    val msgsArr = obj.getJSONArray("messages")
                    val newMsg = JSONObject().apply {
                        put("sender", sender)
                        put("text", text)
                        put("timestamp", System.currentTimeMillis())
                    }
                    msgsArr.put(newMsg)
                    
                    if (newStatus != null) {
                        obj.put("status", newStatus)
                    } else if (sender == "ADMIN") {
                        obj.put("status", "IN_PROGRESS")
                    }
                    break
                }
            }
            writeJsonFile(context, "support_tickets.json", arr.toString(4))
        } catch (e: Exception) {
            Log.e(TAG, "Error replying to ticket: ${e.message}")
        }
    }

    // ================== APP UPDATES & VERSION CONTROL ==================

    fun getAppVersions(context: Context): List<AppVersion> {
        val result = mutableListOf<AppVersion>()
        try {
            val content = readJsonFile(context, "app_versions.json")
            if (content.isEmpty()) return result
            val arr = JSONArray(content)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                result.add(
                    AppVersion(
                        versionCode = obj.getInt("versionCode"),
                        versionName = obj.getString("versionName"),
                        releaseDate = obj.getString("releaseDate"),
                        changelog = obj.getString("changelog"),
                        downloadUrl = obj.getString("downloadUrl"),
                        isCritical = obj.getBoolean("isCritical")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing app versions: ${e.message}")
        }
        return result
    }

    fun publishNewVersion(context: Context, versionName: String, changelog: String, isCritical: Boolean): AppVersion {
        val versions = getAppVersions(context)
        val nextCode = (versions.maxOfOrNull { it.versionCode } ?: 0) + 1
        
        val newVer = AppVersion(
            versionCode = nextCode,
            versionName = versionName,
            releaseDate = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date()),
            changelog = changelog,
            downloadUrl = "https://tawasulplus.com/downloads/tawasul-v$versionName.apk",
            isCritical = isCritical
        )

        try {
            val content = readJsonFile(context, "app_versions.json")
            val arr = if (content.isEmpty()) JSONArray() else JSONArray(content)
            
            val obj = JSONObject().apply {
                put("versionCode", newVer.versionCode)
                put("versionName", newVer.versionName)
                put("releaseDate", newVer.releaseDate)
                put("changelog", newVer.changelog)
                put("downloadUrl", newVer.downloadUrl)
                put("isCritical", newVer.isCritical)
            }
            arr.put(obj)
            writeJsonFile(context, "app_versions.json", arr.toString(4))
            
            // Log simulated public notification broadcast
            sendGeneralNotification(context, "تحديث جديد متوفر!", "تحديث تواصل بلس رقم ${newVer.versionName} متوفر الآن للتحميل بميزات جديدة.")
        } catch (e: Exception) {
            Log.e(TAG, "Error publishing version: ${e.message}")
        }
        return newVer
    }

    // ================== FEEDBACK & CRASH REPORTS ==================

    fun submitFeedbackOrCrash(context: Context, email: String, type: String, content: String, deviceInfo: String = "") {
        try {
            val filename = "feedback_reports.json"
            val fileContent = readJsonFile(context, filename)
            val arr = if (fileContent.isEmpty()) JSONArray() else JSONArray(fileContent)
            
            val report = JSONObject().apply {
                put("id", "REP-" + (10000..99999).random())
                put("email", email)
                put("type", type) // "FEEDBACK" or "CRASH"
                put("content", content)
                put("deviceInfo", deviceInfo.ifBlank { "${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})" })
                put("timestamp", System.currentTimeMillis())
            }
            arr.put(report)
            writeJsonFile(context, filename, arr.toString(4))
        } catch (e: Exception) {
            Log.e(TAG, "Error submitting feedback/crash: ${e.message}")
        }
    }

    fun getAllFeedbackReports(context: Context): List<CloudFeedback> {
        val result = mutableListOf<CloudFeedback>()
        try {
            val content = readJsonFile(context, "feedback_reports.json")
            if (content.isEmpty()) return result
            val arr = JSONArray(content)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                result.add(
                    CloudFeedback(
                        id = obj.getString("id"),
                        email = obj.getString("email"),
                        type = obj.getString("type"),
                        content = obj.getString("content"),
                        deviceInfo = obj.getString("deviceInfo"),
                        timestamp = obj.getLong("timestamp")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing feedback reports: ${e.message}")
        }
        return result
    }

    // ================== GENERAL PUSH NOTIFICATIONS ==================

    fun sendGeneralNotification(context: Context, title: String, message: String) {
        try {
            val filename = "general_notifications.json"
            val fileContent = readJsonFile(context, filename)
            val arr = if (fileContent.isEmpty()) JSONArray() else JSONArray(fileContent)
            
            val notification = JSONObject().apply {
                put("id", "NTF-" + (1000..9999).random())
                put("title", title)
                put("message", message)
                put("timestamp", System.currentTimeMillis())
            }
            arr.put(notification)
            writeJsonFile(context, filename, arr.toString(4))
        } catch (e: Exception) {
            Log.e(TAG, "Error broadcasting general notification: ${e.message}")
        }
    }

    fun getGeneralNotifications(context: Context): List<CloudNotification> {
        val result = mutableListOf<CloudNotification>()
        try {
            val content = readJsonFile(context, "general_notifications.json")
            if (content.isEmpty()) return result
            val arr = JSONArray(content)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                result.add(
                    CloudNotification(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        message = obj.getString("message"),
                        timestamp = obj.getLong("timestamp")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing notifications list: ${e.message}")
        }
        return result.sortedByDescending { it.timestamp }
    }
}

// Data models for Cloud Simulation
data class CloudUser(val email: String, val name: String, val status: String, val createdTime: Long)
data class CloudSession(val id: String, val email: String, val deviceName: String, val deviceType: String, val loginTime: Long, val ipAddress: String, val isActive: Boolean)
data class SupportTicket(val id: String, val email: String, val title: String, val description: String, val category: String, val status: String, val createdAt: Long, val messages: List<TicketMessage>)
data class TicketMessage(val sender: String, val text: String, val timestamp: Long)
data class AppVersion(val versionCode: Int, val versionName: String, val releaseDate: String, val changelog: String, val downloadUrl: String, val isCritical: Boolean)
data class CloudFeedback(val id: String, val email: String, val type: String, val content: String, val deviceInfo: String, val timestamp: Long)
data class CloudNotification(val id: String, val title: String, val message: String, val timestamp: Long)
