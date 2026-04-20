package com.example.galize.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.galize.ui.theme.CyberPurple

@Composable
fun FloatingBubbleContent(onTap: () -> Unit) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(CyberPurple.copy(alpha = 0.9f))
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "G",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
