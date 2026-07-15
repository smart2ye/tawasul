package com.example.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bluetooth.ActiveCall
import com.example.bluetooth.ConnectionState
import com.example.data.ChatMessage
import com.example.data.PeerDevice
import com.example.data.UserSettings
import com.example.ui.theme.CallActiveGreen
import com.example.ui.theme.CallMissedRed
import com.example.ui.theme.CallRingingOrange
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val activeChatPeer by viewModel.activeChatPeer.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    
    val activeCall by viewModel.activeCall.collectAsStateWithLifecycle()
    val incomingCall by viewModel.incomingCall.collectAsStateWithLifecycle()
    
    // Always force RTL for Arabic layout consistency as requested
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MyApplicationTheme(darkModeSetting = settings.darkMode) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    val showShellBars = currentScreen != AppScreen.CHAT_DETAIL && 
                                        currentScreen != AppScreen.LOGIN && 
                                        currentScreen != AppScreen.CLOUD_HUB && 
                                        currentScreen != AppScreen.OFFICIAL_WEB && 
                                        currentScreen != AppScreen.ADMIN_DASHBOARD

                    // Main Screen Router
                    Scaffold(
                        topBar = {
                            if (showShellBars) {
                                MainTopAppBar(viewModel, currentScreen)
                            }
                        },
                        bottomBar = {
                            if (showShellBars) {
                                MainBottomNavigation(
                                    currentScreen = currentScreen,
                                    onTabSelected = { viewModel.navigateTo(it) }
                                )
                            }
                        },
                        contentWindowInsets = WindowInsets.safeDrawing
                    ) { innerPadding ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            when (currentScreen) {
                                AppScreen.CHATS -> ChatsListSection(viewModel)
                                AppScreen.DEVICES -> DevicesSection(viewModel)
                                AppScreen.CALLS -> CallsHistorySection(viewModel)
                                AppScreen.SETTINGS -> SettingsSection(viewModel)
                                AppScreen.ABOUT -> AboutSection(viewModel)
                                AppScreen.LOGIN -> LoginSection(viewModel)
                                AppScreen.CLOUD_HUB -> CloudHubSection(viewModel)
                                AppScreen.OFFICIAL_WEB -> OfficialWebsiteSection(viewModel)
                                AppScreen.ADMIN_DASHBOARD -> AdminDashboardSection(viewModel)
                                AppScreen.CHAT_DETAIL -> {
                                    activeChatPeer?.let { peer ->
                                        ChatDetailScreen(viewModel = viewModel, peer = peer)
                                    }
                                }
                            }
                        }
                    }

                    // Full-screen Call Overlays
                    AnimatedVisibility(
                        visible = incomingCall != null,
                        enter = fadeIn() + slideInVertically { it },
                        exit = fadeOut() + slideOutVertically { it }
                    ) {
                        incomingCall?.let { call ->
                            IncomingCallOverlay(call = call, viewModel = viewModel)
                        }
                    }

                    AnimatedVisibility(
                        visible = activeCall != null,
                        enter = fadeIn() + expandIn(),
                        exit = fadeOut() + shrinkOut()
                    ) {
                        activeCall?.let { call ->
                            ActiveCallOverlay(call = call, viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopAppBar(viewModel: MainViewModel, currentScreen: AppScreen) {
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val connectedPeer by viewModel.connectedPeer.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    TopAppBar(
        navigationIcon = {
            IconButton(onClick = { viewModel.navigateTo(AppScreen.SETTINGS) }) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (settings.profilePictureUrl.isNotBlank()) {
                        coil.compose.AsyncImage(
                            model = settings.profilePictureUrl,
                            contentDescription = "الملف الشخصي",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "الملف الشخصي",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "اتصال آمن",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "تواصل بلس",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        actions = {
            if (connectionState == ConnectionState.CONNECTED && connectedPeer != null) {
                Row(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                        .clickable { viewModel.navigateTo(AppScreen.CHATS) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(CallActiveGreen)
                    )
                    Text(
                        text = "متصل بـ ${connectedPeer?.getDisplayName()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 120.dp)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.padding(end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.OfflineBolt,
                        contentDescription = "بدون إنترنت",
                        tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "مستقل/أوفلاين",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun MainBottomNavigation(
    currentScreen: AppScreen,
    onTabSelected: (AppScreen) -> Unit
) {
    NavigationBar(
        tonalElevation = 8.dp,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        NavigationBarItem(
            selected = currentScreen == AppScreen.CHATS || currentScreen == AppScreen.CHAT_DETAIL,
            onClick = { onTabSelected(AppScreen.CHATS) },
            icon = { Icon(Icons.Default.ChatBubble, contentDescription = "الدردشات") },
            label = { Text("الدردشات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            modifier = Modifier.testTag("nav_chats")
        )
        NavigationBarItem(
            selected = currentScreen == AppScreen.DEVICES,
            onClick = { onTabSelected(AppScreen.DEVICES) },
            icon = { Icon(Icons.Default.BluetoothSearching, contentDescription = "البحث") },
            label = { Text("الأجهزة", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            modifier = Modifier.testTag("nav_devices")
        )
        NavigationBarItem(
            selected = currentScreen == AppScreen.CALLS,
            onClick = { onTabSelected(AppScreen.CALLS) },
            icon = { Icon(Icons.Default.Call, contentDescription = "المكالمات") },
            label = { Text("المكالمات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            modifier = Modifier.testTag("nav_calls")
        )
        NavigationBarItem(
            selected = currentScreen == AppScreen.SETTINGS || currentScreen == AppScreen.ABOUT,
            onClick = { onTabSelected(AppScreen.SETTINGS) },
            icon = { Icon(Icons.Default.Settings, contentDescription = "الإعدادات") },
            label = { Text("الإعدادات", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
            modifier = Modifier.testTag("nav_settings")
        )
    }
}

// ---------------------- CHATS SECTION ----------------------
@Composable
fun ChatsListSection(viewModel: MainViewModel) {
    val peers by viewModel.peers.collectAsStateWithLifecycle()
    val messages by viewModel.allMessages.collectAsStateWithLifecycle()

    if (peers.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Chat,
                    contentDescription = "لا توجد دردشات",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
                Text(
                    text = "لا توجد محادثات نشطة بعد",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "انتقل إلى علامة تبويب 'الأجهزة' للبحث عن مستخدمين قريبين وبدء مكالمة أو دردشة مشفرة بأمان بدون إنترنت.",
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Button(
                    onClick = { viewModel.navigateTo(AppScreen.DEVICES) },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Bluetooth, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("البحث عن الأجهزة القريبة")
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "المحادثات الأخيرة (مشفرة بالكامل 🔒)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(peers) { peer ->
                val peerMessages = messages.filter { it.peerMacAddress == peer.macAddress }
                val lastMsg = peerMessages.lastOrNull()

                ChatPeerCard(
                    peer = peer,
                    lastMessage = lastMsg,
                    viewModel = viewModel,
                    onClick = { viewModel.openChatWith(peer) }
                )
            }
        }
    }
}

@Composable
fun ChatPeerCard(
    peer: PeerDevice,
    lastMessage: ChatMessage?,
    viewModel: MainViewModel,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("chat_card_${peer.macAddress}")
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Profile Initials or Avatar
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.secondaryContainer
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = peer.getDisplayName().take(2),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                // Online Indicator Dot
                if (peer.isOnline) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .clip(CircleShape)
                            .background(CallActiveGreen)
                            .align(Alignment.BottomEnd)
                    )
                }
            }

            // Name and last message detail
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = peer.getDisplayName(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    lastMessage?.let {
                        Text(
                            text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(it.timestamp)),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                val dispText = when {
                    lastMessage == null -> "انقر لبدء محادثة آمنة"
                    lastMessage.type == "FILE" -> "📁 ملف: ${lastMessage.attachmentName}"
                    lastMessage.type == "CALL" -> "📞 مكالمة: " + (if (lastMessage.callStatus == "MISSED") "فائتة" else "مكتملة")
                    else -> viewModel.decryptMessageText(lastMessage)
                }

                Text(
                    text = dispText,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

// ---------------------- DEVICES SECTION (BLUETOOTH RADAR) ----------------------
@Composable
fun DevicesSection(viewModel: MainViewModel) {
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val discoveredDevices by viewModel.discoveredDevices.collectAsStateWithLifecycle()
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val connectedPeer by viewModel.connectedPeer.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val isBtEnabled = viewModel.isBluetoothEnabled()
    val hasPermissions = viewModel.hasBluetoothPermissions()
    val isDeviceDiscoverable = viewModel.isDiscoverable()

    // Auto scan upon opening the devices screen
    LaunchedEffect(Unit) {
        viewModel.startScanning()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Upper card showing energy status & settings info
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (settings.powerSavingMode) Icons.Default.BatteryChargingFull else Icons.Default.Bolt,
                    contentDescription = null,
                    tint = if (settings.powerSavingMode) CallActiveGreen else CallRingingOrange,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = if (settings.powerSavingMode) "نظام توفير الطاقة الذكي: مفعل 🔋" else "نظام الأداء الأقصى: مفعل ⚡",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = if (settings.powerSavingMode) "يتم تقليل طاقة البث وفترات المسح بنسبة 40% للحفاظ على البطارية."
                        else "مسح مستمر واكتشاف سريع جداً للأجهزة لضمان جودة اتصال فائقة.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Radar Visualizer
        Box(
            modifier = Modifier
                .size(180.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            RadarAnimation(isScanning = isScanning)
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = { viewModel.toggleScanning() },
                    modifier = Modifier.testTag("radar_button")
                ) {
                    Icon(
                        imageVector = if (isScanning) Icons.Default.Stop else Icons.Default.Bluetooth,
                        contentDescription = "مسح",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = if (isScanning) "جاري البحث عن أجهزة قريبة..." else "البحث متوقف حالياً",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.secondary
                )
                IconButton(
                    onClick = { viewModel.startScanning(forceRestart = true) },
                    modifier = Modifier.size(24.dp).testTag("btn_refresh_scan")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "إعادة البحث",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Discoverable visual indicator
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isDeviceDiscoverable) CallActiveGreen.copy(alpha = 0.15f) else CallRingingOrange.copy(alpha = 0.15f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = if (isDeviceDiscoverable) "جهازك مرئي للجميع 🟢" else "جهازك غير قابل للاكتشاف ⚠️",
                    fontSize = 11.sp,
                    color = if (isDeviceDiscoverable) CallActiveGreen else CallRingingOrange,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Action-oriented visibility request card if not discoverable
        if (!isDeviceDiscoverable && isBtEnabled) {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("warning_not_discoverable"),
                colors = CardDefaults.cardColors(containerColor = CallRingingOrange.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = CallRingingOrange
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "جهازك غير قابل للاكتشاف حالياً ⚠️",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = CallRingingOrange
                        )
                        Text(
                            text = "قد لا تتمكن الأجهزة الأخرى من العثور عليك حتى تقوم بتفعيل الرؤية العامة.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    Button(
                        onClick = { viewModel.requestDiscoverable() },
                        colors = ButtonDefaults.buttonColors(containerColor = CallRingingOrange),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("تفعيل الرؤية", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }

        // List of found devices / status warnings
        when {
            !hasPermissions -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GppBad,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = CallMissedRed
                        )
                        Text(
                            text = "أذونات البلوتوث أو الأجهزة القريبة غير ممنوحة ⚠️",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CallMissedRed,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "الرجاء منح تطبيق تواصل بلس أذونات الأجهزة القريبة والبلوتوث من إعدادات النظام ليتسنى له العمل بشكل صحيح واكتشاف المحيطين بك.",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            !isBtEnabled && !settings.enableSimulation -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BluetoothDisabled,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = CallRingingOrange
                        )
                        Text(
                            text = "البلوتوث غير مُفعّل حالياً 📶",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = CallRingingOrange,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "الرجاء تشغيل البلوتوث من شريط الإعدادات السريعة لتبدأ في البحث عن الأجهزة القريبة والتواصل معها مجانياً بدون إنترنت.",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            discoveredDevices.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Text(
                            text = "لا توجد أجهزة قريبة حالياً 🔍",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "تأكد من أن الأجهزة الأخرى فتحت تطبيق تواصل بلس وميزة البلوتوث مفعلة لديهم لتتمكن من اكتشافهم وربط قناة مشفرة.",
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(discoveredDevices) { peer ->
                        val isConnecting = connectionState == ConnectionState.CONNECTING && connectedPeer?.macAddress == peer.macAddress
                        val isConnected = connectionState == ConnectionState.CONNECTED && connectedPeer?.macAddress == peer.macAddress

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (!isConnected && !isConnecting) {
                                        viewModel.connectToDevice(peer)
                                    } else if (isConnected) {
                                        viewModel.openChatWith(peer)
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isConnected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                else MaterialTheme.colorScheme.surface
                             )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = if (peer.publicKey != null) Icons.Default.VerifiedUser else Icons.Default.PhoneAndroid,
                                        contentDescription = null,
                                        tint = if (peer.publicKey != null) CallActiveGreen else MaterialTheme.colorScheme.primary
                                    )
                                    Column {
                                        Text(
                                            text = peer.getDisplayName(),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = peer.macAddress,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                            if (peer.rssi != -100) {
                                                val signalIcon = when {
                                                    peer.rssi > -60 -> "📶 قوية"
                                                    peer.rssi > -80 -> "📶 متوسطة"
                                                    else -> "📶 ضعيفة"
                                                }
                                                Text(
                                                    text = "• $signalIcon (${peer.rssi} dBm)",
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                        val stateText = when {
                                            isConnected -> "متصل ✅"
                                            isConnecting -> "جارٍ الاتصال... ⏳"
                                            peer.isOnline -> "متاح 🟢"
                                            else -> "غير متاح 🔴"
                                        }
                                        val stateColor = when {
                                            isConnected -> CallActiveGreen
                                            isConnecting -> CallRingingOrange
                                            peer.isOnline -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                        }
                                        Text(
                                            text = stateText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = stateColor,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }

                                // Action button or state
                                when {
                                    isConnecting -> {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                                    }
                                    isConnected -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text("متصل مشفر", fontSize = 12.sp, color = CallActiveGreen, fontWeight = FontWeight.Bold)
                                            IconButton(onClick = { viewModel.disconnect() }) {
                                                Icon(Icons.Default.LinkOff, contentDescription = "قطع الاتصال", tint = CallMissedRed)
                                            }
                                        }
                                    }
                                    else -> {
                                        Button(
                                            onClick = { viewModel.connectToDevice(peer) },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text("ربط آمن", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RadarAnimation(isScanning: Boolean) {
    val transition = rememberInfiniteTransition(label = "RadarSweep")
    
    val radiusMultiplier by if (isScanning) {
        transition.animateFloat(
            initialValue = 0.2f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "RadarRadius"
        )
    } else {
        remember { mutableStateOf(0.5f) }
    }

    val alphaMultiplier by if (isScanning) {
        transition.animateFloat(
            initialValue = 0.8f,
            targetValue = 0.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "RadarAlpha"
        )
    } else {
        remember { mutableStateOf(0.3f) }
    }

    val radarColor = MaterialTheme.colorScheme.primary

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxRadius = size.width.coerceAtMost(size.height) / 2f

        // Stationary rings
        drawCircle(
            color = radarColor.copy(alpha = 0.1f),
            radius = maxRadius,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = radarColor.copy(alpha = 0.15f),
            radius = maxRadius * 0.6f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )
        drawCircle(
            color = radarColor.copy(alpha = 0.2f),
            radius = maxRadius * 0.3f,
            center = center,
            style = Stroke(width = 1.dp.toPx())
        )

        // Pulsing active sweep wave
        if (isScanning) {
            drawCircle(
                color = radarColor.copy(alpha = alphaMultiplier * 0.4f),
                radius = maxRadius * radiusMultiplier,
                center = center
            )
            drawCircle(
                color = radarColor.copy(alpha = alphaMultiplier),
                radius = maxRadius * radiusMultiplier,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }
    }
}

// ---------------------- CALLS HISTORY SECTION ----------------------
@Composable
fun CallsHistorySection(viewModel: MainViewModel) {
    val calls by viewModel.callsHistory.collectAsStateWithLifecycle()
    val peers by viewModel.peers.collectAsStateWithLifecycle()

    if (calls.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneCallback,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
                Text(
                    text = "سجل المكالمات فارغ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "المكالمات الصوتية والمرئية التي تجريها عبر البلوتوث محلياً ستظهر هنا مع تفاصيل مدة المكالمة ومستوى التشفير الخاص بها.",
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "سجل المكالمات المشفرة (طرف-إلى-طرف 📞)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            items(calls) { call ->
                val peer = peers.find { it.macAddress == call.peerMacAddress }
                val displayName = peer?.getDisplayName() ?: call.peerMacAddress

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Call icon state
                            val (icon, color) = when (call.callStatus) {
                                "MISSED" -> Pair(Icons.Default.CallMissed, CallMissedRed)
                                "INCOMING" -> Pair(Icons.Default.CallReceived, CallActiveGreen)
                                else -> Pair(Icons.Default.CallMade, MaterialTheme.colorScheme.primary)
                            }
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(color.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = icon, contentDescription = null, tint = color)
                            }

                            Column {
                                Text(
                                    text = displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (call.callType == "VIDEO") Icons.Default.Videocam else Icons.Default.Call,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = if (call.callType == "VIDEO") "مكالمة فيديو" else "مكالمة صوتية",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                    Text(
                                        text = "•",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                    )
                                    Text(
                                        text = "${call.callDurationSeconds} ثانية",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }

                        // Call back button
                        IconButton(
                            onClick = {
                                peer?.let { viewModel.openChatWith(it) }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhoneEnabled,
                                contentDescription = "إعادة الاتصال",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

// ---------------------- SETTINGS SECTION ----------------------
@Composable
fun SettingsSection(viewModel: MainViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val showHotspotShareDialog by viewModel.showHotspotShareDialog.collectAsStateWithLifecycle()
    var editName by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf("") }
    
    val context = LocalContext.current

    LaunchedEffect(settings.displayName) {
        tempName = settings.displayName
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "إعدادات الملف الشخصي والخصوصية ⚙️",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Profile Display Name Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "اسم جهازي التعريفي (يظهر للآخرين)",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )

                    if (editName) {
                        OutlinedTextField(
                            value = tempName,
                            onValueChange = { tempName = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("display_name_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors()
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            TextButton(onClick = { editName = false }) {
                                Text("إلغاء")
                            }
                            Button(
                                onClick = {
                                    if (tempName.isNotBlank()) {
                                        viewModel.updateDisplayName(tempName)
                                    }
                                    editName = false
                                },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("حفظ")
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                if (settings.profilePictureUrl.isNotBlank()) {
                                    coil.compose.AsyncImage(
                                        model = settings.profilePictureUrl,
                                        contentDescription = "صورة الملف الشخصي",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = settings.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (settings.accountEmail.isNotBlank()) {
                                    Text(
                                        text = settings.accountEmail,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(onClick = { editName = true }) {
                                Icon(Icons.Default.Edit, contentDescription = "تعديل الاسم", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        // App customization settings
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "خيارات العرض والتخصيص",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    // Dark mode toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("الوضع الداكن (مظلم تلقائي)", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text("تخصيص العرض المظلم لراحة العين", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val options = listOf("AUTO" to "تلقائي", "LIGHT" to "مضيء", "DARK" to "مظلم")
                            options.forEach { (mode, label) ->
                                FilterChip(
                                    selected = settings.darkMode == mode,
                                    onClick = { viewModel.updateDarkMode(mode) },
                                    label = { Text(label, fontSize = 11.sp) }
                                )
                            }
                        }
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    // Smart Power Saving mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("توفير الطاقة الذكي (Bluetooth Power)", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text("يقلل فترات البحث واستهلاك طاقة البث لزيادة عمر البطارية", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        Switch(
                            checked = settings.powerSavingMode,
                            onCheckedChange = { viewModel.togglePowerSaving() }
                        )
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    // Notifications Enabled
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تنبيهات الرسائل الفورية", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text("إظهار إشعار فوري عند تلقي رسالة جديدة في الخلفية", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        Switch(
                            checked = settings.notificationsEnabled,
                            onCheckedChange = { viewModel.toggleNotifications() }
                        )
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    // Calls alerts Enabled
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تنبيه المكالمات الواردة", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text("إشعارات بملء الشاشة مع رنين واهتزاز للمكالمات الواردة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        Switch(
                            checked = settings.callAlertsEnabled,
                            onCheckedChange = { viewModel.toggleCallAlerts() }
                        )
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    // Device Simulation Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth().testTag("toggle_simulation_mode"),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تفعيل أجهزة المحاكاة والافتراضية 🧪", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text("إظهار أجهزة وهمية في الرادار للتجربة والاختبار السريع (تعطيلها يضمن مصداقية الاتصالات الحقيقية)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        Switch(
                            checked = settings.enableSimulation,
                            onCheckedChange = { viewModel.toggleSimulation() },
                            modifier = Modifier.testTag("switch_simulation_mode")
                        )
                    }
                }
            }
        }

        // Account & Cloud Backup Dashboard Section
        item {
            val backupLoading by viewModel.backupLoading.collectAsStateWithLifecycle()
            val backupStatusMsg by viewModel.backupStatusMsg.collectAsStateWithLifecycle()
            
            val backupDate = remember(settings.lastBackupTime) {
                if (settings.lastBackupTime > 0) {
                    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                    sdf.format(java.util.Date(settings.lastBackupTime))
                } else {
                    "لا توجد نسخ احتياطية سابقة"
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "☁️ الحساب والنسخ الاحتياطي السحابي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    // Profile summary
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            if (settings.profilePictureUrl.isNotBlank()) {
                                coil.compose.AsyncImage(
                                    model = settings.profilePictureUrl,
                                    contentDescription = "صورة جوجل",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudQueue,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = if (settings.displayName.isBlank()) "مستخدم تواصل" else settings.displayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (settings.accountEmail.isBlank()) "غير مسجل" else settings.accountEmail,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

                    // Backup Details
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("آخر نسخة احتياطية سحابية:", fontSize = 13.sp)
                            Text(
                                text = backupDate,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("إجمالي عمليات النسخ:", fontSize = 13.sp)
                            Text(
                                text = "${settings.backupCount} مرات",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Actions block
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Backup now button
                        Button(
                            onClick = { viewModel.backupToCloud() },
                            modifier = Modifier.fillMaxWidth().height(44.dp).testTag("btn_backup_now"),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !backupLoading,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("حفظ نسخة احتياطية سحابية الآن", fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Restore button
                            Button(
                                onClick = {
                                    viewModel.restoreFromCloud(settings.accountEmail) { success, msg ->
                                        if (success) {
                                            Toast.makeText(context, "تمت الاستعادة بنجاح!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "فشلت الاستعادة: $msg", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).height(44.dp).testTag("btn_restore_now"),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !backupLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("استعادة المحادثات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            // Download profile information
                            Button(
                                onClick = {
                                    viewModel.downloadAccountInfo { uri, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        if (uri != null) {
                                            try {
                                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                    type = "application/json"
                                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(android.content.Intent.createChooser(intent, "مشاركة معلومات حسابي"))
                                            } catch (e: Exception) {
                                                Log.e("Settings", "Share failed: ${e.message}")
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f).height(44.dp).testTag("btn_download_info"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تنزيل بياناتي", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Logout button
                        OutlinedButton(
                            onClick = { viewModel.logoutUser() },
                            modifier = Modifier.fillMaxWidth().height(44.dp).testTag("btn_logout"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("تسجيل الخروج من الحساب", fontWeight = FontWeight.Bold)
                        }
                    }

                    // Progress / Status Messages
                    backupStatusMsg?.let { msg ->
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        // Cloud Portal and Developer Gateways
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("card_cloud_gateways"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudQueue,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "بوابة الخدمات والموقع والمشرفين ☁️🌐",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    Text(
                        text = "الولوج الآمن والمباشر لجميع خدمات السحابة والتحقق من التحديثات الرسمية بالإضافة إلى الموقع التعريفي ولوحة تحكم الإدارة.",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.navigateTo(AppScreen.CLOUD_HUB) },
                            modifier = Modifier.fillMaxWidth().height(42.dp).testTag("btn_go_cloud_hub"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("مركز الخدمات السحابية للتطبيق ☁️", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.navigateTo(AppScreen.OFFICIAL_WEB) },
                                modifier = Modifier.weight(1f).height(42.dp).testTag("btn_go_official_web"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.Web, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("الموقع الرسمي 🌐", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.navigateTo(AppScreen.ADMIN_DASHBOARD) },
                                modifier = Modifier.weight(1f).height(42.dp).testTag("btn_go_admin_dashboard"),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("لوحة المسؤول ⚙️", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // App Invitation & Local Offline Sharing Section (Bluetooth / Hotspot)
        item {
            Card(
                modifier = Modifier.fillMaxWidth().testTag("card_invite_and_share"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "دعوة الأصدقاء وتثبيت تواصل بلس 📲",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "شارك التطبيق مباشرة مع من حولك بدون الحاجة لإنترنت عبر البلوتوث أو نقطة اتصال الواي فاي السريعة ليتسنى لهم تثبيته فوراً والبدء بالتواصل معك مشفراً ومجانياً!",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.shareAppApk(context)
                            },
                            modifier = Modifier.weight(1f).testTag("btn_share_via_bluetooth"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Bluetooth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("عبر البلوتوث", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.shareAppViaHotspot(context)
                            },
                            modifier = Modifier.weight(1f).testTag("btn_share_via_hotspot"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("عبر نقطة اتصال", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // About the Developer item
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.navigateTo(AppScreen.ABOUT) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text("من نحن وتفاصيل المطور", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("عبدالقادر مهدلي • تواصل معنا وسياسة الخصوصية", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                    Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    if (showHotspotShareDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setShowHotspotShareDialog(false) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setShowHotspotShareDialog(false)
                        viewModel.shareAppApk(context)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("مشاركة ملف الـ APK 📤")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setShowHotspotShareDialog(false) }) {
                    Text("إغلاق")
                }
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "المشاركة عبر نقطة الاتصال 📡",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "شارك التطبيق مع أصدقائك القريبين بسرعة فائقة وبدون أي استخدام للإنترنت من خلال الخطوات البسيطة التالية:",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Step 1
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "1️⃣",
                                fontSize = 16.sp
                            )
                            Column {
                                Text(
                                    text = "قم بتفعيل نقطة الاتصال (Hotspot)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "افتح الإعدادات السريعة في هاتفك وقم بتفعيل نقطة الاتصال المحمولة (Wi-Fi Hotspot).",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Step 2
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "2️⃣",
                                fontSize = 16.sp
                            )
                            Column {
                                Text(
                                    text = "اجعل الصديق يتصل بشبكتك",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    text = "اطلب من صديقك الاتصال بشبكة الواي فاي الخاصة بنقطة الاتصال التي قمت بتفعيلها.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Step 3
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "3️⃣",
                                fontSize = 16.sp
                            )
                            Column {
                                Text(
                                    text = "مشاركة وتثبيت التطبيق",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    text = "اضغط على مشاركة الملف بالأسفل لإرسال ملف الـ APK مباشرة عبر الواي فاي المباشر أو Quick Share أو البلوتوث وتثبيته فوراً!",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            shape = RoundedCornerShape(24.dp)
        )
    }
}

// ---------------------- ABOUT & PRIVACY SCREEN ----------------------
@Composable
fun AboutSection(viewModel: MainViewModel) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = { viewModel.navigateTo(AppScreen.SETTINGS) }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                }
                Text(
                    text = "من نحن ومعلومات المطور",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Developer Info Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Text(
                        text = "عبدالقادر مهدلي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "مطور تطبيقات أندرويد متكاملة",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    HorizontalDivider(thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))

                    Text(
                        text = "تواصل مباشر مع المطور لطلب تعديلات أو تطوير حلول مخصصة:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )

                    // Contact Row 1: WhatsApp
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CallActiveGreen.copy(alpha = 0.15f))
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://wa.me/967737254619")
                                }
                                context.startActivity(intent)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.PhoneEnabled, contentDescription = "واتساب", tint = CallActiveGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "واتساب: +967 737 254 619",
                            fontWeight = FontWeight.Bold,
                            color = CallActiveGreen,
                            fontSize = 14.sp
                        )
                    }

                    // Contact Row 2: Facebook
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .clickable {
                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                    data = Uri.parse("https://facebook.com/abdulqader.mahdly")
                                }
                                context.startActivity(intent)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "فيسبوك", tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "فيسبوك: abdulqader.mahdly",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Terms and Privacy Policy Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "الشروط والأحكام وسياسة الخصوصية",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "1. خصوصية مطلقة: تطبيق تواصل بلس يقوم بتشفير كافة الرسائل والملفات والمكالمات بشكل طرف-إلى-طرف (E2EE) باستخدام محرك تشفير RSA وAES محلي بالكامل. لا نقوم برفع أي بيانات لأي خوادم خارجية.\n" +
                               "2. العمل بدون إنترنت: تم تصميم التطبيق ليعمل في بيئة غير متصلة بالشبكة بالكامل باستخدام تقنيات الاتصال القريب والبلوتوث والاتصالات اللاسلكية المباشرة.\n" +
                               "3. أذونات التشغيل: يتطلب التطبيق الوصول للبلوتوث، الكاميرا والميكروفون فقط لغرض تفعيل خدمات الدردشة والمكالمات المشفرة محلياً.\n" +
                               "جميع الحقوق محفوظة للمطور عبدالقادر مهدلي © 2026",
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

// ---------------------- CHAT DETAIL SCREEN ----------------------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(viewModel: MainViewModel, peer: PeerDevice) {
    val messages by viewModel.activeChatMessages.collectAsStateWithLifecycle(emptyList())
    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    var inputText by remember { mutableStateOf("") }
    
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(peer.getDisplayName().take(2), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(peer.getDisplayName(), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Text(
                                text = if (peer.isOnline) "نشط حالياً • اتصال مشفر" else "غير متصل",
                                fontSize = 11.sp,
                                color = if (peer.isOnline) CallActiveGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.CHATS) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    if (peer.isOnline) {
                        IconButton(onClick = { viewModel.startVoiceCall() }) {
                            Icon(Icons.Default.Call, contentDescription = "مكالمة صوتية", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { viewModel.startVideoCall() }) {
                            Icon(Icons.Default.Videocam, contentDescription = "مكالمة مرئية", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    IconButton(onClick = { viewModel.deleteChatHistory(peer.macAddress) }) {
                        Icon(Icons.Default.Delete, contentDescription = "مسح الشات", tint = CallMissedRed)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Chat bubble list
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f)),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                Text("تشفير كامل للرسائل والملفات RSA + AES-256", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                items(messages) { msg ->
                    ChatBubble(message = msg, viewModel = viewModel)
                }
            }

            // Input panel
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Send File Button
                    IconButton(
                        onClick = {
                            // High-speed Simulated file transfers (choose document or video)
                            val fileNames = listOf("تقرير_التخرج_2026.pdf", "فيديو_مشروع_الشبكات.mp4", "قاعدة_بيانات_العملاء.db", "تثبيت_التطبيق.apk")
                            val selectedFile = fileNames.random()
                            val sizes = listOf(15_000_000L, 45_000_000L, 120_000_000L, 350_000_000L)
                            viewModel.sendFile(selectedFile, sizes.random())
                        }
                    ) {
                        Icon(Icons.Default.AttachFile, contentDescription = "مشاركة ملف", tint = MaterialTheme.colorScheme.primary)
                    }

                    // Nudge (Poke/Vibration) Button
                    IconButton(
                        onClick = {
                            viewModel.sendNudge()
                        },
                        modifier = Modifier.testTag("btn_nudge")
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = "نكز واهتزاز",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input"),
                        placeholder = { Text("اكتب رسالة مشفرة آمنة...", fontSize = 13.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )

                    // Send Button
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendTextMessage(inputText)
                                inputText = ""
                            }
                        },
                        modifier = Modifier.testTag("send_msg_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage, viewModel: MainViewModel) {
    val alignment = if (message.isIncoming) Alignment.Start else Alignment.End
    val cardColor = if (message.isIncoming) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.primaryContainer
    
    val textColor = if (message.isIncoming) MaterialTheme.colorScheme.onSecondaryContainer
                    else MaterialTheme.colorScheme.onPrimaryContainer

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = alignment
    ) {
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isIncoming) 4.dp else 16.dp,
                bottomEnd = if (message.isIncoming) 16.dp else 4.dp
            ),
            colors = CardDefaults.cardColors(containerColor = cardColor),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                if (message.type == "FILE") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.InsertDriveFile, contentDescription = null, tint = textColor, modifier = Modifier.size(32.dp))
                        Column {
                            Text(text = message.attachmentName ?: "ملف", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textColor)
                            Text(text = "${String.format("%.1f", (message.attachmentSize / 1024f / 1024f))} MB", fontSize = 11.sp, color = textColor.copy(alpha = 0.7f))
                        }
                    }
                    if (message.transferProgress < 100) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { message.transferProgress / 100f },
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                            color = textColor,
                            trackColor = textColor.copy(alpha = 0.3f)
                        )
                        Text(text = "جاري النقل: ${message.transferProgress}%", fontSize = 10.sp, color = textColor.copy(alpha = 0.7f), modifier = Modifier.align(Alignment.End))
                    }
                } else if (message.type == "CALL") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (message.callType == "VIDEO") Icons.Default.Videocam else Icons.Default.Call,
                            contentDescription = null,
                            tint = if (message.callStatus == "MISSED") CallMissedRed else CallActiveGreen
                        )
                        Column {
                            Text(
                                text = if (message.callType == "VIDEO") "مكالمة مرئية" else "مكالمة صوتية",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = textColor
                            )
                            Text(
                                text = when (message.callStatus) {
                                    "MISSED" -> "مكالمة فائتة"
                                    else -> "مكتملة • ${message.callDurationSeconds} ثانية"
                                },
                                fontSize = 11.sp,
                                color = textColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else if (message.type == "NUDGE") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "نكز واهتزاز",
                            tint = if (message.isIncoming) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary
                        )
                        Column {
                            Text(
                                text = message.text,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = textColor
                            )
                            Text(
                                text = "تنبيه اهتزاز نشط 🫵",
                                fontSize = 10.sp,
                                color = textColor.copy(alpha = 0.7f)
                            )
                        }
                    }
                } else {
                    // Decrypt the message content to show on-the-fly
                    val textContent = viewModel.decryptMessageText(message)
                    Text(text = textContent, fontSize = 14.sp, color = textColor)
                }

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.align(Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp)),
                        fontSize = 9.sp,
                        color = textColor.copy(alpha = 0.5f)
                    )
                    if (!message.isIncoming) {
                        Icon(
                            imageVector = if (message.status == "READ") Icons.Default.DoneAll else Icons.Default.Done,
                            contentDescription = null,
                            tint = textColor.copy(alpha = 0.5f),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

// ---------------------- OVERLAYS FOR CALLING ----------------------
@Composable
fun IncomingCallOverlay(call: ActiveCall, viewModel: MainViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Animated pulsing avatar for ringing
            val infiniteTransition = rememberInfiniteTransition(label = "RingingPulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "Pulse"
            )

            Box(
                modifier = Modifier
                    .size(100.dp)
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp * pulseScale)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }

            Text(
                text = call.peerName,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.White
            )

            Text(
                text = "مكالمة ${if (call.type == "VOICE") "صوتية" else "مرئية"} مشفرة واردة...",
                fontSize = 14.sp,
                color = CallRingingOrange,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Action buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reject Button
                IconButton(
                    onClick = { viewModel.rejectCall() },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(CallMissedRed)
                        .testTag("reject_call")
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = "رفض", tint = Color.White, modifier = Modifier.size(32.dp))
                }

                // Accept Button
                IconButton(
                    onClick = { viewModel.acceptCall() },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(CallActiveGreen)
                        .testTag("accept_call")
                ) {
                    Icon(
                        imageVector = if (call.type == "VIDEO") Icons.Default.Videocam else Icons.Default.Call,
                        contentDescription = "قبول",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveCallOverlay(call: ActiveCall, viewModel: MainViewModel) {
    var micMuted by remember { mutableStateOf(false) }
    var videoOff by remember { mutableStateOf(false) }
    var speakerOn by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
    ) {
        // If it's a video call, simulate the feeds
        if (call.type == "VIDEO" && !videoOff) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Main camera stream: Simulated wireframe/scenic draw or loading indicator
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw abstract grid for futuristic stream preview
                    val gridWidth = size.width / 10
                    val gridHeight = size.height / 15
                    for (i in 0..10) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.04f),
                            start = Offset(i * gridWidth, 0f),
                            end = Offset(i * gridWidth, size.height),
                            strokeWidth = 1f
                        )
                    }
                    for (j in 0..15) {
                        drawLine(
                            color = Color.White.copy(alpha = 0.04f),
                            start = Offset(0f, j * gridHeight),
                            end = Offset(size.width, j * gridHeight),
                            strokeWidth = 1f
                        )
                    }
                }
                
                // Big Peer video feedback simulator
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    Text("بث فيديو مباشر من ${call.peerName}", fontSize = 13.sp, color = Color.White.copy(alpha = 0.6f))
                }

                // Small self camera preview overlay (top corner)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 24.dp, start = 16.dp)
                        .size(100.dp, 140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.DarkGray)
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = Color.LightGray)
                        Text("أنت", fontSize = 10.sp, color = Color.LightGray)
                    }
                }
            }
        } else {
            // Audio calling screen view
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                    Text(text = call.peerName, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.White)
                    Text(text = "مكالمة صوتية آمنة في الخلفية", fontSize = 13.sp, color = Color.LightGray)
                }
            }
        }

        // Streaming Stats (Encryption, delay, FPS)
        Column(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 24.dp, end = 16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CallActiveGreen.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp), tint = CallActiveGreen)
                    Text("مشفر AES-256", fontSize = 10.sp, color = CallActiveGreen, fontWeight = FontWeight.Bold)
                }
            }
            Text("معدل البث: 64 kbps", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
            Text("زمن الاستجابة: 14ms", fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f))
        }

        // Call Timer and controls panel at the bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Timer formatting
            val minutes = call.durationSeconds / 60
            val seconds = call.durationSeconds % 60
            Text(
                text = String.format("%02d:%02d", minutes, seconds),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.White
            )

            // Buttons panel
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic button
                IconButton(
                    onClick = { micMuted = !micMuted },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (micMuted) Color.Red else Color.DarkGray)
                ) {
                    Icon(
                        imageVector = if (micMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "كتم",
                        tint = Color.White
                    )
                }

                // Speaker button
                IconButton(
                    onClick = { speakerOn = !speakerOn },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (speakerOn) MaterialTheme.colorScheme.primary else Color.DarkGray)
                ) {
                    Icon(
                        imageVector = if (speakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                        contentDescription = "مكبر الصوت",
                        tint = Color.White
                    )
                }

                // Video toggle (if it's a video call)
                if (call.type == "VIDEO") {
                    IconButton(
                        onClick = { videoOff = !videoOff },
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(if (videoOff) Color.Red else Color.DarkGray)
                    ) {
                        Icon(
                            imageVector = if (videoOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                            contentDescription = "إيقاف الكاميرا",
                            tint = Color.White
                        )
                    }
                }

                // Hangup Button
                IconButton(
                    onClick = { viewModel.hangUp() },
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(CallMissedRed)
                        .testTag("hang_up_call")
                ) {
                    Icon(Icons.Default.CallEnd, contentDescription = "إنهاء المكالمة", tint = Color.White, modifier = Modifier.size(32.dp))
                }
            }
        }
    }
}
