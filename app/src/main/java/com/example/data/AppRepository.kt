package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class AppRepository(
    private val peerDao: PeerDao,
    private val messageDao: MessageDao,
    private val settingsDao: SettingsDao
) {
    val allPeers: Flow<List<PeerDevice>> = peerDao.getAllPeers()
    val allMessages: Flow<List<ChatMessage>> = messageDao.getAllMessages()
    val allCallsHistory: Flow<List<ChatMessage>> = messageDao.getCallsHistory()
    val settingsFlow: Flow<UserSettings?> = settingsDao.getSettingsFlow()

    // Query messages specifically for a peer
    fun getMessagesForPeer(peerMac: String): Flow<List<ChatMessage>> =
        messageDao.getMessagesForPeer(peerMac)

    // Direct access to settings
    suspend fun getSettingsDirect(): UserSettings {
        return settingsDao.getSettingsDirect() ?: UserSettings().also {
            settingsDao.insertSettings(it)
        }
    }

    // Direct access to peer details
    suspend fun getPeerDirect(mac: String): PeerDevice? {
        return peerDao.getPeerDirect(mac)
    }

    // Peer CRUD Operations
    suspend fun insertPeer(peer: PeerDevice) = peerDao.insertPeer(peer)
    
    suspend fun updatePeerOnlineStatus(mac: String, isOnline: Boolean) =
        peerDao.updatePeerOnlineStatus(mac, isOnline)

    suspend fun updatePeerKey(mac: String, key: String) =
        peerDao.updatePeerKey(mac, key)

    suspend fun updatePeerNickname(mac: String, nickname: String?) =
        peerDao.updatePeerNickname(mac, nickname)

    suspend fun updatePeerTrustLevel(mac: String, trustLevel: Int) =
        peerDao.updatePeerTrustLevel(mac, trustLevel)

    suspend fun deletePeer(peer: PeerDevice) = peerDao.deletePeer(peer)

    suspend fun getAllPeersDirect(): List<PeerDevice> = peerDao.getAllPeersDirect()
    suspend fun insertPeers(peers: List<PeerDevice>) = peerDao.insertPeers(peers)

    // Message Operations
    suspend fun getAllMessagesDirect(): List<ChatMessage> = messageDao.getAllMessagesDirect()
    suspend fun insertMessages(messages: List<ChatMessage>) = messageDao.insertMessages(messages)
    suspend fun insertMessage(message: ChatMessage): Long =
        messageDao.insertMessage(message)

    suspend fun updateTransferProgress(id: Long, progress: Int) =
        messageDao.updateTransferProgress(id, progress)

    suspend fun updateMessageStatus(id: Long, status: String) =
        messageDao.updateMessageStatus(id, status)

    suspend fun deleteChatHistory(peerMac: String) =
        messageDao.deleteChatHistory(peerMac)

    // UserSettings updates
    suspend fun updateDisplayName(name: String) = settingsDao.updateDisplayName(name)
    suspend fun updateDarkMode(mode: String) = settingsDao.updateDarkMode(mode)
    suspend fun updatePowerSaving(enabled: Boolean) = settingsDao.updatePowerSaving(enabled)
    suspend fun updateNotifications(enabled: Boolean) = settingsDao.updateNotifications(enabled)
    suspend fun updateCallAlerts(enabled: Boolean) = settingsDao.updateCallAlerts(enabled)
    suspend fun updatePrivacyApproval(enabled: Boolean) = settingsDao.updatePrivacyApproval(enabled)
    suspend fun updateEnableSimulation(enabled: Boolean) = settingsDao.updateEnableSimulation(enabled)
    
    // Generic update for entire settings object
    suspend fun updateUserSettings(settings: UserSettings) = settingsDao.insertSettings(settings)
}
