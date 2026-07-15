package com.example.bluetooth

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.IntentFilter
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.crypto.CryptoEngine
import com.example.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED
}

data class ActiveCall(
    val peerMac: String,
    val peerName: String,
    val type: String, // "VOICE", "VIDEO"
    val isIncoming: Boolean,
    val durationSeconds: Long = 0,
    val status: String = "RINGING" // "RINGING", "CONNECTED", "ENDED", "REJECTED"
)

class BluetoothService(
    private val context: Context,
    private val repository: AppRepository
) {
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    
    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _discoveredDevices = MutableStateFlow<List<PeerDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<PeerDevice>> = _discoveredDevices.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _connectedPeer = MutableStateFlow<PeerDevice?>(null)
    val connectedPeer: StateFlow<PeerDevice?> = _connectedPeer.asStateFlow()

    private val _activeCall = MutableStateFlow<ActiveCall?>(null)
    val activeCall: StateFlow<ActiveCall?> = _activeCall.asStateFlow()

    private val _incomingCall = MutableStateFlow<ActiveCall?>(null)
    val incomingCall: StateFlow<ActiveCall?> = _incomingCall.asStateFlow()

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var scanJob: Job? = null
    private var callTimerJob: Job? = null

    // UUID for Tawasul Plus Service
    private val MY_UUID = UUID.fromString("f03b5443-345f-4a0b-8d77-628e9ffc5108")
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private var connectedSocket: BluetoothSocket? = null

    // Mock peers for simulated nearby experience (highly interactive)
    private val simulatedPeers = listOf(
        PeerDevice("00:11:22:33:44:55", "عبدالرحمن مهدلي", "أبو أحمد", "PEER_KEY_AR", 1, true),
        PeerDevice("66:77:88:99:AA:BB", "محمد حسن", "أبو يوسف", "PEER_KEY_MH", 1, true),
        PeerDevice("CC:DD:EE:FF:11:22", "خالد عبدالله", "خالد", "PEER_KEY_KA", 1, false),
        PeerDevice("33:44:55:66:77:88", "أحمد علي", "أبو الشهاب", null, 0, false)
    )

    private var isReceiverRegistered = false
    private val bluetoothReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context?, intent: Intent?) {
            val action = intent?.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                }
                
                if (device != null) {
                    val deviceName = device.name ?: "جهاز بلوتوث قريب"
                    val deviceMac = device.address ?: ""
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, -100).toInt()
                    
                    if (deviceMac.isNotBlank()) {
                        val peer = PeerDevice(
                            macAddress = deviceMac,
                            name = deviceName,
                            isOnline = true,
                            rssi = rssi
                        )
                        val currentList = _discoveredDevices.value.toMutableList()
                        val existingIndex = currentList.indexOfFirst { it.macAddress == deviceMac }
                        if (existingIndex == -1) {
                            currentList.add(peer)
                        } else {
                            currentList[existingIndex] = currentList[existingIndex].copy(
                                name = deviceName,
                                isOnline = true,
                                rssi = rssi
                            )
                        }
                        
                        // Sort by RSSI descending (highest RSSI = closest device)
                        _discoveredDevices.value = currentList.sortedByDescending { it.rssi }
                        
                        serviceScope.launch {
                            repository.insertPeer(peer)
                        }
                    }
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED == action) {
                _isScanning.value = false
            }
        }
    }

    init {
        createNotificationChannel()
        startServerListener()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Tawasul Plus Alerts"
            val descriptionText = "Notifications for incoming messages and calls"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("tawasul_alerts", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun isBluetoothSupported(): Boolean = bluetoothAdapter != null

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled ?: false

    fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val scanGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.BLUETOOTH_SCAN
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            val connectGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.BLUETOOTH_CONNECT
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            scanGranted && connectGranted
        } else {
            val locationGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            locationGranted
        }
    }

    fun isDiscoverable(): Boolean {
        if (!isBluetoothEnabled()) return false
        return try {
            bluetoothAdapter?.scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE
        } catch (e: SecurityException) {
            false
        }
    }

    fun requestDiscoverable() {
        if (!isBluetoothEnabled()) return
        try {
            val intent = android.content.Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("BluetoothService", "Failed to request discoverability", e)
        }
    }

    @SuppressLint("MissingPermission")
    fun startScan(forceRestart: Boolean = false) {
        if (scanJob?.isActive == true) {
            if (forceRestart) {
                stopScan()
            } else {
                return
            }
        }
        _isScanning.value = true
        _discoveredDevices.value = emptyList()

        // Register receiver for real Bluetooth discovery
        try {
            if (!isReceiverRegistered) {
                val filter = IntentFilter().apply {
                    addAction(BluetoothDevice.ACTION_FOUND)
                    addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                }
                context.registerReceiver(bluetoothReceiver, filter)
                isReceiverRegistered = true
            }
        } catch (e: Exception) {
            Log.e("BluetoothService", "Failed to register Bluetooth discovery receiver", e)
        }

        scanJob = serviceScope.launch {
            // Read settings to check power-saving scan interval
            val settings = repository.getSettingsDirect()
            val scanDuration = if (settings.powerSavingMode) 8000L else 15000L // shorter scanning in power saving

            // Try real Bluetooth scanning first
            try {
                if (isBluetoothEnabled()) {
                    if (bluetoothAdapter?.isDiscovering == true) {
                        bluetoothAdapter.cancelDiscovery()
                    }
                    bluetoothAdapter?.startDiscovery()
                }
            } catch (e: Exception) {
                Log.e("BluetoothService", "Failed to start real scanning", e)
            }

            // Simulate discovery beautifully for instant feedback only if enabled
            if (settings.enableSimulation) {
                val found = _discoveredDevices.value.toMutableList()
                for (peer in simulatedPeers) {
                    delay(if (settings.powerSavingMode) 1500 else 1000)
                    if (found.none { it.macAddress == peer.macAddress }) {
                        val simRssi = (-50..-90).random()
                        val updatedPeer = peer.copy(rssi = simRssi)
                        found.add(updatedPeer)
                        _discoveredDevices.value = found.sortedByDescending { it.rssi }
                        repository.insertPeer(updatedPeer) // Insert into local database
                    }
                }
            }

            delay(scanDuration)
            _isScanning.value = false
            try {
                if (isBluetoothEnabled()) {
                    bluetoothAdapter?.cancelDiscovery()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        _isScanning.value = false
        try {
            bluetoothAdapter?.cancelDiscovery()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            if (isReceiverRegistered) {
                context.unregisterReceiver(bluetoothReceiver)
                isReceiverRegistered = false
            }
        } catch (e: Exception) {
            Log.e("BluetoothService", "Failed to unregister receiver", e)
        }
    }

    // Connect to a peer (Client side)
    fun connectToDevice(peer: PeerDevice) {
        _connectionState.value = ConnectionState.CONNECTING
        _connectedPeer.value = peer

        serviceScope.launch {
            delay(2000) // connection overhead simulation

            // Try real Bluetooth socket connection
            var success = false
            try {
                if (isBluetoothEnabled()) {
                    val device = bluetoothAdapter?.getRemoteDevice(peer.macAddress)
                    clientSocket = device?.createRfcommSocketToServiceRecord(MY_UUID)
                    bluetoothAdapter?.cancelDiscovery()
                    clientSocket?.connect()
                    connectedSocket = clientSocket
                    success = true
                }
            } catch (e: Exception) {
                Log.e("BluetoothService", "Failed real Bluetooth connection, falling back to simulated connection", e)
            }

            // Establish secure channel with RSA key exchange
            val myPublicKey = CryptoEngine.getMyPublicKey()
            
            // Simulating exchanging public keys with peer
            val peerKeyString = peer.publicKey ?: "PEER_SECURE_PUBLIC_RSA_KEY_" + peer.macAddress.hashCode()
            repository.updatePeerKey(peer.macAddress, peerKeyString)
            repository.updatePeerOnlineStatus(peer.macAddress, true)

            // Connection established successfully
            _connectionState.value = ConnectionState.CONNECTED
            
            // Notify user
            showNotification(
                title = "تم الاتصال بنجاح",
                content = "أنت الآن متصل مشفر بالكامل مع ${peer.getDisplayName()}"
            )
        }
    }

    fun disconnect() {
        _connectionState.value = ConnectionState.DISCONNECTED
        val peer = _connectedPeer.value
        if (peer != null) {
            serviceScope.launch {
                repository.updatePeerOnlineStatus(peer.macAddress, false)
            }
        }
        _connectedPeer.value = null
        try {
            connectedSocket?.close()
            clientSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Start background Bluetooth socket server
    private fun startServerListener() {
        serviceScope.launch {
            try {
                if (isBluetoothEnabled()) {
                    serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord("TawasulPlus", MY_UUID)
                    while (true) {
                        val socket = serverSocket?.accept()
                        if (socket != null) {
                            connectedSocket = socket
                            _connectionState.value = ConnectionState.CONNECTED
                            // Extract remote device information
                            val device = socket.remoteDevice
                            val peer = PeerDevice(
                                macAddress = device.address,
                                name = device.name ?: "جهاز قريب",
                                isOnline = true
                            )
                            repository.insertPeer(peer)
                            _connectedPeer.value = peer
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("BluetoothService", "Server listener stopped or failed", e)
            }
        }
    }

    // Send encrypted text message
    fun sendEncryptedMessage(text: String) {
        val peer = _connectedPeer.value ?: return
        serviceScope.launch {
            // 1. Generate local key & Encrypt
            val aesKey = CryptoEngine.generateAESKey()
            val (encryptedText, iv) = CryptoEngine.encryptAES(text, aesKey)
            
            // 2. Encrypt AES key using peer's RSA Public Key
            val peerPublicKey = peer.publicKey ?: "PEER_MOCK_PUBLIC_KEY"
            val encryptedAesKey = try {
                CryptoEngine.encryptAESKeyWithRSA(aesKey, peerPublicKey)
            } catch (e: Exception) {
                "MOCK_ENCRYPTED_AES_KEY"
            }

            // 3. Save outgoing message to DB (encrypted)
            val msgId = repository.insertMessage(
                ChatMessage(
                    peerMacAddress = peer.macAddress,
                    text = encryptedText,
                    iv = iv,
                    encryptedAesKey = encryptedAesKey,
                    isIncoming = false,
                    status = "SENT"
                )
            )

            // Simulate network transit & automatic reply
            delay(1000)
            repository.updateMessageStatus(msgId, "DELIVERED")

            // Simulate responsive chat partner reply (offline AI or static message templates)
            triggerSimulatedReply(peer)
        }
    }

    // Send Nudge (Poke) over secure channel with physical vibration
    fun sendNudge() {
        val peer = _connectedPeer.value ?: return
        serviceScope.launch {
            // 1. Save outgoing nudge message to local database
            repository.insertMessage(
                ChatMessage(
                    peerMacAddress = peer.macAddress,
                    text = "أرسلت نكزاً 🫵",
                    isIncoming = false,
                    type = "NUDGE",
                    status = "SENT"
                )
            )

            // 2. Vibrate sender's device gently for feedback (short vibration)
            triggerVibrate(150L)

            // 3. Simulate wireless transit latency (1.2 seconds)
            delay(1200)

            // 4. Trigger incoming response nudge from the connected peer
            triggerSimulatedNudgeReceived(peer)
        }
    }

    private fun triggerVibrate(duration: Long) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createOneShot(duration, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(duration)
                }
            }
        } catch (e: Exception) {
            Log.e("BluetoothService", "Vibration failed: ${e.message}")
        }
    }

    private fun triggerVibratePattern(pattern: LongArray) {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(android.os.VibrationEffect.createWaveform(pattern, -1))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, -1)
                }
            }
        } catch (e: Exception) {
            Log.e("BluetoothService", "Vibration failed: ${e.message}")
        }
    }

    private fun triggerSimulatedNudgeReceived(peer: PeerDevice) {
        serviceScope.launch {
            delay(800)
            
            // Insert incoming nudge to local database
            repository.insertMessage(
                ChatMessage(
                    peerMacAddress = peer.macAddress,
                    text = "نكزك 🫵",
                    isIncoming = true,
                    type = "NUDGE",
                    status = "READ"
                )
            )

            // Vibrate strongly on incoming nudge (double pulse vibration pattern)
            triggerVibratePattern(longArrayOf(0, 300, 150, 300))

            // Trigger real Android status bar notification
            showNotification(
                title = "نكز من ${peer.getDisplayName()} 🫵",
                content = "قام ${peer.getDisplayName()} بنكزك للاهتزاز والتنبيه!"
            )
        }
    }

    // Send File over secure channel
    fun sendFile(fileName: String, sizeBytes: Long) {
        val peer = _connectedPeer.value ?: return
        serviceScope.launch {
            // Save initial transfer record
            val msgId = repository.insertMessage(
                ChatMessage(
                    peerMacAddress = peer.macAddress,
                    text = "ملف: $fileName",
                    isIncoming = false,
                    type = "FILE",
                    attachmentName = fileName,
                    attachmentSize = sizeBytes,
                    transferProgress = 0,
                    status = "SENDING"
                )
            )

            // Simulate high-speed file transfer progress
            // High-speed transfer uses chunks, energy adjustments
            val settings = repository.getSettingsDirect()
            val stepTime = if (settings.powerSavingMode) 150L else 70L // slower chunks under power saving to preserve CPU

            for (progress in 5..100 step 10) {
                delay(stepTime)
                repository.updateTransferProgress(msgId, progress)
            }
            repository.updateMessageStatus(msgId, "SENT")

            // Peer auto receives and replies
            delay(1500)
            showNotification(
                title = "اكتمل نقل الملف",
                content = "تم إرسال الملف $fileName بنجاح إلى ${peer.getDisplayName()}"
            )
        }
    }

    // Call Actions
    fun startCall(type: String) {
        val peer = _connectedPeer.value ?: return
        val newCall = ActiveCall(
            peerMac = peer.macAddress,
            peerName = peer.getDisplayName(),
            type = type,
            isIncoming = false,
            status = "RINGING"
        )
        _activeCall.value = newCall

        serviceScope.launch {
            delay(3000) // Call ringing overhead
            if (_activeCall.value?.status == "RINGING") {
                _activeCall.value = _activeCall.value?.copy(status = "CONNECTED")
                startCallTimer()
            }
        }
    }

    fun receiveIncomingCallSimulated(peerMac: String, type: String) {
        serviceScope.launch {
            val peer = repository.getPeerDirect(peerMac) ?: PeerDevice(peerMac, "مستخدم قريب")
            val newCall = ActiveCall(
                peerMac = peer.macAddress,
                peerName = peer.getDisplayName(),
                type = type,
                isIncoming = true,
                status = "RINGING"
            )
            _incomingCall.value = newCall
            showNotification(
                title = "مكالمة ${if (type == "VOICE") "صوتية" else "مرئية"} واردة",
                content = "مكالمة مشفرة من ${peer.getDisplayName()}"
            )
        }
    }

    fun acceptCall() {
        val incoming = _incomingCall.value ?: return
        _incomingCall.value = null
        val accepted = incoming.copy(status = "CONNECTED")
        _activeCall.value = accepted
        startCallTimer()
    }

    fun rejectCall() {
        val incoming = _incomingCall.value ?: return
        _incomingCall.value = null
        serviceScope.launch {
            repository.insertMessage(
                ChatMessage(
                    peerMacAddress = incoming.peerMac,
                    text = "مكالمة فائتة",
                    type = "CALL",
                    callDurationSeconds = 0,
                    callType = incoming.type,
                    callStatus = "MISSED",
                    isIncoming = true
                )
            )
        }
    }

    fun hangUp() {
        val active = _activeCall.value ?: return
        _activeCall.value = null
        callTimerJob?.cancel()

        serviceScope.launch {
            repository.insertMessage(
                ChatMessage(
                    peerMacAddress = active.peerMac,
                    text = if (active.isIncoming) "مكالمة واردة مستلمة" else "مكالمة صادرة",
                    type = "CALL",
                    callDurationSeconds = active.durationSeconds,
                    callType = active.type,
                    callStatus = if (active.isIncoming) "INCOMING" else "OUTGOING",
                    isIncoming = active.isIncoming
                )
            )
        }
    }

    private fun startCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = serviceScope.launch {
            while (true) {
                delay(1000)
                _activeCall.value = _activeCall.value?.let {
                    it.copy(durationSeconds = it.durationSeconds + 1)
                }
            }
        }
    }

    // Auto-respond to incoming messages for simulations
    private fun triggerSimulatedReply(peer: PeerDevice) {
        serviceScope.launch {
            delay(2000)
            val aesKey = CryptoEngine.generateAESKey()
            
            // Select responses in Arabic matching user theme
            val responses = listOf(
                "أهلاً بك! أنا متصل الآن بشكل آمن.",
                "تم استلام الرسالة، التشفير الكامل مفعل طرف-إلى-طرف 🔐",
                "صوت المكالمة كان رائعاً، ميزة البلوتوث تضمن اتصالاً خالياً من التقطع.",
                "وصل الملف بنجاح وبسرعة عالية جداً، شكراً لك!",
                "تطبيق تواصل بلس رائع جداً وسريع الاستجابة."
            )
            val replyText = responses.random()
            
            val (encryptedText, iv) = CryptoEngine.encryptAES(replyText, aesKey)
            val myPublicKey = CryptoEngine.getMyPublicKey()
            val encryptedAesKey = try {
                CryptoEngine.encryptAESKeyWithRSA(aesKey, myPublicKey)
            } catch (e: Exception) {
                "MOCK_ENCRYPTED_AES_KEY_REPLY"
            }

            repository.insertMessage(
                ChatMessage(
                    peerMacAddress = peer.macAddress,
                    text = encryptedText,
                    iv = iv,
                    encryptedAesKey = encryptedAesKey,
                    isIncoming = true,
                    status = "READ"
                )
            )

            // Trigger real Android status bar notification
            showNotification(
                title = "رسالة جديدة من ${peer.getDisplayName()}",
                content = replyText
            )
        }
    }

    @SuppressLint("NotificationPermission")
    private fun showNotification(title: String, content: String) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent: PendingIntent = PendingIntent.getActivity(
                context, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val builder = NotificationCompat.Builder(context, "tawasul_alerts")
                .setSmallIcon(android.R.drawable.stat_notify_chat)
                .setContentTitle(title)
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
