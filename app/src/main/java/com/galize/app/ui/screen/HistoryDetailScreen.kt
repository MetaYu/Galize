package com.galize.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.galize.app.model.db.ChatLogEntity
import com.galize.app.model.db.ConversationEntity
import com.galize.app.ui.theme.*
import com.galize.app.ui.viewmodel.HistoryDetailViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(
    conversationId: Long,
    onBack: () -> Unit,
    viewModel: HistoryDetailViewModel = hiltViewModel(),
) {
    val conversation by viewModel.conversation.collectAsState()
    val chatLogs by viewModel.chatLogs.collectAsState(initial = emptyList())

    val dateFormat = remember { SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    Scaffold(
        containerColor = NightSky,
        topBar = {
            TopAppBar(
                title = {
                    conversation?.let { conv ->
                        Column {
                            Text(
                                text = if (conv.contactName.isNotBlank()) conv.contactName else "对话详情",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE8E0F0),
                            )
                            if (conv.contactName.isBlank() && conv.appType.isNotBlank()) {
                                Text(
                                    text = conv.appType,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFBDB3CC),
                                )
                            }
                        }
                    }
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
        conversation?.let { conv ->
            if (chatLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "暂无对话记录",
                        color = Color(0xFFBDB3CC).copy(alpha = 0.5f),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // Header info card
                    item {
                        ConversationHeader(conversation = conv, dateFormat = dateFormat)
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Chat messages with time separators
                    var lastDay = ""
                    items(chatLogs) { log ->
                        val logDay = dateFormat.format(Date(log.timestamp))
                        if (logDay != lastDay) {
                            lastDay = logDay
                            TimeDivider(text = logDay)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        ChatMessageBubble(log = log, timeFormat = timeFormat)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationHeader(
    conversation: ConversationEntity,
    dateFormat: SimpleDateFormat,
) {
    val affinity = conversation.totalAffinity.coerceIn(0, 100)
    val accentColor = when {
        affinity > 70 -> PureHeartGreen
        affinity > 30 -> AuroraPurple
        else -> ChaosRed
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(NightElevated.copy(alpha = 0.55f))
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    if (conversation.contactName.isNotBlank()) {
                        Text(
                            text = "联系人: ${conversation.contactName}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE8E0F0),
                        )
                    }
                    if (conversation.packageName.isNotBlank()) {
                        Text(
                            text = "平台: ${conversation.packageName.split('.').last().replaceFirstChar { it.uppercase() }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFBDB3CC),
                        )
                    }
                }
                Text(
                    text = dateFormat.format(Date(conversation.startedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFBDB3CC).copy(alpha = 0.6f),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "消息数: ${conversation.messageCount}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFBDB3CC),
                )
                Text(
                    text = "好感度: $affinity",
                    style = MaterialTheme.typography.bodySmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (conversation.summary.isNotBlank()) {
                Text(
                    text = "摘要: ${conversation.summary}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFBDB3CC).copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun TimeDivider(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color(0xFF4A3D66).copy(alpha = 0.3f),
        )
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp),
            fontSize = 11.sp,
            color = Color(0xFFBDB3CC).copy(alpha = 0.4f),
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color(0xFF4A3D66).copy(alpha = 0.3f),
        )
    }
}

@Composable
private fun ChatMessageBubble(
    log: ChatLogEntity,
    timeFormat: SimpleDateFormat,
) {
    val isMe = log.isFromMe

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 16.dp,
                    )
                )
                .then(
                    if (isMe) Modifier.background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                AuroraPurple.copy(alpha = 0.35f),
                                AuroraPurple.copy(alpha = 0.18f),
                            ),
                        ),
                    ) else Modifier.background(NightElevated.copy(alpha = 0.6f))
                )
                .padding(12.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (log.senderName.isNotEmpty()) {
                    Text(
                        text = log.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isMe) AuroraViolet else AuroraCyan,
                    )
                }

                Text(
                    text = log.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE8E0F0),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Text(
                        text = timeFormat.format(Date(log.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFBDB3CC).copy(alpha = 0.4f),
                    )
                }

                if (log.chosenReply != null && log.chosenReply.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    HorizontalDivider(color = Color(0xFF4A3D66).copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "选择的回复: ${log.chosenReply}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AuroraGreen,
                        fontStyle = FontStyle.Italic,
                    )
                }
            }
        }
    }
}
