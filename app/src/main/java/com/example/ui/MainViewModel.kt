package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.tasks.await
import com.example.bluetooth.ActiveCall
import com.example.bluetooth.BluetoothService
import com.example.bluetooth.ConnectionState
import com.example.crypto.CryptoEngine
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.content.Intent
import android.widget.Toast
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

enum class AppScreen {
    CHATS, CHAT_DETAIL, CALLS, DEVICES, SETTINGS, ABOUT, LOGIN, CLOUD_HUB, OFFICIAL_WEB, ADMIN_DASHBOARD
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = AppRepository(
        database.peerDao(),
        database.messageDao(),
        database.settingsDao()
    )
    private val bluetoothService = BluetoothService(application, repository)

    // Navigation and active UI selection
    private val _currentScreen = MutableStateFlow(AppScreen.CHATS)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _activeChatPeer = MutableStateFlow<PeerDevice?>(null)
    val activeChatPeer: StateFlow<PeerDevice?> = _activeChatPeer.asStateFlow()

    // Database reactive streams
    val peers: StateFlow<List<PeerDevice>> = repository.allPeers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMessages: StateFlow<List<ChatMessage>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val callsHistory: StateFlow<List<ChatMessage>> = repository.allCallsHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<UserSettings> = repository.settingsFlow
        .map { it ?: UserSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserSettings())

    // Bluetooth Service reactive streams
    val isScanning: StateFlow<Boolean> = bluetoothService.isScanning
    val discoveredDevices: StateFlow<List<PeerDevice>> = bluetoothService.discoveredDevices
    val connectionState: StateFlow<ConnectionState> = bluetoothService.connectionState
    val connectedPeer: StateFlow<PeerDevice?> = bluetoothService.connectedPeer
    val activeCall: StateFlow<ActiveCall?> = bluetoothService.activeCall
    val incomingCall: StateFlow<ActiveCall?> = bluetoothService.incomingCall

    // Auth, Backup and Restore states
    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _backupLoading = MutableStateFlow(false)
    val backupLoading: StateFlow<Boolean> = _backupLoading.asStateFlow()

    private val _backupStatusMsg = MutableStateFlow<String?>(null)
    val backupStatusMsg: StateFlow<String?> = _backupStatusMsg.asStateFlow()

    private val _showGoogleSimulationDialog = MutableStateFlow(false)
    val showGoogleSimulationDialog: StateFlow<Boolean> = _showGoogleSimulationDialog.asStateFlow()

    fun setGoogleSimulationDialog(visible: Boolean) {
        _showGoogleSimulationDialog.value = visible
    }

    // Simulated Cloud States
    private val _cloudSessions = MutableStateFlow<List<CloudSession>>(emptyList())
    val cloudSessions: StateFlow<List<CloudSession>> = _cloudSessions.asStateFlow()

    private val _cloudTickets = MutableStateFlow<List<SupportTicket>>(emptyList())
    val cloudTickets: StateFlow<List<SupportTicket>> = _cloudTickets.asStateFlow()

    private val _cloudVersions = MutableStateFlow<List<AppVersion>>(emptyList())
    val cloudVersions: StateFlow<List<AppVersion>> = _cloudVersions.asStateFlow()

    private val _cloudFeedback = MutableStateFlow<List<CloudFeedback>>(emptyList())
    val cloudFeedback: StateFlow<List<CloudFeedback>> = _cloudFeedback.asStateFlow()

    private val _cloudNotifications = MutableStateFlow<List<CloudNotification>>(emptyList())
    val cloudNotifications: StateFlow<List<CloudNotification>> = _cloudNotifications.asStateFlow()

    private val _cloudUsers = MutableStateFlow<List<CloudUser>>(emptyList())
    val cloudUsers: StateFlow<List<CloudUser>> = _cloudUsers.asStateFlow()

