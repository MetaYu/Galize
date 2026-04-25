package com.galize.app.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.galize.app.model.db.ConversationEntity
import com.galize.app.ui.theme.*
import com.galize.app.ui.viewmodel.HistoryViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onConversationClick: (Long) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val conversations by viewModel.conversations.collectAsState(initial = emptyList())

    Scaffold(
        containerColor = NightSky,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "History",
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
        if (conversations.isEmpty()) {
            // Empty state with aurora wave animation
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AuroraWaveAnimation(
                        modifier = Modifier
                            .width(200.dp)
                            .height(80.dp),
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No conversations yet",
                        color = Color(0xFFBDB3CC).copy(alpha = 0.6f),
                        fontSize = 15.sp,
                    )
                    Text(
                        text = "Start using Galize to see your sessions here",
                        color = Color(0xFFBDB3CC).copy(alpha = 0.35f),
                        fontSize = 12.sp,
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(conversations) { conversation ->
                    ConversationItem(
                        conversation = conversation,
                        onClick = { onConversationClick(conversation.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: ConversationEntity,
    onClick: () -> Unit,
) {
    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    val affinity = conversation.totalAffinity.coerceIn(0, 100)
    val accentColor = when {
        affinity > 70 -> PureHeartGreen
        affinity > 30 -> AuroraPurple
        else -> ChaosRed
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(NightElevated.copy(alpha = 0.5f))
            .clickable { onClick() }
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left accent bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .defaultMinSize(minHeight = 64.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor),
        )

        // Content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (conversation.contactName.isNotBlank()) conversation.contactName
                        else conversation.appType,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFFE8E0F0),
                    )
                    if (conversation.contactName.isNotBlank() && conversation.packageName.isNotBlank()) {
                        Text(
                            text = conversation.packageName.split('.').last()
                                .replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFBDB3CC).copy(alpha = 0.6f),
                        )
                    }
                }
                Text(
                    text = dateFormat.format(Date(conversation.startedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFBDB3CC).copy(alpha = 0.5f),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (conversation.summary.isNotBlank()) {
                Text(
                    text = conversation.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE8E0F0).copy(alpha = 0.7f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            } else {
                Text(
                    text = "${conversation.messageCount} messages",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFBDB3CC).copy(alpha = 0.5f),
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Affinity: $affinity",
                style = MaterialTheme.typography.bodySmall,
                color = accentColor,
                fontWeight = FontWeight.SemiBold,
            )
        }

        // Mini circular affinity indicator
        Box(
            modifier = Modifier
                .padding(end = 12.dp)
                .size(36.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                progress = { affinity / 100f },
                modifier = Modifier.fillMaxSize(),
                color = accentColor,
                strokeWidth = 3.dp,
                trackColor = Color.White.copy(alpha = 0.06f),
            )
            Text(
                text = "$affinity",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
            )
        }
    }
}

/** Animated aurora wave lines for empty state */
@Composable
private fun AuroraWaveAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "wave")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "wave_phase",
    )

    val colors = listOf(
        AuroraPurple.copy(alpha = 0.5f),
        AuroraCyan.copy(alpha = 0.4f),
        AuroraPink.copy(alpha = 0.3f),
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val mid = h / 2

        colors.forEachIndexed { index, color ->
            val path = Path()
            val amp = 12f + index * 6f
            val freq = 0.02f - index * 0.003f
            val phaseOffset = phase + index * 1.2f

            path.moveTo(0f, mid)
            var x = 0f
            while (x <= w) {
                val y = mid + amp * sin((x * freq + phaseOffset).toDouble()).toFloat()
                path.lineTo(x, y)
                x += 2f
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 2f, cap = StrokeCap.Round),
            )
        }
    }
}
