package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CloudBackupManager {
    private const val TAG = "CloudBackupManager"
    private const val BACKUP_DOC_PATH = "latest"

    // Check if Firebase is configured and available at runtime
    fun isFirebaseConfigured(context: Context): Boolean {
        return try {
            val apps = FirebaseApp.getApps(context)
            apps.isNotEmpty()
        } catch (e: Exception) {
            Log.w(TAG, "Firebase is not configured or missing google-services.json: ${e.message}")
            false
        }
    }

    /**
     * Backup all data (Peers, Messages, Settings) to the cloud database.
     * Fallbacks to Simulated Cloud Storage if Firebase is not configured.
     */
    suspend fun backupData(context: Context, repository: AppRepository, email: String): Result<Long> {
        return try {
            val timestamp = System.currentTimeMillis()
            val settings = repository.getSettingsDirect()
            val peers = repository.getAllPeersDirect()
            val messages = repository.getAllMessagesDirect()

            // Construct JSON Payload
            val payloadJson = buildBackupJson(email, settings.displayName, timestamp, peers, messages, settings)
            val useRealFirebase = isFirebaseConfigured(context)

            if (useRealFirebase) {
                // Real Firestore Backup
                val db = FirebaseFirestore.getInstance()
                val backupData = hashMapOf(
                    "email" to email,
                    "displayName" to settings.displayName,
                    "timestamp" to timestamp,
                    "payload" to payloadJson,
                    "deviceType" to "Android"
                )
                db.collection("users")
                    .document(email)
                    .collection("backups")
                    .document(BACKUP_DOC_PATH)
                    .set(backupData)
                    .await()
                
                Log.d(TAG, "Successful Firestore cloud backup for $email")
            } else {
                // Simulated Cloud Database Backup (Writes to private cache dir mimicking cloud server)
                saveSimulatedCloudBackup(context, email, payloadJson)
                Log.d(TAG, "Successful Simulated cloud backup for $email")
            }

            // Update user settings backup info
            val updatedSettings = settings.copy(
                lastBackupTime = timestamp,
                backupCount = settings.backupCount + 1
            )
            repository.updateUserSettings(updatedSettings)

            Result.success(timestamp)
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Restore data (Peers, Messages, Settings) from the cloud database.
     * Fallbacks to Simulated Cloud Storage if Firebase is not configured.
     */
    suspend fun restoreData(context: Context, repository: AppRepository, email: String): Result<Boolean> {
        return try {
            val useRealFirebase = isFirebaseConfigured(context)
            val payloadJson: String?

            if (useRealFirebase) {
                // Real Firestore Restore
                val db = FirebaseFirestore.getInstance()
                val document = db.collection("users")
                    .document(email)
                    .collection("backups")
                    .document(BACKUP_DOC_PATH)
                    .get()
                    .await()

                if (!document.exists()) {
                    return Result.failure(Exception("لا توجد نسخ احتياطية سحابية لهذا الحساب."))
                }
                payloadJson = document.getString("payload")
            } else {
                // Simulated Cloud Database Restore
                payloadJson = getSimulatedCloudBackup(context, email)
            }

            if (payloadJson.isNullOrEmpty()) {
                return Result.failure(Exception("لم يتم العثور على أي بيانات في النسخة الاحتياطية."))
            }

            // Restore elements to database
            parseAndRestoreBackup(repository, payloadJson)
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Restore failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Check if a backup exists for this account email.
     */
    suspend fun checkBackupExists(context: Context, email: String): Result<Boolean> {
        return try {
            val useRealFirebase = isFirebaseConfigured(context)
            if (useRealFirebase) {
                val db = FirebaseFirestore.getInstance()
                val document = db.collection("users")
                    .document(email)
                    .collection("backups")
                    .document(BACKUP_DOC_PATH)
                    .get()
                    .await()
                Result.success(document.exists())
            } else {
                val file = File(getSimulatedCloudDir(context), "${email.lowercase()}_cloud_backup.json")
                Result.success(file.exists())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Helper: Build a JSON string containing all data
    private fun buildBackupJson(
        email: String,
        displayName: String,
        timestamp: Long,
        peers: List<PeerDevice>,
        messages: List<ChatMessage>,
        settings: UserSettings
    ): String {
        val root = JSONObject()
        root.put("email", email)
        root.put("displayName", displayName)
        root.put("timestamp", timestamp)

        // Serialize Settings
        val settingsJson = JSONObject()
        settingsJson.put("displayName", settings.displayName)
        settingsJson.put("darkMode", settings.darkMode)
        settingsJson.put("powerSavingMode", settings.powerSavingMode)
        settingsJson.put("notificationsEnabled", settings.notificationsEnabled)
        settingsJson.put("callAlertsEnabled", settings.callAlertsEnabled)
        settingsJson.put("privacyRequireApproval", settings.privacyRequireApproval)
        settingsJson.put("profilePictureUrl", settings.profilePictureUrl)
        root.put("settings", settingsJson)

        // Serialize Peers
        val peersArray = JSONArray()
        for (peer in peers) {
            val peerJson = JSONObject()
            peerJson.put("macAddress", peer.macAddress)
            peerJson.put("name", peer.name)
            peerJson.put("nickname", peer.nickname ?: JSONObject.NULL)
            peerJson.put("publicKey", peer.publicKey ?: JSONObject.NULL)
            peerJson.put("trustLevel", peer.trustLevel)
            peerJson.put("isOnline", peer.isOnline)
            peerJson.put("lastSeen", peer.lastSeen)
            peersArray.put(peerJson)
        }
        root.put("peers", peersArray)

        // Serialize Messages
        val messagesArray = JSONArray()
        for (msg in messages) {
            val msgJson = JSONObject()
            msgJson.put("peerMacAddress", msg.peerMacAddress)
            msgJson.put("text", msg.text)
            msgJson.put("iv", msg.iv)
            msgJson.put("encryptedAesKey", msg.encryptedAesKey ?: JSONObject.NULL)
            msgJson.put("isIncoming", msg.isIncoming)
            msgJson.put("timestamp", msg.timestamp)
            msgJson.put("type", msg.type)
            msgJson.put("status", msg.status)
            msgJson.put("attachmentPath", msg.attachmentPath ?: JSONObject.NULL)
            msgJson.put("attachmentName", msg.attachmentName ?: JSONObject.NULL)
            msgJson.put("attachmentSize", msg.attachmentSize)
            msgJson.put("transferProgress", msg.transferProgress)
            msgJson.put("callDurationSeconds", msg.callDurationSeconds)
            msgJson.put("callType", msg.callType ?: JSONObject.NULL)
            msgJson.put("callStatus", msg.callStatus ?: JSONObject.NULL)
            messagesArray.put(msgJson)
        }
        root.put("messages", messagesArray)

        return root.toString()
    }

    // Helper: Parse backup JSON and insert into DB
    private suspend fun parseAndRestoreBackup(repository: AppRepository, jsonStr: String) {
        val root = JSONObject(jsonStr)
        
        // 1. Restore Settings
        if (root.has("settings")) {
            val currentSettings = repository.getSettingsDirect()
            val settingsJson = root.getJSONObject("settings")
            val restoredSettings = currentSettings.copy(
                displayName = settingsJson.optString("displayName", currentSettings.displayName),
                darkMode = settingsJson.optString("darkMode", currentSettings.darkMode),
                powerSavingMode = settingsJson.optBoolean("powerSavingMode", currentSettings.powerSavingMode),
                notificationsEnabled = settingsJson.optBoolean("notificationsEnabled", currentSettings.notificationsEnabled),
                callAlertsEnabled = settingsJson.optBoolean("callAlertsEnabled", currentSettings.callAlertsEnabled),
                privacyRequireApproval = settingsJson.optBoolean("privacyRequireApproval", currentSettings.privacyRequireApproval),
                profilePictureUrl = settingsJson.optString("profilePictureUrl", ""),
                isLoggedIn = true,
                accountEmail = root.optString("email", ""),
                lastBackupTime = root.optLong("timestamp", System.currentTimeMillis())
            )
            repository.updateUserSettings(restoredSettings)
        }

        // 2. Restore Peers
        if (root.has("peers")) {
            val peersArray = root.getJSONArray("peers")
            val peersList = mutableListOf<PeerDevice>()
            for (i in 0 until peersArray.length()) {
                val p = peersArray.getJSONObject(i)
                peersList.add(
                    PeerDevice(
                        macAddress = p.getString("macAddress"),
                        name = p.getString("name"),
                        nickname = if (p.isNull("nickname")) null else p.getString("nickname"),
                        publicKey = if (p.isNull("publicKey")) null else p.getString("publicKey"),
                        trustLevel = p.optInt("trustLevel", 1),
                        isOnline = p.optBoolean("isOnline", false),
                        lastSeen = p.optLong("lastSeen", System.currentTimeMillis())
                    )
                )
            }
            if (peersList.isNotEmpty()) {
                repository.insertPeers(peersList)
            }
        }

        // 3. Restore Messages
        if (root.has("messages")) {
            val msgArray = root.getJSONArray("messages")
            val msgList = mutableListOf<ChatMessage>()
            for (i in 0 until msgArray.length()) {
                val m = msgArray.getJSONObject(i)
                msgList.add(
                    ChatMessage(
                        peerMacAddress = m.getString("peerMacAddress"),
                        text = m.getString("text"),
                        iv = m.optString("iv", ""),
                        encryptedAesKey = if (m.isNull("encryptedAesKey")) null else m.getString("encryptedAesKey"),
                        isIncoming = m.getBoolean("isIncoming"),
                        timestamp = m.getLong("timestamp"),
                        type = m.optString("type", "TEXT"),
                        status = m.optString("status", "SENT"),
                        attachmentPath = if (m.isNull("attachmentPath")) null else m.getString("attachmentPath"),
                        attachmentName = if (m.isNull("attachmentName")) null else m.getString("attachmentName"),
                        attachmentSize = m.optLong("attachmentSize", 0),
                        transferProgress = m.optInt("transferProgress", 100),
                        callDurationSeconds = m.optLong("callDurationSeconds", 0),
                        callType = if (m.isNull("callType")) null else m.getString("callType"),
                        callStatus = if (m.isNull("callStatus")) null else m.getString("callStatus")
                    )
                )
            }
            if (msgList.isNotEmpty()) {
                repository.insertMessages(msgList)
            }
        }
    }

    // Simulated Cloud File operations
    private fun getSimulatedCloudDir(context: Context): File {
        val dir = File(context.filesDir, "simulated_cloud_database")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun saveSimulatedCloudBackup(context: Context, email: String, payload: String) {
        val file = File(getSimulatedCloudDir(context), "${email.lowercase()}_cloud_backup.json")
        file.writeText(payload)
    }

    private fun getSimulatedCloudBackup(context: Context, email: String): String? {
        val file = File(getSimulatedCloudDir(context), "${email.lowercase()}_cloud_backup.json")
        return if (file.exists()) file.readText() else null
    }
}