    fun refreshCloudData() {
        val app = getApplication<Application>()
        val email = settings.value.accountEmail
        _cloudSessions.value = if (email.isNotBlank()) SimulatedCloudServer.getSessions(app, email) else emptyList()
        _cloudTickets.value = if (email.isNotBlank()) SimulatedCloudServer.getTicketsForUser(app, email) else emptyList()
        _cloudVersions.value = SimulatedCloudServer.getAppVersions(app)
        _cloudFeedback.value = SimulatedCloudServer.getAllFeedbackReports(app)
        _cloudNotifications.value = SimulatedCloudServer.getGeneralNotifications(app)
        _cloudUsers.value = SimulatedCloudServer.getUsers(app)
    }

    init {
        // Initialize End-to-End Cryptography Engine
        CryptoEngine.initialize(application)
        SimulatedCloudServer.initializeDefaultsIfNeeded(application)
        
        // Auto-redirect to Login screen if not logged in
        viewModelScope.launch {
            settings.collect { currentSettings ->
                if (!currentSettings.isLoggedIn && _currentScreen.value != AppScreen.LOGIN) {
                    _currentScreen.value = AppScreen.LOGIN
                }
                refreshCloudData()
            }
        }
        
        // Listen to connection state to auto-navigate to chat detail if connected
        viewModelScope.launch {
            connectedPeer.collect { peer ->
                if (peer != null && _currentScreen.value == AppScreen.DEVICES) {
                    _activeChatPeer.value = peer
                    _currentScreen.value = AppScreen.CHAT_DETAIL
                }
            }
        }
    }

