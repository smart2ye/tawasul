package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PeerDao {
    @Query("SELECT * FROM peer_devices ORDER BY lastSeen DESC")
    fun getAllPeers(): Flow<List<PeerDevice>>

    @Query("SELECT * FROM peer_devices WHERE macAddress = :macAddress")
    fun getPeer(macAddress: String): Flow<PeerDevice?>

    @Query("SELECT * FROM peer_devices WHERE macAddress = :macAddress")
    suspend fun getPeerDirect(macAddress: String): PeerDevice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeer(peer: PeerDevice)

    @Query("SELECT * FROM peer_devices")
    suspend fun getAllPeersDirect(): List<PeerDevice>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeers(peers: List<PeerDevice>)

    @Query("UPDATE peer_devices SET isOnline = :isOnline, lastSeen = :timestamp WHERE macAddress = :macAddress")
    suspend fun updatePeerOnlineStatus(macAddress: String, isOnline: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE peer_devices SET publicKey = :key WHERE macAddress = :macAddress")
    suspend fun updatePeerKey(macAddress: String, key: String)

    @Query("UPDATE peer_devices SET nickname = :nickname WHERE macAddress = :macAddress")
    suspend fun updatePeerNickname(macAddress: String, nickname: String?)

    @Query("UPDATE peer_devices SET trustLevel = :trustLevel WHERE macAddress = :macAddress")
    suspend fun updatePeerTrustLevel(macAddress: String, trustLevel: Int)

    @Delete
    suspend fun deletePeer(peer: PeerDevice)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM chat_messages WHERE peerMacAddress = :peerMac ORDER BY timestamp ASC")
    fun getMessagesForPeer(peerMac: String): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_messages WHERE type = 'CALL' ORDER BY timestamp DESC")
    fun getCallsHistory(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("SELECT * FROM chat_messages")
    suspend fun getAllMessagesDirect(): List<ChatMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<ChatMessage>)

    @Query("UPDATE chat_messages SET transferProgress = :progress WHERE id = :id")
    suspend fun updateTransferProgress(id: Long, progress: Int)

    @Query("UPDATE chat_messages SET status = :status WHERE id = :id")
    suspend fun updateMessageStatus(id: Long, status: String)

    @Query("DELETE FROM chat_messages WHERE peerMacAddress = :peerMac")
    suspend fun deleteChatHistory(peerMac: String)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun getSettingsFlow(): Flow<UserSettings?>

    @Query("SELECT * FROM user_settings WHERE id = 1")
    suspend fun getSettingsDirect(): UserSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: UserSettings)

    @Query("UPDATE user_settings SET displayName = :name WHERE id = 1")
    suspend fun updateDisplayName(name: String)

    @Query("UPDATE user_settings SET darkMode = :mode WHERE id = 1")
    suspend fun updateDarkMode(mode: String)

    @Query("UPDATE user_settings SET powerSavingMode = :enabled WHERE id = 1")
    suspend fun updatePowerSaving(enabled: Boolean)

    @Query("UPDATE user_settings SET notificationsEnabled = :enabled WHERE id = 1")
    suspend fun updateNotifications(enabled: Boolean)

    @Query("UPDATE user_settings SET callAlertsEnabled = :enabled WHERE id = 1")
    suspend fun updateCallAlerts(enabled: Boolean)

    @Query("UPDATE user_settings SET privacyRequireApproval = :enabled WHERE id = 1")
    suspend fun updatePrivacyApproval(enabled: Boolean)

    @Query("UPDATE user_settings SET enableSimulation = :enabled WHERE id = 1")
    suspend fun updateEnableSimulation(enabled: Boolean)
}
