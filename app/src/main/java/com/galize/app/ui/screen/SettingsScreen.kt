package com.galize.app.ui.screen

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.galize.app.ui.viewmodel.SettingsViewModel
import com.galize.app.utils.PermissionManager
import com.galize.app.service.ScreenCapturePermissionManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val apiKey by viewModel.apiKey.collectAsState()
    val apiBaseUrl by viewModel.apiBaseUrl.collectAsState()
    val selectedPersona by viewModel.selectedPersona.collectAsState()
    val permissionState by viewModel.permissionState.collectAsState()
    val shouldRequestMediaProjection by viewModel.shouldRequestMediaProjection.collectAsState()
    
    // Media projection permission launcher
    val mediaProjectionPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val granted = result.resultCode == Activity.RESULT_OK
        val data = result.data
        viewModel.onMediaProjectionPermissionResult(granted, data)
    }
    
    // Handle media projection permission request
    LaunchedEffect(shouldRequestMediaProjection) {
        if (shouldRequestMediaProjection) {
            val permissionManager = ScreenCapturePermissionManager(context)
            mediaProjectionPermissionLauncher.launch(permissionManager.createScreenCaptureIntent())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        // 添加滚动功能
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 权限管理区域
            PermissionSection(
                permissions = permissionState.permissions,
                onRequestPermission = { permissionType ->
                    viewModel.requestPermission(permissionType)
                }
            )
            
            HorizontalDivider()
            // API Configuration
            Text(text = "AI Configuration", style = MaterialTheme.typography.titleMedium)

            OutlinedTextField(
                value = apiBaseUrl,
                onValueChange = { viewModel.updateApiBaseUrl(it) },
                label = { Text("API Base URL") },
                placeholder = { Text("https://api.openai.com/v1") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = { viewModel.updateApiKey(it) },
                label = { Text("API Key") },
                placeholder = { Text("sk-...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            HorizontalDivider()

            // Persona Selection
            Text(text = "Persona", style = MaterialTheme.typography.titleMedium)

            val personas = listOf(
                "default" to "Default (Balanced)",
                "cool_genius" to "Cool Genius (高冷学霸)",
                "hot_blooded" to "Hot Blooded (热血少年)",
                "sharp_tongue" to "Sharp Tongue (毒舌执事)"
            )

            personas.forEach { (key, label) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RadioButton(
                        selected = selectedPersona == key,
                        onClick = { viewModel.updatePersona(key) }
                    )
                    Text(
                        text = label,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
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
                Button(
                    onClick = {
                        viewModel.clearPermissionDialog()
                        val intent = PermissionManager.createPermissionSettingIntent(
                            context,
                            permission.type
                        )
                        intent?.let { context.startActivity(it) }
                    }
                ) {
                    Text("去授权")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.clearPermissionDialog() }
                ) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 权限管理区域组件
 */
@Composable
fun PermissionSection(
    permissions: List<com.galize.app.utils.PermissionManager.PermissionResult>,
    onRequestPermission: (com.galize.app.utils.PermissionManager.PermissionType) -> Unit
) {
    Text(text = "权限管理", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    
    permissions.forEach { permission ->
        PermissionItem(
            permission = permission,
            onRequestPermission = onRequestPermission
        )
    }
}

/**
 * 单个权限项组件
 */
@Composable
fun PermissionItem(
    permission: com.galize.app.utils.PermissionManager.PermissionResult,
    onRequestPermission: (com.galize.app.utils.PermissionManager.PermissionType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (permission.isGranted) 
                Color(0xFFE8F5E9).copy(alpha = 0.3f)
            else 
                Color(0xFFFFF3E0).copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (permission.isGranted) 
                        Icons.Default.CheckCircle 
                    else 
                        Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (permission.isGranted) 
                        Color(0xFF4CAF50) 
                    else 
                        Color(0xFFFF9800),
                    modifier = Modifier.size(24.dp)
                )
                
                Column {
                    Text(
                        text = permission.title,
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = if (permission.isGranted) "已授权" else "未授权",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (permission.isGranted) 
                            Color(0xFF4CAF50) 
                        else 
                            Color(0xFFFF9800)
                    )
                }
            }
            
            if (!permission.isGranted) {
                OutlinedButton(
                    onClick = { onRequestPermission(permission.type) },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text("申请权限")
                }
            }
        }
    }
}