    // Navigation actions
    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
        if (screen != AppScreen.CHAT_DETAIL) {
            _activeChatPeer.value = null
        }
    }

    fun openChatWith(peer: PeerDevice) {
        _activeChatPeer.value = peer
        _currentScreen.value = AppScreen.CHAT_DETAIL
        // Connect if not already connected
        if (connectedPeer.value?.macAddress != peer.macAddress) {
            bluetoothService.connectToDevice(peer)
        }
    }

    // Bluetooth discovery actions
    fun startScanning(forceRestart: Boolean = false) {
        bluetoothService.startScan(forceRestart)
    }

    fun stopScanning() {
        bluetoothService.stopScan()
    }

    fun hasBluetoothPermissions(): Boolean = bluetoothService.hasBluetoothPermissions()
    fun isBluetoothEnabled(): Boolean = bluetoothService.isBluetoothEnabled()
    fun isDiscoverable(): Boolean = bluetoothService.isDiscoverable()
    fun requestDiscoverable() = bluetoothService.requestDiscoverable()

    fun toggleScanning() {
        if (isScanning.value) {
            bluetoothService.stopScan()
        } else {
            bluetoothService.startScan()
        }
    }

    fun connectToDevice(peer: PeerDevice) {
        bluetoothService.connectToDevice(peer)
    }

    fun disconnect() {
        bluetoothService.disconnect()
        _currentScreen.value = AppScreen.CHATS
    }

    // Chat Actions
    val activeChatMessages: Flow<List<ChatMessage>> = _activeChatPeer.flatMapLatest { peer ->
        if (peer == null) flowOf(emptyList())
        else repository.getMessagesForPeer(peer.macAddress)
    }

    fun sendTextMessage(text: String) {
        if (text.isBlank()) return
        bluetoothService.sendEncryptedMessage(text)
    }

    fun sendNudge() {
        bluetoothService.sendNudge()
    }

    fun sendFile(fileName: String, sizeBytes: Long) {
        bluetoothService.sendFile(fileName, sizeBytes)
    }

    fun deleteChatHistory(peerMac: String) {
        viewModelScope.launch {
            repository.deleteChatHistory(peerMac)
        }
    }

    // Call Actions
    fun startVoiceCall() {
        bluetoothService.startCall("VOICE")
    }

    fun startVideoCall() {
        bluetoothService.startCall("VIDEO")
    }

    fun acceptCall() {
        bluetoothService.acceptCall()
    }

    fun rejectCall() {
        bluetoothService.rejectCall()
    }

    fun hangUp() {
        bluetoothService.hangUp()
    }

    // Simulated Trigger (Allows simulating incoming call from any peer)
    fun simulateIncomingCall(peerMac: String, type: String) {
        bluetoothService.receiveIncomingCallSimulated(peerMac, type)
    }

    // Settings modifiers
    fun updateDisplayName(name: String) = viewModelScope.launch {
        repository.updateDisplayName(name)
    }

    fun updateDarkMode(mode: String) = viewModelScope.launch {
        repository.updateDarkMode(mode)
    }

    fun togglePowerSaving() = viewModelScope.launch {
        val current = settings.value.powerSavingMode
        repository.updatePowerSaving(!current)
    }

    fun toggleNotifications() = viewModelScope.launch {
        val current = settings.value.notificationsEnabled
        repository.updateNotifications(!current)
    }

    fun toggleCallAlerts() = viewModelScope.launch {
        val current = settings.value.callAlertsEnabled
        repository.updateCallAlerts(!current)
    }

    fun togglePrivacyRequireApproval() = viewModelScope.launch {
        val current = settings.value.privacyRequireApproval
        repository.updatePrivacyApproval(!current)
    }

    fun toggleSimulation() = viewModelScope.launch {
        val current = settings.value.enableSimulation
        repository.updateEnableSimulation(!current)
    }

    fun updatePeerNickname(mac: String, nickname: String?) = viewModelScope.launch {
        repository.updatePeerNickname(mac, nickname)
        // Sync active peer state if we renamed them
        if (_activeChatPeer.value?.macAddress == mac) {
            _activeChatPeer.value = _activeChatPeer.value?.copy(nickname = nickname)
        }
    }

    fun deletePeer(peer: PeerDevice) = viewModelScope.launch {
        repository.deletePeer(peer)
        repository.deleteChatHistory(peer.macAddress)
        if (_activeChatPeer.value?.macAddress == peer.macAddress) {
            _activeChatPeer.value = null
            _currentScreen.value = AppScreen.CHATS
        }
    }

    // Secure Decryption helper on-the-fly for UI rendering
    fun decryptMessageText(msg: ChatMessage): String {
        if (msg.type != "TEXT") return msg.text
        val encryptedAesKey = msg.encryptedAesKey ?: return msg.text
        val iv = msg.iv
        
        return try {
            val aesKey = CryptoEngine.decryptAESKeyWithRSA(encryptedAesKey)
            if (aesKey != null) {
                CryptoEngine.decryptAES(msg.text, iv, aesKey)
            } else {
                "🔒 فشل في فك تشفير الرسالة"
            }
        } catch (e: Exception) {
            "🔐 رسالة مشفرة آمنة"
        }
    }

    fun getMyPublicKey(): String = CryptoEngine.getMyPublicKey()

    // --- Authentication and Cloud Backups ---

    private fun hashPassword(password: String): String {
        return try {
            val digest = java.security.MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(password.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { String.format("%02x", it) }
        } catch (e: Exception) {
            password
        }
    }

    fun registerUser(email: String, name: String, passwordRaw: String, onComplete: (Boolean) -> Unit) {
        if (email.isBlank() || name.isBlank() || passwordRaw.length < 6) {
            _authError.value = "يجب ملء جميع الحقول وكلمة المرور لا تقل عن 6 أحرف"
            onComplete(false)
            return
        }
        _authError.value = null
        viewModelScope.launch {
            try {
                val pwdHash = hashPassword(passwordRaw)
                val current = repository.getSettingsDirect()
                
                // If Firebase is available, perform actual Firebase registration
                val isFirebase = CloudBackupManager.isFirebaseConfigured(getApplication())
                if (isFirebase) {
                    try {
                        com.google.firebase.auth.FirebaseAuth.getInstance()
                            .createUserWithEmailAndPassword(email, passwordRaw)
                            .await()
                    } catch (fe: Exception) {
                        Log.w("MainViewModel", "Firebase auth error, continuing locally: ${fe.message}")
                    }
                }

                // Save locally
                val updated = current.copy(
                    isLoggedIn = true,
                    accountEmail = email,
                    accountPasswordHash = pwdHash,
                    displayName = name
                )
                repository.updateUserSettings(updated)
                SimulatedCloudServer.registerOrUpdateUserInCloud(getApplication(), email, name)
                
                _currentScreen.value = AppScreen.CHATS
                onComplete(true)
            } catch (e: Exception) {
                _authError.value = "حدث خطأ أثناء التسجيل: ${e.message}"
                onComplete(false)
            }
        }
    }

    fun loginUser(email: String, passwordRaw: String, onComplete: (Boolean, String?) -> Unit) {
        if (email.isBlank() || passwordRaw.isBlank()) {
            _authError.value = "الرجاء إدخال البريد الإلكتروني وكلمة المرور"
            onComplete(false, null)
            return
        }
        _authError.value = null
        viewModelScope.launch {
            try {
                val pwdHash = hashPassword(passwordRaw)
                val current = repository.getSettingsDirect()

                // If user matches locally registered user
                if (current.isLoggedIn && current.accountEmail.lowercase() == email.lowercase()) {
                    if (current.accountPasswordHash == pwdHash) {
                        SimulatedCloudServer.registerOrUpdateUserInCloud(getApplication(), email, current.displayName)
                        _currentScreen.value = AppScreen.CHATS
                        onComplete(true, null)
                        return@launch
                    } else {
                        _authError.value = "كلمة المرور غير صحيحة"
                        onComplete(false, null)
                        return@launch
                    }
                }

                // If not matched locally, check if cloud backup exists to restore!
                _backupLoading.value = true
                val checkResult = CloudBackupManager.checkBackupExists(getApplication(), email)
                _backupLoading.value = false

                if (checkResult.getOrDefault(false)) {
                    // Offer Restore!
                    onComplete(false, "RESTORE_AVAILABLE")
                } else {
                    // Try real firebase auth to check if they have account but no backups
                    val isFirebase = CloudBackupManager.isFirebaseConfigured(getApplication())
                    var firebaseSuccess = false
                    if (isFirebase) {
                        try {
                            com.google.firebase.auth.FirebaseAuth.getInstance()
                                .signInWithEmailAndPassword(email, passwordRaw)
                                .await()
                            firebaseSuccess = true
                        } catch (e: Exception) {
                            Log.e("MainViewModel", "Firebase Sign-In failed: ${e.message}")
                        }
                    }

                    if (firebaseSuccess) {
                        // User exists in Firebase, restore database or complete login
                        val updated = current.copy(
                            isLoggedIn = true,
                            accountEmail = email,
                            accountPasswordHash = pwdHash,
                            displayName = email.substringBefore("@")
                        )
                        repository.updateUserSettings(updated)
                        SimulatedCloudServer.registerOrUpdateUserInCloud(getApplication(), email, email.substringBefore("@"))
                        _currentScreen.value = AppScreen.CHATS
                        onComplete(true, null)
                    } else {
                        // Create user locally if no user exists at all (First run)
                        val updated = current.copy(
                            isLoggedIn = true,
                            accountEmail = email,
                            accountPasswordHash = pwdHash,
                            displayName = email.substringBefore("@")
                        )
                        repository.updateUserSettings(updated)
                        SimulatedCloudServer.registerOrUpdateUserInCloud(getApplication(), email, email.substringBefore("@"))
                        _currentScreen.value = AppScreen.CHATS
                        onComplete(true, null)
                    }
                }
            } catch (e: Exception) {
                _authError.value = "حدث خطأ أثناء تسجيل الدخول: ${e.message}"
                onComplete(false, null)
            }
        }
    }

    fun signInWithGoogle(context: android.content.Context, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _backupLoading.value = true
            _authError.value = null
            try {
                val credentialManager = CredentialManager.create(context)
                val webClientId = "10414533908849-mock-client-id-for-tawasul-plus.apps.googleusercontent.com"
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setAutoSelectEnabled(false)
                    .setServerClientId(webClientId)
                    .build()
                
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                
                try {
                    val result = credentialManager.getCredential(context = context, request = request)
                    val credential = result.credential
                    if (credential is GoogleIdTokenCredential) {
                        val googleIdTokenCredential = credential
                        val email = googleIdTokenCredential.id
                        val name = googleIdTokenCredential.displayName ?: email.substringBefore("@")
                        val photoUrl = googleIdTokenCredential.profilePictureUri?.toString() ?: ""
                        completeGoogleSignIn(email, name, photoUrl, onComplete)
                    } else {
                        throw Exception("نوع الاعتماد غير مدعوم")
                    }
                } catch (ce: Exception) {
                    Log.w("MainViewModel", "CredentialManager failed: ${ce.message}. Opening custom Google Sandbox Account helper.")
                    _showGoogleSimulationDialog.value = true
                    _backupLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "Google sign in exception: ${e.message}")
                _showGoogleSimulationDialog.value = true
                _backupLoading.value = false
            }
        }
    }

    fun completeSimulatedGoogleSignIn(email: String, name: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _backupLoading.value = true
            // Use a clean professional avatar gradient or photo
            val mockPhoto = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80"
            completeGoogleSignIn(email, name, mockPhoto, onComplete)
            _showGoogleSimulationDialog.value = false
        }
    }

    private suspend fun completeGoogleSignIn(email: String, name: String, photoUrl: String, onComplete: (Boolean, String?) -> Unit) {
        val current = repository.getSettingsDirect()
        val updated = current.copy(
            isLoggedIn = true,
            accountEmail = email,
            displayName = name,
            profilePictureUrl = photoUrl
        )
        repository.updateUserSettings(updated)
        SimulatedCloudServer.registerOrUpdateUserInCloud(getApplication(), email, name)
        
        val isFirebase = CloudBackupManager.isFirebaseConfigured(getApplication())
        if (isFirebase) {
            try {
                com.google.firebase.auth.FirebaseAuth.getInstance().signInAnonymously().await()
            } catch (e: Exception) {
                Log.e("MainViewModel", "Firebase sign in failed: ${e.message}")
            }
        }
        
        _currentScreen.value = AppScreen.CHATS
        _backupLoading.value = false
        onComplete(true, null)
    }

    private suspend fun simulateGoogleSignIn(onComplete: (Boolean, String?) -> Unit) {
        val mockEmail = "waelmahdly531@gmail.com"
        val mockName = "وائل المهدلي"
        // High quality professional Unsplash developer user portrait photo
        val mockPhoto = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&q=80"
        completeGoogleSignIn(mockEmail, mockName, mockPhoto, onComplete)
    }

    fun logoutUser() {
        viewModelScope.launch {
            val current = repository.getSettingsDirect()
            val updated = current.copy(isLoggedIn = false)
            repository.updateUserSettings(updated)
            _currentScreen.value = AppScreen.LOGIN
        }
    }

    fun backupToCloud() {
        val email = settings.value.accountEmail
        if (email.isBlank()) {
            _backupStatusMsg.value = "الرجاء تسجيل الدخول أولاً"
            return
        }
        viewModelScope.launch {
            _backupLoading.value = true
            _backupStatusMsg.value = "جاري رفع النسخة الاحتياطية السحابية..."
            val result = CloudBackupManager.backupData(getApplication(), repository, email)
            _backupLoading.value = false
            if (result.isSuccess) {
                _backupStatusMsg.value = "تم أخذ نسخة احتياطية بنجاح"
            } else {
                _backupStatusMsg.value = "فشل النسخ الاحتياطي: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun restoreFromCloud(email: String, onResult: (Boolean, String) -> Unit) {
        if (email.isBlank()) {
            onResult(false, "البريد الإلكتروني فارغ")
            return
        }
        viewModelScope.launch {
            _backupLoading.value = true
            _backupStatusMsg.value = "جاري استعادة البيانات من السحابة..."
            val result = CloudBackupManager.restoreData(getApplication(), repository, email)
            _backupLoading.value = false
            if (result.isSuccess) {
                _backupStatusMsg.value = "تمت استعادة البيانات بنجاح!"
                _currentScreen.value = AppScreen.CHATS
                onResult(true, "تمت استعادة البيانات بنجاح!")
            } else {
                val err = result.exceptionOrNull()?.message ?: "خطأ غير معروف"
                _backupStatusMsg.value = "فشلت الاستعادة: $err"
                onResult(false, err)
            }
        }
    }

    fun downloadAccountInfo(onResult: (android.net.Uri?, String) -> Unit) {
        val email = settings.value.accountEmail
        val name = settings.value.displayName
        if (email.isBlank()) {
            onResult(null, "يجب تسجيل الدخول أولاً")
            return
        }
        viewModelScope.launch {
            try {
                val stats = mapOf(
                    "total_peers" to peers.value.size,
                    "total_messages" to allMessages.value.size,
                    "total_calls" to callsHistory.value.size,
                    "is_firebase_active" to CloudBackupManager.isFirebaseConfigured(getApplication()),
                    "backup_count" to settings.value.backupCount,
                    "public_key" to getMyPublicKey()
                )
                val result = AccountExportHelper.exportAccountInfo(getApplication(), email, name, stats)
                if (result.isSuccess) {
                    onResult(result.getOrNull(), "تم تحميل معلومات الحساب بنجاح في مجلد التنزيلات")
                } else {
                    onResult(null, "فشل التحميل: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                onResult(null, "فشل التحميل: ${e.message}")
            }
        }
    }

    private val _showHotspotShareDialog = MutableStateFlow(false)
    val showHotspotShareDialog: StateFlow<Boolean> = _showHotspotShareDialog.asStateFlow()

    fun setShowHotspotShareDialog(show: Boolean) {
        _showHotspotShareDialog.value = show
    }

    fun shareAppApk(context: android.content.Context) {
        try {
            val apkPath = context.packageCodePath
            val apkFile = java.io.File(apkPath)
            if (apkFile.exists()) {
                val contentUri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    apkFile
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/vnd.android.package-archive"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "مشاركة تطبيق تواصل بلس عبر البلوتوث / الواي فاي:"))
            } else {
                Toast.makeText(context, "لم يتم العثور على ملف التطبيق الأساسي!", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error sharing APK: ${e.message}")
            // Fallback: Share link
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "تحميل تطبيق تواصل بلس")
                putExtra(Intent.EXTRA_TEXT, "قم بتحميل تطبيق تواصل بلس للمحادثات المشفرة مباشرة عبر الرابط: https://play.google.com/store/apps/details?id=${context.packageName}")
            }
            context.startActivity(Intent.createChooser(intent, "مشاركة رابط التطبيق:"))
        }
    }

    fun shareAppViaHotspot(context: android.content.Context) {
        // Show step by step local server download hotspot guide dialog
        _showHotspotShareDialog.value = true
    }

    // --- Simulated Cloud Core Action APIs ---
    fun createCloudTicket(title: String, description: String, category: String, onComplete: (Boolean) -> Unit) {
        val email = settings.value.accountEmail
        if (email.isBlank()) {
            onComplete(false)
            return
        }
        viewModelScope.launch {
            SimulatedCloudServer.createSupportTicket(getApplication(), email, title, description, category)
            refreshCloudData()
            onComplete(true)
        }
    }

    fun replyToCloudTicket(ticketId: String, sender: String, text: String, newStatus: String? = null) {
        viewModelScope.launch {
            SimulatedCloudServer.replyToTicket(getApplication(), ticketId, sender, text, newStatus)
            refreshCloudData()
        }
    }

    fun publishNewAppVersion(versionName: String, changelog: String, isCritical: Boolean) {
        viewModelScope.launch {
            SimulatedCloudServer.publishNewVersion(getApplication(), versionName, changelog, isCritical)
            refreshCloudData()
        }
    }

    fun submitAppFeedback(type: String, content: String) {
        val email = settings.value.accountEmail
        if (email.isBlank()) return
        viewModelScope.launch {
            SimulatedCloudServer.submitFeedbackOrCrash(getApplication(), email, type, content)
            refreshCloudData()
        }
    }

    fun revokeCloudSession(sessionId: String) {
        viewModelScope.launch {
            SimulatedCloudServer.revokeSession(getApplication(), sessionId)
            refreshCloudData()
        }
    }

    fun sendGlobalAdminNotification(title: String, message: String) {
        viewModelScope.launch {
            SimulatedCloudServer.sendGeneralNotification(getApplication(), title, message)
            refreshCloudData()
        }
    }

    fun toggleCloudUserBlock(email: String, currentStatus: String) {
        viewModelScope.launch {
            val nextStatus = if (currentStatus == "ACTIVE") "BLOCKED" else "ACTIVE"
            SimulatedCloudServer.updateUserStatus(getApplication(), email, nextStatus)
            refreshCloudData()
        }
    }
}
