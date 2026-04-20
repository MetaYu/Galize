package com.example.galize.ui.overlay

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.galize.ui.theme.ChaosRed
import com.example.galize.ui.theme.CyberPurple
import com.example.galize.ui.theme.PureHeartGreen

/**
 * Displays an affinity gauge bar that reflects the current relationship score.
 * Range: 0 to 100
 */
@Composable
fun AffinityBar(
    currentAffinity: Int,
    modifier: Modifier = Modifier
) {
    val normalizedAffinity = currentAffinity.coerceIn(0, 100) / 100f

    val barColor by animateColorAsState(
        targetValue = when {
            currentAffinity > 70 -> PureHeartGreen
            currentAffinity > 30 -> CyberPurple
            else -> ChaosRed
        },
        label = "affinity_color"
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Affinity",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "$currentAffinity / 100",
                color = barColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { normalizedAffinity },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = barColor,
            trackColor = Color.White.copy(alpha = 0.1f),
        )
    }
}
