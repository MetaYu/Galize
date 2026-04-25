package com.galize.app.ui.screen

import android.app.Activity
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.galize.app.ui.theme.*
import com.galize.app.ui.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val isServiceRunning by viewModel.isServiceRunning.collectAsState()
    val hasOverlayPermission by viewModel.hasOverlayPermission.collectAsState()
    val hasNotificationPermission by viewModel.hasNotificationPermission.collectAsState()
    val hasMediaProjectionPermission by viewModel.hasMediaProjectionPermission.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val permissionDialogState by viewModel.permissionDialogState.collectAsState()
    val shouldRequestMediaProjection by viewModel.shouldRequestMediaProjection.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(message = it, duration = SnackbarDuration.Long)
            viewModel.clearError()
        }
    }

    LaunchedEffect(Unit) { viewModel.checkPermissions(context) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { viewModel.checkPermissions(context) }

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
            val pm = com.galize.app.service.ScreenCapturePermissionManager(context)
            mediaProjectionPermissionLauncher.launch(pm.createScreenCaptureIntent())
        }
    }

    // Permission dialog
    if (permissionDialogState.showDialog && permissionDialogState.permissionResult != null) {
        val pr = permissionDialogState.permissionResult!!
        AlertDialog(
            onDismissRequest = { viewModel.clearPermissionDialog() },
            title = { Text("需要${pr.title}") },
            text = { Text(pr.message) },
            confirmButton = {
                Button(onClick = {
                    viewModel.clearPermissionDialog()
                    com.galize.app.utils.PermissionManager
                        .createPermissionSettingIntent(context, pr.type)
                        ?.let { context.startActivity(it) }
                }) { Text("去授权") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.clearPermissionDialog() }) { Text("取消") }
            },
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = NightSky,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Galize",
                        style = TextStyle(
                            brush = auroraHorizontalBrush(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                        ),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(Icons.Default.History, "History", tint = AuroraViolet)
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, "Settings", tint = AuroraViolet)
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // ── Brand section ──
            Text(
                text = "万物皆可 Galgame",
                style = TextStyle(
                    brush = auroraHorizontalBrush(),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Everything is a Visual Novel",
                fontSize = 13.sp,
                color = Color(0xFFBDB3CC).copy(alpha = 0.7f),
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ── Permission cards ──
            if (!hasOverlayPermission) {
                PermissionCard(
                    icon = Icons.Default.Layers,
                    label = "悬浮窗权限",
                    onClick = {
                        viewModel.requestPermission(
                            context,
                            com.galize.app.utils.PermissionManager.PermissionType.OVERLAY,
                        )
                    },
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                PermissionCard(
                    icon = Icons.Default.Notifications,
                    label = "通知权限",
                    onClick = {
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    },
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            if (!hasMediaProjectionPermission) {
                PermissionCard(
                    icon = Icons.Default.Screenshot,
                    label = "屏幕截图权限",
                    onClick = {
                        viewModel.requestPermission(
                            context,
                            com.galize.app.utils.PermissionManager.PermissionType.MEDIA_PROJECTION,
                        )
                    },
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Main toggle button (aurora gradient) ──
            val allPermissionsGranted = hasOverlayPermission && hasNotificationPermission
            AuroraButton(
                text = if (isServiceRunning) "Stop Galize" else "Start Galize",
                enabled = allPermissionsGranted,
                onClick = { viewModel.toggleService(context) },
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Status card ──
            StatusCard(isRunning = isServiceRunning)

            Spacer(modifier = Modifier.weight(1f))

            // ── Footer ──
            Text(
                text = "Powered by Galize",
                fontSize = 11.sp,
                color = Color(0xFFBDB3CC).copy(alpha = 0.3f),
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }
    }
}

// ── Sub-components ──────────────────────────────────────────

@Composable
private fun PermissionCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = NightElevated.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = AuroraCyan,
                modifier = Modifier.size(22.dp),
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                color = Color(0xFFE8E0F0),
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFBDB3CC).copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun AuroraButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "btn")
    // Use a full 360-degree phase so the gradient loops seamlessly
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "phase",
    )

    // Two full cycles so the gradient spans 2x width; shifting by 1x width
    // produces the exact same visible pattern → seamless restart.
    val loopColors = listOf(
        AuroraPurple, AuroraCyan, AuroraPink,
        AuroraPurple, AuroraCyan, AuroraPink,
        AuroraPurple, // close the second cycle
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            disabledContainerColor = NightElevated.copy(alpha = 0.4f),
        ),
        contentPadding = PaddingValues(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (enabled) Modifier.drawBehind {
                        // Gradient spans [−shift, 2w−shift]. Visible area [0, w]
                        // always sits inside the gradient, so no edge-clamp artifacts.
                        // At phase≈1 → shift≈w, visible shows 2nd cycle = same as 1st → seamless.
                        val w = size.width
                        val shift = phase * w
                        drawRoundRect(
                            brush = Brush.linearGradient(
                                colors = loopColors,
                                start = Offset(-shift, 0f),
                                end = Offset(2f * w - shift, 0f),
                            ),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx()),
                        )
                    } else Modifier
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) Color.White else Color(0xFFBDB3CC).copy(alpha = 0.4f),
            )
        }
    }
}

@Composable
private fun StatusCard(isRunning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "status")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isRunning) 0.8f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "border_alpha",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isRunning) Modifier.border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            AuroraPurple.copy(alpha = borderAlpha),
                            AuroraCyan.copy(alpha = borderAlpha),
                            AuroraPink.copy(alpha = borderAlpha),
                        ),
                    ),
                    shape = RoundedCornerShape(16.dp),
                ) else Modifier
            )
            .clip(RoundedCornerShape(16.dp))
            .background(NightElevated.copy(alpha = 0.45f))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isRunning) AuroraGreen else Color(0xFF4A3D66)),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Status",
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFE8E0F0),
                    fontSize = 14.sp,
                )
                Text(
                    text = if (isRunning) "Floating bubble is active" else "Service is stopped",
                    color = if (isRunning) AuroraGreen else Color(0xFFBDB3CC).copy(alpha = 0.5f),
                    fontSize = 13.sp,
                )
            }
        }
    }
}
