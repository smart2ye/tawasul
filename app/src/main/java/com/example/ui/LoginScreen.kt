package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun LoginSection(viewModel: MainViewModel) {
    val context = LocalContext.current
    var isRegisterTab by remember { mutableStateOf(false) }
    
    var email by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val backupLoading by viewModel.backupLoading.collectAsStateWithLifecycle()
    val backupStatusMsg by viewModel.backupStatusMsg.collectAsStateWithLifecycle()

    var showRestorePrompt by remember { mutableStateOf(false) }
    var targetRestoreEmail by remember { mutableStateOf("") }

    val showGoogleSimulationDialog by viewModel.showGoogleSimulationDialog.collectAsStateWithLifecycle()
    var simEmail by remember { mutableStateOf("") }
    var simName by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .verticalScroll(scrollState),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 450.dp)
        ) {
            // App Logo Hero Graphic
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "شعار تواصل بلس",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(44.dp)
                )
            }

            Text(
                text = "تواصل بلس | Tawasul Plus",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            Text(
                text = "نظام المراسلة الآمن والمشفر المعتمد على الشبكة اللامركزية مع النسخ الاحتياطي السحابي التلقائي",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            // Tabs for Register and Login
            TabRow(
                selectedTabIndex = if (isRegisterTab) 1 else 0,
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Tab(
                    selected = !isRegisterTab,
                    onClick = { isRegisterTab = false },
                    text = { Text("تسجيل الدخول", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_login")
                )
                Tab(
                    selected = isRegisterTab,
                    onClick = { isRegisterTab = true },
                    text = { Text("إنشاء حساب جديد", fontWeight = FontWeight.Bold) },
                    modifier = Modifier.testTag("tab_register")
                )
            }

            // Input Fields Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Email input
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("البريد الإلكتروني") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_email"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    // Display Name (Only in Register tab)
                    AnimatedVisibility(visible = isRegisterTab) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("الاسم التعريفي الكامل") },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("input_name"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }

                    // Password input
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("كلمة المرور") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = "عرض كلمة المرور"
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_password"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    // Error Message Display
                    authError?.let { err ->
                        Text(
                            text = err,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 4.dp),
                            textAlign = TextAlign.Start
                        )
                    }

                    // Submit Button
                    Button(
                        onClick = {
                            if (isRegisterTab) {
                                viewModel.registerUser(email, name, password) { success ->
                                    if (success) {
                                        Toast.makeText(context, "تم التسجيل بنجاح!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                viewModel.loginUser(email, password) { success, code ->
                                    if (success) {
                                        Toast.makeText(context, "تم الدخول بنجاح!", Toast.LENGTH_SHORT).show()
                                    } else if (code == "RESTORE_AVAILABLE") {
                                        targetRestoreEmail = email
                                        showRestorePrompt = true
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_auth_submit"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !backupLoading
                    ) {
                        if (backupLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = if (isRegisterTab) "تسجيل حساب جديد" else "تسجيل الدخول آمن",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // OR Divider
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                        Text("أو", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HorizontalDivider(modifier = Modifier.weight(1f), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                    }

                    // Google Sign-In Button
                    OutlinedButton(
                        onClick = {
                            viewModel.signInWithGoogle(context) { success, err ->
                                if (success) {
                                    Toast.makeText(context, "تم تسجيل الدخول بواسطة Google بنجاح!", Toast.LENGTH_SHORT).show()
                                } else if (err != null) {
                                    Toast.makeText(context, "فشل تسجيل الدخول: $err", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_google_signin"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = "شعار Google",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "تسجيل الدخول باستخدام Google",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Explanatory Cloud Notice
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = "السحابة",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "التطبيق يدعم النسخ الاحتياطي السحابي الذكي لقاعدة البيانات المحلية. بمجرد تسجيل الدخول، يمكنك تفعيل استعادة بياناتك تلقائياً على أي جهاز آخر.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }

    // Backup Restore available dialog
    if (showRestorePrompt) {
        AlertDialog(
            onDismissRequest = { showRestorePrompt = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("تم العثور على نسخة احتياطية!", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "تم اكتشاف نسخة احتياطية سحابية سابقة مرتبطة بالبريد الإلكتروني ($targetRestoreEmail).",
                        fontSize = 14.sp
                    )
                    Text(
                        "هل تود استيراد واستعادة كافة جهات الاتصال الخاصة بك، وسجل المحادثات، والإعدادات السابقة من السحابة الآن؟",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (backupStatusMsg != null) {
                        Text(
                            text = backupStatusMsg!!,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.restoreFromCloud(targetRestoreEmail) { success, msg ->
                            if (success) {
                                Toast.makeText(context, "تمت استعادة البيانات بنجاح!", Toast.LENGTH_SHORT).show()
                                showRestorePrompt = false
                            } else {
                                Toast.makeText(context, "فشلت الاستعادة: $msg", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (backupLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("نعم، استعادة الآن")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        // Skip and start fresh (sign in locally)
                        viewModel.registerUser(targetRestoreEmail, targetRestoreEmail.substringBefore("@"), password) { success ->
                            if (success) {
                                Toast.makeText(context, "تم تسجيل الدخول كملف جديد", Toast.LENGTH_SHORT).show()
                            }
                            showRestorePrompt = false
                        }
                    }
                ) {
                    Text("تخطي والبدء من جديد", color = MaterialTheme.colorScheme.outline)
                }
            }
        )
    }

    // Google simulated sign-in dialog
    if (showGoogleSimulationDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setGoogleSimulationDialog(false) },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("تسجيل دخول تجريبي لـ Google 🧪", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "الوصول لخدمات Google Play مقيد لعدم تسجيل توقيع SHA-1 في الكونسول. لمحاكاة تسجيل دخول حقيقي لـ Google بالبريد الذي تريده (كالمسجل بالمحاكي)، يرجى كتابته:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                    
                    OutlinedTextField(
                        value = simEmail,
                        onValueChange = { simEmail = it },
                        label = { Text("بريد Google الإلكتروني") },
                        placeholder = { Text("example@gmail.com") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_sim_email")
                    )

                    OutlinedTextField(
                        value = simName,
                        onValueChange = { simName = it },
                        label = { Text("الاسم الكامل في Google") },
                        placeholder = { Text("اسم المستخدم") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("input_sim_name")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val finalEmail = if (simEmail.isNotBlank()) simEmail.trim() else "waelmahdly531@gmail.com"
                        val finalName = if (simName.isNotBlank()) simName.trim() else finalEmail.substringBefore("@")
                        viewModel.completeSimulatedGoogleSignIn(finalEmail, finalName) { success, _ ->
                            if (success) {
                                Toast.makeText(context, "تم تسجيل الدخول لـ $finalEmail بنجاح!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("تأكيد وتسجيل الدخول", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setGoogleSimulationDialog(false) }) {
                    Text("إلغاء", color = MaterialTheme.colorScheme.outline)
                }
            }
        )
    }
}
