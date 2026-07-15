package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.*
import java.text.SimpleDateFormat
import java.util.*

// Helper for formatting time
private fun formatCloudTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

// Custom Deep Purple & Navy Blue gradient for headers
private val CloudHeaderGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF6200EE), Color(0xFF3700B3))
)

// Custom Slate Grey gradient for Admin Dashboard
private val AdminHeaderGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A))
)

// =========================================================================
// 1. CLOUD HUB SCREEN (مركز الخدمات السحابية للمستخدم)
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudHubSection(viewModel: MainViewModel) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val sessions by viewModel.cloudSessions.collectAsStateWithLifecycle()
    val tickets by viewModel.cloudTickets.collectAsStateWithLifecycle()
    val versions by viewModel.cloudVersions.collectAsStateWithLifecycle()
    val notifications by viewModel.cloudNotifications.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0: Sessions, 1: Backups, 2: Tickets, 3: Updates
    
    // Support Ticket Form State
    var ticketTitle by remember { mutableStateOf("") }
    var ticketCategory by remember { mutableStateOf("GENERAL") }
    var ticketDesc by remember { mutableStateOf("") }
    var expandedCategory by remember { mutableStateOf(false) }
    var selectedTicketForChat by remember { mutableStateOf<SupportTicket?>(null) }

    // Feedback Form State
    var feedbackType by remember { mutableStateOf("FEEDBACK") } // FEEDBACK or CRASH
    var feedbackContent by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.refreshCloudData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("بوابة الخدمات السحابية ☁️", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.SETTINGS) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // User Header Profile Banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CloudHeaderGradient)
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
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
                            Icon(Icons.Default.CloudQueue, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(settings.displayName, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(settings.accountEmail, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("الخدمة نشطة", color = Color.Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Tabs Menu
            TabRow(
                selectedTabIndex = activeTab,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(selected = activeTab == 0, onClick = { activeTab = 0; selectedTicketForChat = null }, text = { Text("الأجهزة والجلسات", fontSize = 11.sp, fontWeight = FontWeight.Bold) })
                Tab(selected = activeTab == 1, onClick = { activeTab = 1; selectedTicketForChat = null }, text = { Text("النسخ الاحتياطي", fontSize = 11.sp, fontWeight = FontWeight.Bold) })
                Tab(selected = activeTab == 2, onClick = { activeTab = 2 }, text = { Text("الدعم الفني", fontSize = 11.sp, fontWeight = FontWeight.Bold) })
                Tab(selected = activeTab == 3, onClick = { activeTab = 3; selectedTicketForChat = null }, text = { Text("التحديثات والإشعارات", fontSize = 11.sp, fontWeight = FontWeight.Bold) })
            }

            // Main Tab Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
            ) {
                when (activeTab) {
                    0 -> {
                        // Sessions & Connected Devices View
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                Text("إدارة الأجهزة والجلسات النشطة 📱💻", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                                Text("يعرض الأجهزة المتصلة بحسابك حالياً. يمكنك تتبعها وفصل أي جلسة مشبوهة لحماية بياناتك.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                            }

                            if (sessions.isEmpty()) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                        Text("لا توجد جلسات مسجلة", color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            } else {
                                items(sessions) { session ->
                                    ElevatedCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (session.deviceType == "Android") Icons.Default.PhoneAndroid else Icons.Default.Computer,
                                                contentDescription = null,
                                                tint = if (session.isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.size(32.dp)
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Text(session.deviceName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    if (session.isActive) {
                                                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color.Green))
                                                        Text("نشط حالياً", color = Color.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    } else {
                                                        Text("جلسة منتهية", color = MaterialTheme.colorScheme.outline, fontSize = 10.sp)
                                                    }
                                                }
                                                Text("نوع الجهاز: ${session.deviceType} • عنوان الـ IP: ${session.ipAddress}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                Text("تاريخ الدخول: ${formatCloudTime(session.loginTime)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                            }
                                            if (session.isActive) {
                                                OutlinedButton(
                                                    onClick = {
                                                        viewModel.revokeCloudSession(session.id)
                                                        Toast.makeText(context, "تم فصل الجلسة بنجاح!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text("إنهاء", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // Backup & Restores View
                        val backupLoading by viewModel.backupLoading.collectAsStateWithLifecycle()
                        val backupStatusMsg by viewModel.backupStatusMsg.collectAsStateWithLifecycle()
                        val backupDate = remember(settings.lastBackupTime) {
                            if (settings.lastBackupTime > 0) formatCloudTime(settings.lastBackupTime) else "لا توجد نسخ احتياطية سابقة"
                        }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                Text("إدارة وتشفير النسخ الاحتياطية 🔒☁️", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                                Text("يتم تشفير كافة محادثاتك وجهات اتصالك وإعداداتك محلياً بمفتاح تشفير مشتق من كلمة مرورك قبل رفعها على السحابة، مما يضمن استحالة قراءة بياناتك من قبل أي طرف خارجي (Zero-Knowledge Encryption).", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("آخر نسخة احتياطية سحابية:", fontSize = 13.sp)
                                            Text(backupDate, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("إجمالي عمليات الرفع السحابي:", fontSize = 13.sp)
                                            Text("${settings.backupCount} مرات", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("بروتوكول التشفير الفعال:", fontSize = 13.sp)
                                            Text("AES-256-GCM (مشفّر بالكامل)", fontWeight = FontWeight.Bold, color = Color(0xFF10B981), fontSize = 12.sp)
                                        }
                                    }
                                }
                            }

                            item {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                    Button(
                                        onClick = { viewModel.backupToCloud() },
                                        modifier = Modifier.fillMaxWidth().height(46.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = !backupLoading
                                    ) {
                                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("أخذ نسخة احتياطية مشفرة الآن", fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            viewModel.restoreFromCloud(settings.accountEmail) { success, msg ->
                                                Toast.makeText(context, if (success) "تمت استعادة البيانات بنجاح!" else "فشلت الاستعادة: $msg", Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth().height(46.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        enabled = !backupLoading
                                    ) {
                                        Icon(Icons.Default.CloudDownload, contentDescription = null)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("استيراد واستعادة محادثاتي", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (backupStatusMsg != null) {
                                item {
                                    Text(
                                        text = backupStatusMsg!!,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                    )
                                }
                            }

                            item {
                                HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("إرسال تقرير عطل أو ملاحظة فنية 🛠️", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = feedbackType == "FEEDBACK",
                                        onClick = { feedbackType = "FEEDBACK" },
                                        label = { Text("اقتراح أو ملاحظة") }
                                    )
                                    FilterChip(
                                        selected = feedbackType == "CRASH",
                                        onClick = { feedbackType = "CRASH" },
                                        label = { Text("تقرير عطل فني") }
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = feedbackContent,
                                    onValueChange = { feedbackContent = it },
                                    label = { Text(if (feedbackType == "FEEDBACK") "اكتب اقتراحك بالتفصيل هنا..." else "يرجى كتابة تفاصيل العطل والخطوات المسببة له...") },
                                    modifier = Modifier.fillMaxWidth().height(100.dp),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                    Button(
                                        onClick = {
                                            if (feedbackContent.isNotBlank()) {
                                                viewModel.submitAppFeedback(feedbackType, feedbackContent)
                                                feedbackContent = ""
                                                Toast.makeText(context, "تم إرسال تقريرك بنجاح. شكراً لك لمساعدتنا في تحسين تواصل بلس!", Toast.LENGTH_LONG).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("إرسال التقرير")
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // Support Tickets System View
                        if (selectedTicketForChat != null) {
                            // Render active support chat inside ticket
                            val activeTicket = tickets.find { it.id == selectedTicketForChat!!.id } ?: selectedTicketForChat!!
                            var chatReplyText by remember { mutableStateOf("") }

                            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(onClick = { selectedTicketForChat = null }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "عودة")
                                    }
                                    Column {
                                        Text(activeTicket.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("تذكرة رقم: ${activeTicket.id} • القسم: ${activeTicket.category}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when (activeTicket.status) {
                                                    "OPEN" -> Color(0xFF3B82F6).copy(alpha = 0.15f)
                                                    "IN_PROGRESS" -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                                    else -> Color(0xFF10B981).copy(alpha = 0.15f)
                                                }
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = when (activeTicket.status) {
                                                "OPEN" -> "مفتوحة"
                                                "IN_PROGRESS" -> "قيد المعالجة"
                                                else -> "محلولة"
                                            },
                                            color = when (activeTicket.status) {
                                                "OPEN" -> Color(0xFF3B82F6)
                                                "IN_PROGRESS" -> Color(0xFFF59E0B)
                                                else -> Color(0xFF10B981)
                                            },
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                LazyColumn(
                                    modifier = Modifier.weight(1f).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)).padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(activeTicket.messages) { msg ->
                                        val isUser = msg.sender == "USER"
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(
                                                        RoundedCornerShape(
                                                            topStart = 12.dp,
                                                            topEnd = 12.dp,
                                                            bottomStart = if (isUser) 12.dp else 0.dp,
                                                            bottomEnd = if (isUser) 0.dp else 12.dp
                                                        )
                                                    )
                                                    .background(if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer)
                                                    .padding(12.dp)
                                                    .widthIn(max = 280.dp)
                                            ) {
                                                Text(
                                                    text = msg.text,
                                                    color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                                    fontSize = 13.sp
                                                )
                                            }
                                            Text(
                                                text = if (isUser) "أنا • " + formatCloudTime(msg.timestamp) else "الدعم الفني • " + formatCloudTime(msg.timestamp),
                                                fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                if (activeTicket.status != "RESOLVED") {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = chatReplyText,
                                            onValueChange = { chatReplyText = it },
                                            placeholder = { Text("اكتب ردك للدعم الفني هنا...", fontSize = 12.sp) },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(24.dp)
                                        )
                                        FloatingActionButton(
                                            onClick = {
                                                if (chatReplyText.isNotBlank()) {
                                                    viewModel.replyToCloudTicket(activeTicket.id, "USER", chatReplyText)
                                                    chatReplyText = ""
                                                }
                                            },
                                            shape = CircleShape,
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال", tint = Color.White)
                                        }
                                    }
                                } else {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.1f))
                                    ) {
                                        Text(
                                            "تم إغلاق هذه التذكرة لحل المشكلة بنجاح. إذا كانت لديك مشكلة أخرى يرجى فتح تذكرة جديدة.",
                                            fontSize = 11.sp,
                                            color = Color(0xFF10B981),
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(12.dp),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            // Normal tickets list and ticket opening form
                            var isOpeningNewTicket by remember { mutableStateOf(false) }

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("نظام الدعم الفني وتذاكر المساعدة 💬🎟️", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                                        TextButton(onClick = { isOpeningNewTicket = !isOpeningNewTicket }) {
                                            Text(if (isOpeningNewTicket) "عرض تذاكري" else "+ تذكرة جديدة", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                if (isOpeningNewTicket) {
                                    item {
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        ) {
                                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Text("إنشاء تذكرة دعم جديدة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                
                                                OutlinedTextField(
                                                    value = ticketTitle,
                                                    onValueChange = { ticketTitle = it },
                                                    label = { Text("عنوان المشكلة") },
                                                    placeholder = { Text("مثال: عطل في تشفير جهة اتصال محددة") },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    singleLine = true
                                                )

                                                Box(modifier = Modifier.fillMaxWidth()) {
                                                    OutlinedCard(
                                                        onClick = { expandedCategory = true },
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                            Text("قسم المشكلة: " + when(ticketCategory) {
                                                                "ACCOUNT" -> "حسابات المستخدمين"
                                                                "BACKUP" -> "النسخ الاحتياطي"
                                                                "BLUETOOTH" -> "اتصال البلوتوث"
                                                                "FEATURE_REQUEST" -> "اقتراح ميزة جديدة"
                                                                else -> "أخرى / عام"
                                                            }, fontWeight = FontWeight.Medium)
                                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                                        }
                                                    }
                                                    DropdownMenu(expanded = expandedCategory, onDismissRequest = { expandedCategory = false }) {
                                                        val cats = listOf("ACCOUNT" to "حسابات المستخدمين", "BACKUP" to "النسخ الاحتياطي", "BLUETOOTH" to "اتصال البلوتوث", "FEATURE_REQUEST" to "اقتراح ميزة جديدة", "GENERAL" to "أخرى / عام")
                                                        cats.forEach { (key, valStr) ->
                                                            DropdownMenuItem(
                                                                text = { Text(valStr) },
                                                                onClick = {
                                                                    ticketCategory = key
                                                                    expandedCategory = false
                                                                }
                                                            )
                                                        }
                                                    }
                                                }

                                                OutlinedTextField(
                                                    value = ticketDesc,
                                                    onValueChange = { ticketDesc = it },
                                                    label = { Text("شرح تفصيلي للمشكلة") },
                                                    placeholder = { Text("يرجى كتابة التفاصيل والخطوات لتسهيل تتبعها من مهندسي الدعم...") },
                                                    modifier = Modifier.fillMaxWidth().height(120.dp)
                                                )

                                                Button(
                                                    onClick = {
                                                        if (ticketTitle.isNotBlank() && ticketDesc.isNotBlank()) {
                                                            viewModel.createCloudTicket(ticketTitle, ticketDesc, ticketCategory) { success ->
                                                                if (success) {
                                                                    ticketTitle = ""
                                                                    ticketDesc = ""
                                                                    ticketCategory = "GENERAL"
                                                                    isOpeningNewTicket = false
                                                                    Toast.makeText(context, "تم رفع التذكرة، سيقوم مهندسو الدعم بالرد عليك قريباً!", Toast.LENGTH_LONG).show()
                                                                }
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text("إرسال التذكرة فوراً", fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    if (tickets.isEmpty()) {
                                        item {
                                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                                Text("لا توجد لديك تذاكر مسجلة مسبقاً", color = MaterialTheme.colorScheme.outline)
                                            }
                                        }
                                    } else {
                                        items(tickets) { ticket ->
                                            ElevatedCard(
                                                onClick = { selectedTicketForChat = ticket },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                        Text("تذكرة: #${ticket.id}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                                        
                                                        Box(
                                                            modifier = Modifier
                                                                .clip(RoundedCornerShape(8.dp))
                                                                .background(
                                                                    when (ticket.status) {
                                                                        "OPEN" -> Color(0xFF3B82F6).copy(alpha = 0.15f)
                                                                        "IN_PROGRESS" -> Color(0xFFF59E0B).copy(alpha = 0.15f)
                                                                        else -> Color(0xFF10B981).copy(alpha = 0.15f)
                                                                    }
                                                                )
                                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                                        ) {
                                                            Text(
                                                                text = when (ticket.status) {
                                                                    "OPEN" -> "مفتوحة"
                                                                    "IN_PROGRESS" -> "قيد المعالجة"
                                                                    else -> "محلولة"
                                                                },
                                                                color = when (ticket.status) {
                                                                    "OPEN" -> Color(0xFF3B82F6)
                                                                    "IN_PROGRESS" -> Color(0xFFF59E0B)
                                                                    else -> Color(0xFF10B981)
                                                                },
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                    
                                                    Text(ticket.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                    Text(ticket.description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                                    
                                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                        Text("القسم: ${ticket.category}", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                                        Text(formatCloudTime(ticket.createdAt), fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // Updates & Notifications View
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                Text("إصدارات التطبيق والإشعارات العامة 🚀📢", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.primary)
                                Text("تلقي الإعلانات وتحديثات الأمان الهامة لضمان سلامة تطبيقك والتحقق من التحديثات.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            // Update checker block
                            item {
                                val latestVer = versions.maxByOrNull { it.versionCode }
                                val currentVerCode = 1 // Mock current version code
                                
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text("التحقق من التحديثات", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("إصدارك الحالي للتطبيق:")
                                            Text("1.0.0 (الإصدار 1)", fontWeight = FontWeight.Bold)
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("آخر إصدار سحابي متوفر:")
                                            Text(if (latestVer != null) "${latestVer.versionName} (الإصدار ${latestVer.versionCode})" else "1.0.0", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }

                                        if (latestVer != null && latestVer.versionCode > currentVerCode) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (latestVer.isCritical) Color(0xFFEF4444).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f))
                                                    .padding(10.dp)
                                            ) {
                                                Column {
                                                    Text(
                                                        text = if (latestVer.isCritical) "⚠️ تحديث حرج وأمني متوفر للتحميل فوراً!" else "📦 تحديث جديد متوفر للتحميل!",
                                                        color = if (latestVer.isCritical) Color(0xFFEF4444) else Color(0xFFF59E0B),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(latestVer.changelog, fontSize = 11.sp)
                                                }
                                            }
                                            Button(
                                                onClick = {
                                                    Toast.makeText(context, "جاري محاكاة تنزيل الحزمة وتحديث تواصل بلس...", Toast.LENGTH_LONG).show()
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = ButtonDefaults.buttonColors(containerColor = if (latestVer.isCritical) Color(0xFFEF4444) else MaterialTheme.colorScheme.primary)
                                            ) {
                                                Icon(Icons.Default.DownloadForOffline, contentDescription = null)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("تحميل وتثبيت التحديث الآن")
                                            }
                                        } else {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                                                Text("أنت تستخدم آخر إصدار من تواصل بلس ومحمي بالكامل.", fontSize = 12.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            // Public Notifications
                            item {
                                Text("التنبيهات والإعلانات العامة", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }

                            if (notifications.isEmpty()) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                        Text("لا توجد إعلانات نشطة حالياً", color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                            } else {
                                items(notifications) { ntf ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Icon(Icons.Default.Campaign, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                                    Text(ntf.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                }
                                                Text(formatCloudTime(ntf.timestamp), fontSize = 9.sp, color = MaterialTheme.colorScheme.outline)
                                            }
                                            Text(ntf.message, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

// =========================================================================
// 2. OFFICIAL WEBSITE COMPANION SCREEN (الموقع الإلكتروني الرسمي للتطبيق)
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficialWebsiteSection(viewModel: MainViewModel) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val versions by viewModel.cloudVersions.collectAsStateWithLifecycle()
    val backupLoading by viewModel.backupLoading.collectAsStateWithLifecycle()
    
    var webTab by remember { mutableStateOf("HOME") } // HOME, BACKUPS, HELP, PRIVACY, UPDATES, CONTACT
    var isDesktopView by remember { mutableStateOf(true) } // Web switch for Desktop or Mobile view simulator!

    // Simulated Web Login / Session
    var webEmail by remember { mutableStateOf(settings.accountEmail) }
    var webPassword by remember { mutableStateOf("") }
    var webIsLoggedIn by remember { mutableStateOf(settings.isLoggedIn) }

    // Contact Form State
    var contactName by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var contactMsg by remember { mutableStateOf("") }

    LaunchedEffect(settings.accountEmail, settings.isLoggedIn) {
        webEmail = settings.accountEmail
        webIsLoggedIn = settings.isLoggedIn
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("الموقع الرسمي لـ تواصل بلس 🌐", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.SETTINGS) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع للتطبيق")
                    }
                },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(end = 8.dp)) {
                        Text(if (isDesktopView) "عرض شاشة كمبيوتر" else "عرض هاتف", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Switch(
                            checked = isDesktopView,
                            onCheckedChange = { isDesktopView = it },
                            thumbContent = {
                                Icon(
                                    imageVector = if (isDesktopView) Icons.Default.DesktopWindows else Icons.Default.Smartphone,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A),
                    titleContentColor = Color.White
                )
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF0F172A)) // Slate Dark Theme for website
        ) {
            // Simulated Web Browser Header & Navigation Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column {
                    // Browser Top Address Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFEF4444)))
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFF59E0B)))
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF10B981)))
                        }
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF0F172A))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(11.dp))
                                Text("https://tawasulplus.com/" + webTab.lowercase(), color = Color(0xFF94A3B8), fontSize = 10.sp, maxLines = 1)
                            }
                        }
                    }

                    // Website Navbar Brand & Tabs
                    if (isDesktopView) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Brand Logo
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(24.dp))
                                Text("تواصل بلس", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }

                            // Navbar Links
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                val links = listOf("HOME" to "الرئيسية", "BACKUPS" to "النسخ السحابي", "HELP" to "مركز المساعدة", "UPDATES" to "التحديثات", "PRIVACY" to "الخصوصية", "CONTACT" to "الدعم والتواصل")
                                links.forEach { (key, title) ->
                                    TextButton(
                                        onClick = { webTab = key },
                                        colors = ButtonDefaults.textButtonColors(
                                            contentColor = if (webTab == key) Color(0xFF6366F1) else Color(0xFF94A3B8)
                                        )
                                    ) {
                                        Text(title, fontSize = 11.sp, fontWeight = if (webTab == key) FontWeight.Bold else FontWeight.Medium)
                                    }
                                }
                            }
                        }
                    } else {
                        // Mobile Navigation Bar - Single horizontally scrollable navigation
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val links = listOf("HOME" to "الرئيسية", "BACKUPS" to "النسخ السحابي", "HELP" to "المساعدة", "UPDATES" to "التحديثات", "PRIVACY" to "الخصوصية", "CONTACT" to "الدعم")
                            links.forEach { (key, title) ->
                                FilterChip(
                                    selected = webTab == key,
                                    onClick = { webTab = key },
                                    label = { Text(title, fontSize = 10.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFF6366F1),
                                        selectedLabelColor = Color.White,
                                        containerColor = Color(0xFF0F172A),
                                        labelColor = Color(0xFF94A3B8)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Web Page Canvas Body
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    .verticalScroll(rememberScrollState())
            ) {
                when (webTab) {
                    "HOME" -> {
                        // Landing Web Page Hero
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(24.dp)
                        ) {
                            Text(
                                "تواصل بلس | المراسلة اللامركزية المشفرة 🛡️",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "التطبيق الأول للتواصل المباشر والآمن بين الأجهزة بالكامل عبر البلوتوث والشبكة المحلية دون الحاجة لوجود خوادم للمحادثات أو اتصال بالإنترنت!",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.widthIn(max = 600.dp)
                            )

                            // Quick Download Banner
                            Card(
                                modifier = Modifier.fillMaxWidth().widthIn(max = 600.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Icon(Icons.Default.DownloadForOffline, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(48.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("تنزيل تطبيق تواصل بلس الرسمي", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("متوفر حالياً لأجهزة الأندرويد بميزات المحادثات الصوتية والملفات غير المحدودة.", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    }
                                    Button(
                                        onClick = {
                                            Toast.makeText(context, "جاري تحميل ملف APK من خادم الموقع السحابي...", Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                                    ) {
                                        Text("تنزيل APK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            // Key Web Features Columns
                            Text("لماذا تختار تواصل بلس؟ ✨", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            
                            val features = listOf(
                                Triple(Icons.Default.Bluetooth, "اتصال Peer-to-Peer مستقل", "محادثات ومكالمات مباشرة عبر البلوتوث بدون خوادم تتبع أو مراقبة."),
                                Triple(Icons.Default.Security, "تشفير فائق القوة محلي", "توليد مفاتيح تشفير RSA-2048 و AES-256 فريدة لكل جهاز لضمان الخصوصية القصوى."),
                                Triple(Icons.Default.CloudQueue, "نسخ احتياطي سحابي اختياري", "إمكانية تشفير ونسخ جهات الاتصال الخاصة بك واستعادتها بأمان على أي جهاز جديد.")
                            )

                            if (isDesktopView) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    features.forEach { (icon, title, desc) ->
                                        Card(
                                            modifier = Modifier.weight(1f).height(150.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Icon(icon, contentDescription = null, tint = Color(0xFF6366F1))
                                                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(desc, color = Color(0xFF94A3B8), fontSize = 11.sp, lineHeight = 16.sp)
                                            }
                                        }
                                    }
                                }
                            } else {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    features.forEach { (icon, title, desc) ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                                        ) {
                                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Icon(icon, contentDescription = null, tint = Color(0xFF6366F1))
                                                Column {
                                                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    Text(desc, color = Color(0xFF94A3B8), fontSize = 11.sp, lineHeight = 16.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "BACKUPS" -> {
                        // Web Backup Panel Portal Mockup
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("بوابة إدارة البيانات والنسخ السحابية الويب ☁️💻", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("قم بالولوج بأمان لإدارة النسخ الاحتياطية الخاصة بحسابك، والتحقق من أحجام الملفات، أو تنزيلها وفحص جلسات أجهزتك النشطة مباشرة.", color = Color(0xFF94A3B8), fontSize = 11.sp, textAlign = TextAlign.Center)

                            if (!webIsLoggedIn) {
                                Card(
                                    modifier = Modifier.fillMaxWidth().widthIn(max = 400.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                                ) {
                                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text("تسجيل الدخول لبوابة الويب", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        OutlinedTextField(
                                            value = webEmail,
                                            onValueChange = { webEmail = it },
                                            label = { Text("البريد الإلكتروني") },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                        )
                                        OutlinedTextField(
                                            value = webPassword,
                                            onValueChange = { webPassword = it },
                                            label = { Text("كلمة المرور") },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                        )
                                        Button(
                                            onClick = {
                                                if (webEmail.isNotBlank()) {
                                                    webIsLoggedIn = true
                                                    Toast.makeText(context, "تم تسجيل دخولك بنجاح لموقع تواصل الويب!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                                        ) {
                                            Text("تسجيل دخول ويب آمن", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                Card(
                                    modifier = Modifier.fillMaxWidth().widthIn(max = 500.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                                ) {
                                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("مرحباً بك: " + settings.displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            TextButton(onClick = { webIsLoggedIn = false }) {
                                                Text("خروج", color = Color(0xFFEF4444))
                                            }
                                        }

                                        HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF94A3B8).copy(alpha = 0.2f))

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("بريدك السحابي الموثق:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                            Text(webEmail, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("حجم ملف النسخ السحابي:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                            Text("38.4 KB (مشفّر بالكامل)", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("آخر حفظ سحابي قادم من التطبيق:", color = Color(0xFF94A3B8), fontSize = 12.sp)
                                            Text(if (settings.lastBackupTime > 0) formatCloudTime(settings.lastBackupTime) else "لا يوجد", color = Color.White, fontSize = 12.sp)
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    Toast.makeText(context, "جاري ترحيل ملف النسخ من خادم الويب وحفظه محلياً في مجلد التنزيلات...", Toast.LENGTH_LONG).show()
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                            ) {
                                                Text("تنزيل ملف النسخ (.json)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }

                                            Button(
                                                onClick = {
                                                    Toast.makeText(context, "تم حذف نسختك السحابية بنجاح من الخوادم. (بياناتك المحلية بالتطبيق آمنة بالكامل)", Toast.LENGTH_LONG).show()
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                                            ) {
                                                Text("حذف ملف النسخة السحابية", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "HELP" -> {
                        // FAQ Center
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("مركز مساعدة تواصل بلس والأسئلة الشائعة ❓📑", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            
                            val faqs = listOf(
                                "كيف يعمل التطبيق بدون إنترنت؟" to "يعتمد تطبيق تواصل بلس على تقنيات البلوتوث (Bluetooth) والـ Wi-Fi Direct لربط الأجهزة القريبة بشكل مباشر بدون الاعتماد على شبكة إنترنت خارجية أو أبراج اتصالات محيطة.",
                                "هل رسائلي ومكالماتي مشفرة؟" to "نعم، يتم تشفير كافة المحادثات النصية، الصوتية، والملفات بالكامل من الطرف للطرف (End-to-End Encryption) باستخدام خوارزميات التشفير العسكري RSA-2048 لتبادل المفاتيح و AES-256 لتشفير البيانات.",
                                "ما دور الخادم السحابي للتطبيق؟" to "الخادم السحابي لتطبيق تواصل بلس يقتصر دوره على الأمور الثانوية والخدمات التمكينية فقط، مثل: التحقق من الحسابات والبريد الإلكتروني، إرسال إشعارات التحديثات، استضافة النسخ الاحتياطية الاختيارية (المشفرة محلياً)، واستقبال تذاكر الدعم الفني.",
                                "هل يمكن للخادم قراءة ملفات النسخ الاحتياطي؟" to "مطلقاً! يتم فك تشفير النسخة الاحتياطية فقط داخل جهازك باستخدام كلمة المرور السرية الخاصة بك والتي لا يتم إرسالها للخادم أبداً (تشفير صفر معرفة Zero-Knowledge)."
                            )

                            faqs.forEach { (q, a) ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text("❓ " + q, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(a, color = Color(0xFF94A3B8), fontSize = 12.sp, lineHeight = 18.sp)
                                    }
                                }
                            }
                        }
                    }

                    "UPDATES" -> {
                        // Changelog timeline
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("آخر تحديثات وسجل إصدارات تطبيق تواصل بلس 🚀", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)

                            versions.reversed().forEach { ver ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                    border = BorderStroke(0.5.dp, Color(0xFF334155))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text("تحديث رقم: " + ver.versionName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                                if (ver.isCritical) {
                                                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Color(0xFFEF4444)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                                        Text("أمني حرج", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                            Text(ver.releaseDate, color = Color(0xFF94A3B8), fontSize = 11.sp)
                                        }
                                        Text(ver.changelog, color = Color(0xFF94A3B8), fontSize = 11.sp, lineHeight = 16.sp)
                                        
                                        Button(
                                            onClick = {
                                                Toast.makeText(context, "جاري تحميل APK للإصدار ${ver.versionName}...", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.align(Alignment.End),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                                        ) {
                                            Text("تحميل APK للإصدار", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    "PRIVACY" -> {
                        // Privacy Policy text
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("سياسة الخصوصية وشروط الاستخدام لتطبيق تواصل بلس 🛡️📜", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            
                            val policies = listOf(
                                "1. أمن المراسلات والبيانات الفورية" to "التطبيق لا يقوم بتمرير رسائلك، أو مكالماتك، أو صورك، أو ملفاتك المشتركة عبر أي خوادم سحابية على الإطلاق. الاتصال يتم بشكل مباشر من جهاز إلى جهاز (Direct Peer-to-Peer) باستخدام البلوتوث والواي فاي، ويكون التشفير تاماً (End-to-End Encryption) بحيث يستحيل اعتراضه أو قراءته.",
                                "2. خدمات السحابة والنسخ الاحتياطي الاختيارية" to "عند قيامك بإنشاء حساب، فإن بريدك الإلكتروني والاسم التعريفي يُسجلان على السحابة لغايات تمكين الدعم والتحقق من الجلسات فقط. كما يمكنك تفعيل ميزة 'النسخ السحابي' الاختيارية، وتخزن النسخة مشفرة تماماً بمفتاح محلي مشتق من كلمة مرورك الشخصية قبل إرسالها ولا يملك الخادم مفتاح فك التشفير هذا.",
                                "3. حماية الجلسات والأجهزة" to "يحق للمستخدم تتبع كافة الأجهزة التي سجلت الدخول بحسابه عبر موقع الويب أو بوابة الدعم الفني، وفصل أي جهاز مشبوه في أي وقت. كما لا يتشارك التطبيق أي معلومات فنية أو جغرافية مع أي جهات خارجية."
                            )

                            policies.forEach { (title, body) ->
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Text(body, color = Color(0xFF94A3B8), fontSize = 12.sp, lineHeight = 18.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                            }
                        }
                    }

                    "CONTACT" -> {
                        // Web support ticketing / email simulated interface
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("التواصل والدعم الفني السحابي 📬💬", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text("يرجى ملء النموذج أدناه لإرسال رسالة مباشرة لمهندسي الدعم الفني لتواصل بلس. سنرد عليك عبر البريد الإلكتروني في غضون 24 ساعة.", color = Color(0xFF94A3B8), fontSize = 11.sp, textAlign = TextAlign.Center)

                            Card(
                                modifier = Modifier.fillMaxWidth().widthIn(max = 500.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                            ) {
                                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("نموذج الاتصال الفني بمطوري تواصل بلس", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    
                                    OutlinedTextField(
                                        value = contactName,
                                        onValueChange = { contactName = it },
                                        label = { Text("الاسم الكامل") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )

                                    OutlinedTextField(
                                        value = contactEmail,
                                        onValueChange = { contactEmail = it },
                                        label = { Text("البريد الإلكتروني للرد") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )

                                    OutlinedTextField(
                                        value = contactMsg,
                                        onValueChange = { contactMsg = it },
                                        label = { Text("تفاصيل المشكلة أو الرسالة") },
                                        modifier = Modifier.fillMaxWidth().height(100.dp),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )

                                    Button(
                                        onClick = {
                                            if (contactName.isNotBlank() && contactEmail.isNotBlank() && contactMsg.isNotBlank()) {
                                                Toast.makeText(context, "شكراً لك! تم تسليم رسالتك لمهندسي تواصل بلس ومسؤولي الخادم بنجاح.", Toast.LENGTH_LONG).show()
                                                contactName = ""
                                                contactEmail = ""
                                                contactMsg = ""
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                                    ) {
                                        Text("إرسال رسالة الدعم", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Web Browser Footer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "© 2026 تواصل بلس. جميع الحقوق محفوظة لشبكة تواصل اللامركزية والأمن السحابي.",
                    color = Color(0xFF64748B),
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// =========================================================================
// 3. ADMIN DASHBOARD CONTROL PANEL (لوحة تحكم المشرف للمشروع)
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardSection(viewModel: MainViewModel) {
    val context = LocalContext.current
    val users by viewModel.cloudUsers.collectAsStateWithLifecycle()
    val tickets by viewModel.cloudTickets.collectAsStateWithLifecycle()
    val versions by viewModel.cloudVersions.collectAsStateWithLifecycle()
    val feedback by viewModel.cloudFeedback.collectAsStateWithLifecycle()
    val notifications by viewModel.cloudNotifications.collectAsStateWithLifecycle()

    var adminTab by remember { mutableStateOf(0) } // 0: Stats, 1: Users, 2: Tickets, 3: Updates, 4: Feedback
    
    // Admin Reply Ticket Dialog state
    var selectedTicketForReply by remember { mutableStateOf<SupportTicket?>(null) }
    var adminReplyText by remember { mutableStateOf("") }
    var newTicketStatusSelected by remember { mutableStateOf<String?>(null) }

    // Admin Broadcaster state
    var broadTitle by remember { mutableStateOf("") }
    var broadMsg by remember { mutableStateOf("") }

    // Admin Publisher state
    var pubVerName by remember { mutableStateOf("") }
    var pubChangelog by remember { mutableStateOf("") }
    var pubIsCritical by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshCloudData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("لوحة تحكم مدير خوادم تواصل بلس ⚙️🛡️", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.SETTINGS) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1E293B),
                    titleContentColor = Color.White
                )
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF0F172A)) // Sleek administrative dark Slate theme
        ) {
            // Sidebar Menu
            NavigationRail(
                containerColor = Color(0xFF1E293B),
                modifier = Modifier.width(80.dp)
            ) {
                NavigationRailItem(
                    selected = adminTab == 0,
                    onClick = { adminTab = 0 },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null, tint = if(adminTab==0) Color(0xFF6366F1) else Color.White) },
                    label = { Text("الرئيسية", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                )
                NavigationRailItem(
                    selected = adminTab == 1,
                    onClick = { adminTab = 1 },
                    icon = { Icon(Icons.Default.People, contentDescription = null, tint = if(adminTab==1) Color(0xFF6366F1) else Color.White) },
                    label = { Text("المستخدمين", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                )
                NavigationRailItem(
                    selected = adminTab == 2,
                    onClick = { adminTab = 2 },
                    icon = { Icon(Icons.Default.SupportAgent, contentDescription = null, tint = if(adminTab==2) Color(0xFF6366F1) else Color.White) },
                    label = { Text("تذاكر الدعم", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                )
                NavigationRailItem(
                    selected = adminTab == 3,
                    onClick = { adminTab = 3 },
                    icon = { Icon(Icons.Default.Publish, contentDescription = null, tint = if(adminTab==3) Color(0xFF6366F1) else Color.White) },
                    label = { Text("التحديثات", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                )
                NavigationRailItem(
                    selected = adminTab == 4,
                    onClick = { adminTab = 4 },
                    icon = { Icon(Icons.Default.Feedback, contentDescription = null, tint = if(adminTab==4) Color(0xFF6366F1) else Color.White) },
                    label = { Text("الشكاوى", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold) }
                )
            }

            // Sub-window body panel
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                when (adminTab) {
                    0 -> {
                        // Overview & Stats Analytics Cards
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                Text("مؤشرات الخادم السحابي وإحصاءات الأمان 📊🛡️", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                Text("مراقبة نشاط الحسابات، الدعم الفني، وخدمة النسخ السحابي للتطبيق بالكامل.", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            }

                            // Analytics Grid Layout
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    AdminStatCard(modifier = Modifier.weight(1f), title = "إجمالي الحسابات", count = users.size.toString(), icon = Icons.Default.People, color = Color(0xFF6366F1))
                                    AdminStatCard(modifier = Modifier.weight(1f), title = "تذاكر الدعم", count = tickets.size.toString(), icon = Icons.Default.ContactSupport, color = Color(0xFFF59E0B))
                                }
                            }
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    AdminStatCard(modifier = Modifier.weight(1f), title = "إصدارات منشورة", count = versions.size.toString(), icon = Icons.Default.SystemUpdate, color = Color(0xFF10B981))
                                    AdminStatCard(modifier = Modifier.weight(1f), title = "تقارير وأعطال", count = feedback.size.toString(), icon = Icons.Default.BugReport, color = Color(0xFFEF4444))
                                }
                            }

                            // Custom Canvas graphical drawing for Monthly Active Users (MAU)
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text("رسم بياني: نمو وحركة مرور تواصل بلس السحابية 📈", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        
                                        Canvas(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(110.dp)
                                        ) {
                                            // Render some mock coordinates / data lines representing active traffic
                                            val w = size.width
                                            val h = size.height
                                            
                                            // Grid lines
                                            for (i in 1..4) {
                                                val y = h * (i / 4.0f)
                                                drawLine(Color(0xFF334155), start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(w, y), strokeWidth = 1f)
                                            }

                                            // Draw curves
                                            val points = listOf(
                                                androidx.compose.ui.geometry.Offset(w * 0.05f, h * 0.85f),
                                                androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.70f),
                                                androidx.compose.ui.geometry.Offset(w * 0.4f, h * 0.65f),
                                                androidx.compose.ui.geometry.Offset(w * 0.6f, h * 0.40f),
                                                androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.45f),
                                                androidx.compose.ui.geometry.Offset(w * 0.95f, h * 0.15f)
                                            )

                                            for (idx in 0 until points.size - 1) {
                                                drawLine(
                                                    color = Color(0xFF6366F1),
                                                    start = points[idx],
                                                    end = points[idx + 1],
                                                    strokeWidth = 4f
                                                )
                                                drawCircle(
                                                    color = Color.White,
                                                    radius = 4f,
                                                    center = points[idx]
                                                )
                                            }
                                            drawCircle(
                                                color = Color(0xFF10B981),
                                                radius = 6f,
                                                center = points.last()
                                            )
                                        }

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("يناير", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                            Text("مارس", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                            Text("مايو", color = Color(0xFF94A3B8), fontSize = 10.sp)
                                            Text("يوليو (الحالي)", color = Color(0xFF10B981), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // User Account Manager View
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                Text("إدارة حسابات مستخدمي السحابة 👥🔐", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                Text("قائمة الحسابات المسجلة. يمكنك إيقاف، تعليق، أو تنشيط الجلسات والحسابات المخالفة للسياسة.", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            }

                            if (users.isEmpty()) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                        Text("لا يوجد مستخدمون مسجلون", color = Color(0xFF94A3B8))
                                    }
                                }
                            } else {
                                items(users) { usr ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                        border = BorderStroke(0.5.dp, Color(0xFF334155))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF6366F1).copy(alpha = 0.2f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(usr.name.take(1).uppercase(), color = Color(0xFF6366F1), fontWeight = FontWeight.Bold)
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(usr.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text(usr.email, color = Color(0xFF94A3B8), fontSize = 11.sp)
                                                Text("تاريخ التسجيل: " + formatCloudTime(usr.createdTime), color = Color(0xFF64748B), fontSize = 9.sp)
                                            }
                                            
                                            // Block Toggle
                                            val isBlocked = usr.status == "BLOCKED"
                                            Button(
                                                onClick = {
                                                    viewModel.toggleCloudUserBlock(usr.email, usr.status)
                                                    Toast.makeText(context, if (isBlocked) "تم إعادة تنشيط الحساب بنجاح!" else "تم تعليق الحساب وتجميد الجلسات السحابية!", Toast.LENGTH_SHORT).show()
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isBlocked) Color(0xFF10B981) else Color(0xFFEF4444)
                                                ),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                                            ) {
                                                Text(if (isBlocked) "تنشيط" else "حظر", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // Help Desk Desk Tickets View
                        if (selectedTicketForReply != null) {
                            val activeTicket = tickets.find { it.id == selectedTicketForReply!!.id } ?: selectedTicketForReply!!
                            
                            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    IconButton(onClick = { selectedTicketForReply = null }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "عودة", tint = Color.White)
                                    }
                                    Column {
                                        Text("معالجة تذكرة: ${activeTicket.title}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("رقم التذكرة: ${activeTicket.id} • بريد المستخدم: ${activeTicket.email}", color = Color(0xFF94A3B8), fontSize = 11.sp)
                                    }
                                }

                                LazyColumn(
                                    modifier = Modifier.weight(1f).background(Color(0xFF1E293B)).padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(activeTicket.messages) { msg ->
                                        val isUser = msg.sender == "USER"
                                        Column(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalAlignment = if (isUser) Alignment.Start else Alignment.End
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(
                                                        RoundedCornerShape(
                                                            topStart = 12.dp,
                                                            topEnd = 12.dp,
                                                            bottomStart = if (isUser) 0.dp else 12.dp,
                                                            bottomEnd = if (isUser) 12.dp else 0.dp
                                                        )
                                                    )
                                                    .background(if (isUser) Color(0xFF334155) else Color(0xFF6366F1))
                                                    .padding(12.dp)
                                                    .widthIn(max = 280.dp)
                                            ) {
                                                Text(
                                                    text = msg.text,
                                                    color = Color.White,
                                                    fontSize = 13.sp
                                                )
                                            }
                                            Text(
                                                text = if (isUser) "المستخدم • " + formatCloudTime(msg.timestamp) else "أنا (المسؤول) • " + formatCloudTime(msg.timestamp),
                                                fontSize = 9.sp,
                                                color = Color(0xFF64748B),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                // Status switcher and reply bar
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = adminReplyText,
                                        onValueChange = { adminReplyText = it },
                                        placeholder = { Text("اكتب رد المسؤول للدعم الفني هنا...", fontSize = 11.sp, color = Color(0xFF94A3B8)) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(24.dp),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                    )
                                    
                                    // Dropdown status picker
                                    var showStatusPicker by remember { mutableStateOf(false) }
                                    Box {
                                        Button(
                                            onClick = { showStatusPicker = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text("الحالة", fontSize = 10.sp)
                                        }
                                        DropdownMenu(expanded = showStatusPicker, onDismissRequest = { showStatusPicker = false }) {
                                            DropdownMenuItem(text = { Text("محلولة (RESOLVED)") }, onClick = {
                                                newTicketStatusSelected = "RESOLVED"
                                                showStatusPicker = false
                                                Toast.makeText(context, "تم تحديد حالة المشكلة كمحلولة", Toast.LENGTH_SHORT).show()
                                            })
                                            DropdownMenuItem(text = { Text("قيد المتابعة (IN_PROGRESS)") }, onClick = {
                                                newTicketStatusSelected = "IN_PROGRESS"
                                                showStatusPicker = false
                                                Toast.makeText(context, "تم تحديد حالة قيد المعالجة", Toast.LENGTH_SHORT).show()
                                            })
                                        }
                                    }

                                    FloatingActionButton(
                                        onClick = {
                                            if (adminReplyText.isNotBlank()) {
                                                viewModel.replyToCloudTicket(activeTicket.id, "ADMIN", adminReplyText, newTicketStatusSelected)
                                                adminReplyText = ""
                                                newTicketStatusSelected = null
                                            }
                                        },
                                        shape = CircleShape,
                                        containerColor = Color(0xFF6366F1),
                                        modifier = Modifier.size(44.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال رد المسؤول", tint = Color.White)
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item {
                                    Text("تذاكر الدعم الفني الواردة للعملاء 🎧🎟️", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                    Text("يرجى الرد على تذاكر المستخدمين وتحديث حالتها فوراً لتقديم جودة خدمة ممتازة.", fontSize = 11.sp, color = Color(0xFF94A3B8))
                                }

                                if (tickets.isEmpty()) {
                                    item {
                                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                            Text("لا توجد تذاكر دعم فني نشطة حالياً", color = Color(0xFF94A3B8))
                                        }
                                    }
                                } else {
                                    items(tickets) { tck ->
                                        Card(
                                            onClick = { selectedTicketForReply = tck },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                            border = BorderStroke(0.5.dp, Color(0xFF334155))
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                    Text("من: " + tck.email, color = Color(0xFF6366F1), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(
                                                                when (tck.status) {
                                                                    "OPEN" -> Color(0xFF3B82F6).copy(alpha = 0.2f)
                                                                    "IN_PROGRESS" -> Color(0xFFF59E0B).copy(alpha = 0.2f)
                                                                    else -> Color(0xFF10B981).copy(alpha = 0.2f)
                                                                }
                                                            )
                                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                                    ) {
                                                        Text(
                                                            text = when (tck.status) {
                                                                "OPEN" -> "مفتوحة"
                                                                "IN_PROGRESS" -> "قيد العمل"
                                                                else -> "محلولة"
                                                            },
                                                            color = when (tck.status) {
                                                                "OPEN" -> Color(0xFF3B82F6)
                                                                "IN_PROGRESS" -> Color(0xFFF59E0B)
                                                                else -> Color(0xFF10B981)
                                                            },
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                                Text(tck.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("القسم: ${tck.category} • تاريخ الرفع: ${formatCloudTime(tck.createdAt)}", color = Color(0xFF64748B), fontSize = 10.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        // Publish updates & Push Broadcaster view
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                Text("إدارة الإصدارات وإرسال الإشعارات العامة 🚀📢", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                Text("نشر تحديثات APK جديدة وتعميم إشعارات عامة لتظهر فوراً لكافة مستخدمي التطبيق.", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            }

                            // Version Publisher Form
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                    border = BorderStroke(0.5.dp, Color(0xFF334155))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("نشر تحديث APK سحابي جديد", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        
                                        OutlinedTextField(
                                            value = pubVerName,
                                            onValueChange = { pubVerName = it },
                                            label = { Text("اسم الإصدار الفني الجديد") },
                                            placeholder = { Text("مثال: 1.2.0") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                        )

                                        OutlinedTextField(
                                            value = pubChangelog,
                                            onValueChange = { pubChangelog = it },
                                            label = { Text("قائمة التغييرات والإصلاحات") },
                                            placeholder = { Text("اكتب الميزات الجديدة والتحسينات للمستخدم...") },
                                            modifier = Modifier.fillMaxWidth().height(80.dp),
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                        )

                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                            Text("هل هذا التحديث أمني حرج؟", color = Color.White, fontSize = 12.sp)
                                            Switch(checked = pubIsCritical, onCheckedChange = { pubIsCritical = it })
                                        }

                                        Button(
                                            onClick = {
                                                if (pubVerName.isNotBlank() && pubChangelog.isNotBlank()) {
                                                    viewModel.publishNewAppVersion(pubVerName, pubChangelog, pubIsCritical)
                                                    pubVerName = ""
                                                    pubChangelog = ""
                                                    pubIsCritical = false
                                                    Toast.makeText(context, "تم نشر وتعميم التحديث الجديد بنجاح على الخادم!", Toast.LENGTH_LONG).show()
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                                        ) {
                                            Text("نشر وتعميم التحديث الآن 🚀", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // Notification Broadcaster Form
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                    border = BorderStroke(0.5.dp, Color(0xFF334155))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("بث إشعار عام فوري للجميع", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        
                                        OutlinedTextField(
                                            value = broadTitle,
                                            onValueChange = { broadTitle = it },
                                            label = { Text("عنوان التنبيه") },
                                            placeholder = { Text("مثال: صيانة مبرمجة في خوادم الدعم") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                        )

                                        OutlinedTextField(
                                            value = broadMsg,
                                            onValueChange = { broadMsg = it },
                                            label = { Text("نص الرسالة والتعميم") },
                                            modifier = Modifier.fillMaxWidth().height(80.dp),
                                            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                                        )

                                        Button(
                                            onClick = {
                                                if (broadTitle.isNotBlank() && broadMsg.isNotBlank()) {
                                                    viewModel.sendGlobalAdminNotification(broadTitle, broadMsg)
                                                    broadTitle = ""
                                                    broadMsg = ""
                                                    Toast.makeText(context, "تم بث الإشعار للجميع!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                                        ) {
                                            Text("بث الإشعار فوراً للجميع 📢", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    4 -> {
                        // Feedback & Bug Reports View
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                Text("سجل تقارير الأعطال والشكاوى الفنية 🛠️🐛", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                                Text("الشكاوى وملاحظات المستخدمين وتفاصيل العطل والمواصفات الفنية لأجهزتهم المرفوعة.", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            }

                            if (feedback.isEmpty()) {
                                item {
                                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                        Text("لا توجد تقارير شكاوى نشطة", color = Color(0xFF94A3B8))
                                    }
                                }
                            } else {
                                items(feedback) { rpt ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                                        border = BorderStroke(0.5.dp, Color(0xFF334155))
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    Icon(
                                                        imageVector = if (rpt.type == "CRASH") Icons.Default.BugReport else Icons.Default.Feedback,
                                                        contentDescription = null,
                                                        tint = if (rpt.type == "CRASH") Color(0xFFEF4444) else Color(0xFF10B981)
                                                    )
                                                    Text(
                                                        text = if (rpt.type == "CRASH") "تقرير عطل فني" else "اقتراح فني",
                                                        color = if (rpt.type == "CRASH") Color(0xFFEF4444) else Color(0xFF10B981),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 12.sp
                                                    )
                                                }
                                                Text(formatCloudTime(rpt.timestamp), color = Color(0xFF64748B), fontSize = 10.sp)
                                            }

                                            Text(rpt.content, color = Color.White, fontSize = 12.sp, lineHeight = 18.sp)
                                            
                                            HorizontalDivider(thickness = 0.5.dp, color = Color(0xFF334155))
                                            
                                            Column {
                                                Text("مرسل من: " + rpt.email, color = Color(0xFF94A3B8), fontSize = 10.sp)
                                                Text("مواصفات الجهاز: " + rpt.deviceInfo, color = Color(0xFF64748B), fontSize = 9.sp)
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
}

@Composable
fun AdminStatCard(
    modifier: Modifier = Modifier,
    title: String,
    count: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
            }
            Column {
                Text(title, color = Color(0xFF94A3B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(count, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
