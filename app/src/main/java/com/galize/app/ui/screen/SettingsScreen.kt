package com.galize.app.ui.screen

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.galize.app.service.ScreenCapturePermissionManager
import com.galize.app.ui.theme.*
import com.galize.app.ui.viewmodel.SettingsViewModel
import com.galize.app.utils.PermissionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val apiKey by viewModel.apiKey.collectAsState()
    val apiBaseUrl by viewModel.apiBaseUrl.collectAsState()
    val aiModel by viewModel.aiModel.collectAsState()
    val selectedPersona by viewModel.selectedPersona.collectAsState()
    val customSystemPrompt by viewModel.customSystemPrompt.collectAsState()
    val permissionState by viewModel.permissionState.collectAsState()
    val shouldRequestMediaProjection by viewModel.shouldRequestMediaProjection.collectAsState()

    val mediaProjectionPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        viewModel.onMediaProjectionPermissionResult(
            result.resultCode == Activity.RESULT_OK,
            result.data,
        )
    }

    LaunchedEffect(shouldRequestMediaProjection) {
        if (shouldRequestMediaProjection) {
            val pm = ScreenCapturePermissionManager(context)
            mediaProjectionPermissionLauncher.launch(pm.createScreenCaptureIntent())
        }
    }

    Scaffold(
        containerColor = NightSky,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        style = TextStyle(
                            brush = auroraHorizontalBrush(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = AuroraViolet)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ── Permission Section ──
            SectionHeader(title = "权限管理", color = AuroraCyan)
            GlassCardContainer {
                permissionState.permissions.forEach { permission ->
                    PermissionItem(
                        permission = permission,
                        onRequestPermission = { viewModel.requestPermission(it) },
                    )
                }
            }

            // ── AI Configuration ──
            SectionHeader(title = "AI Configuration", color = AuroraPurple)
            GlassCardContainer {
                DarkTextField(
                    value = apiBaseUrl,
                    onValueChange = { viewModel.updateApiBaseUrl(it) },
                    label = "API Base URL",
                    placeholder = "https://api.openai.com/v1",
                )
                Spacer(modifier = Modifier.height(12.dp))
                DarkTextField(
                    value = apiKey,
                    onValueChange = { viewModel.updateApiKey(it) },
                    label = "API Key",
                    placeholder = "sk-...",
                )
                Spacer(modifier = Modifier.height(12.dp))
                DarkTextField(
                    value = aiModel,
                    onValueChange = { viewModel.updateAiModel(it) },
                    label = "AI Model",
                    placeholder = "gpt-4o-mini",
                    supportingText = "Examples: gpt-4o, gpt-4o-mini, claude-3.5-sonnet",
                )
            }

            // ── Persona Selection (horizontal card picker) ──
            SectionHeader(title = "Persona", color = AuroraPink)
            val personas = listOf(
                PersonaOption("default", "Default", "Balanced", Icons.Default.Person),
                PersonaOption("cool_genius", "高冷学霸", "Cool Genius", Icons.Default.Psychology),
                PersonaOption("hot_blooded", "热血少年", "Hot Blooded", Icons.Default.LocalFireDepartment),
                PersonaOption("sharp_tongue", "毒舌执事", "Sharp Tongue", Icons.Default.AutoStories),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                personas.forEach { persona ->
                    PersonaCard(
                        persona = persona,
                        isSelected = selectedPersona == persona.key,
                        onClick = { viewModel.updatePersona(persona.key) },
                    )
                }
            }

            // ── Custom System Prompt ──
            SectionHeader(title = "Custom System Prompt", color = AuroraViolet)
            GlassCardContainer {
                Text(
                    text = "自定义 AI 系统提示词，留空则使用默认提示词",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFBDB3CC).copy(alpha = 0.7f),
                )
                Spacer(modifier = Modifier.height(8.dp))
                DarkTextField(
                    value = customSystemPrompt,
                    onValueChange = { viewModel.updateCustomSystemPrompt(it) },
                    label = "System Prompt",
                    placeholder = "You are Galize, a social interaction AI assistant...",
                    singleLine = false,
                    minHeight = 120.dp,
                    supportingText = "留空使用默认提示词，包含 3 种回复风格和 JSON 格式要求",
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Permission dialog
    if (permissionState.showDialog && permissionState.currentPermission != null) {
        val permission = permissionState.currentPermission!!
        AlertDialog(
            onDismissRequest = { viewModel.clearPermissionDialog() },
            title = { Text("需要${permission.title}") },
            text = { Text(permission.message) },
            confirmButton = {
                Button(onClick = {
                    viewModel.clearPermissionDialog()
                    PermissionManager.createPermissionSettingIntent(context, permission.type)
                        ?.let { context.startActivity(it) }
                }) { Text("去授权") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearPermissionDialog() }) { Text("取消") }
            },
        )
    }
}

// ── Reusable components ─────────────────────────────────────

@Composable
private fun SectionHeader(title: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE8E0F0),
        )
    }
}

