package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "peer_devices")
data class PeerDevice(
    @PrimaryKey val macAddress: String,
    val name: String,
    val nickname: String? = null,
    val publicKey: String? = null,
    val trustLevel: Int = 1, // 0 = Untrusted, 1 = Trusted, 2 = Blocked
    val isOnline: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis(),
    val rssi: Int = -100
) {
    fun getDisplayName(): String = nickname ?: name
}

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val peerMacAddress: String,
    val text: String,
    val iv: String = "",
    val encryptedAesKey: String? = null,
    val isIncoming: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "TEXT", // "TEXT", "FILE", "CALL"
    val status: String = "SENT", // "SENDING", "SENT", "DELIVERED", "READ", "FAILED"
    
    // File attributes (if type == "FILE")
    val attachmentPath: String? = null,
    val attachmentName: String? = null,
    val attachmentSize: Long = 0,
    val transferProgress: Int = 100, // 0 to 100
    
    // Call attributes (if type == "CALL")
    val callDurationSeconds: Long = 0,
    val callType: String? = null, // "VOICE", "VIDEO"
    val callStatus: String? = null // "MISSED", "INCOMING", "OUTGOING", "DECLINED"
)

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val id: Int = 1,
    val displayName: String = "مستخدم تواصل",
    val darkMode: String = "AUTO", // "AUTO", "DARK", "LIGHT"
    val powerSavingMode: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val callAlertsEnabled: Boolean = true,
    val privacyRequireApproval: Boolean = false,
    val enableSimulation: Boolean = false,
    
    // Account and Backup attributes
    val isLoggedIn: Boolean = false,
    val accountEmail: String = "",
    val accountPasswordHash: String = "",
    val profilePictureUrl: String = "",
    val lastBackupTime: Long = 0,
    val backupCount: Int = 0
)