@Composable
private fun GlassCardContainer(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NightElevated.copy(alpha = 0.5f))
            .padding(16.dp),
        content = content,
    )
}

@Composable
private fun DarkTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    singleLine: Boolean = true,
    minHeight: androidx.compose.ui.unit.Dp = 0.dp,
    supportingText: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = Color(0xFFBDB3CC).copy(alpha = 0.3f)) },
        modifier = Modifier
            .fillMaxWidth()
            .then(if (minHeight > 0.dp) Modifier.heightIn(min = minHeight) else Modifier),
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 10,
        supportingText = supportingText?.let { { Text(it) } },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AuroraPurple,
            unfocusedBorderColor = Color(0xFF4A3D66),
            focusedContainerColor = NightSurface.copy(alpha = 0.5f),
            unfocusedContainerColor = NightSurface.copy(alpha = 0.3f),
            cursorColor = AuroraCyan,
            focusedLabelColor = AuroraPurple,
            unfocusedLabelColor = Color(0xFFBDB3CC),
        ),
    )
}

private data class PersonaOption(
    val key: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
)

@Composable
private fun PersonaCard(
    persona: PersonaOption,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val borderColor = if (isSelected) AuroraPurple else Color.Transparent
    val bgColor = if (isSelected) AuroraPurple.copy(alpha = 0.15f) else NightElevated.copy(alpha = 0.5f)

    Column(
        modifier = Modifier
            .width(110.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .then(
                if (isSelected) Modifier.background(
                    color = Color.Transparent,
                    shape = RoundedCornerShape(16.dp),
                ) else Modifier
            )
            .clickable { onClick() }
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            persona.icon,
            contentDescription = null,
            tint = if (isSelected) AuroraPurple else Color(0xFFBDB3CC),
            modifier = Modifier.size(28.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = persona.title,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) AuroraPurple else Color(0xFFE8E0F0),
        )
        Text(
            text = persona.subtitle,
            fontSize = 10.sp,
            color = Color(0xFFBDB3CC).copy(alpha = 0.6f),
        )
    }
}

@Composable
fun PermissionSection(
    permissions: List<PermissionManager.PermissionResult>,
    onRequestPermission: (PermissionManager.PermissionType) -> Unit,
) {
    SectionHeader(title = "权限管理", color = AuroraCyan)
    Spacer(modifier = Modifier.height(8.dp))
    GlassCardContainer {
        permissions.forEach { permission ->
            PermissionItem(permission = permission, onRequestPermission = onRequestPermission)
        }
    }
}

@Composable
fun PermissionItem(
    permission: PermissionManager.PermissionResult,
    onRequestPermission: (PermissionManager.PermissionType) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (permission.isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (permission.isGranted) AuroraGreen else AuroraPink,
                modifier = Modifier.size(22.dp),
            )
            Column {
                Text(
                    text = permission.title,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFFE8E0F0),
                )
                Text(
                    text = if (permission.isGranted) "已授权" else "未授权",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (permission.isGranted) AuroraGreen else AuroraPink,
                )
            }
        }
        if (!permission.isGranted) {
            OutlinedButton(
                onClick = { onRequestPermission(permission.type) },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = AuroraCyan,
                ),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) { Text("申请权限", fontSize = 12.sp) }
        }
    }
}
