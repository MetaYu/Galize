package com.galize.app.ui.overlay

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galize.app.ui.theme.*

/**
 * Displays an affinity gauge bar with aurora gradient fill and glow.
 * Range: 0 to 100
 */
@Composable
fun AffinityBar(
    currentAffinity: Int,
    modifier: Modifier = Modifier,
) {
    val clamped = currentAffinity.coerceIn(0, 100)
    val progress by animateFloatAsState(
        targetValue = clamped / 100f,
        animationSpec = tween(600),
        label = "affinity_progress",
    )

    val endColor by animateColorAsState(
        targetValue = when {
            clamped > 70 -> PureHeartGreen
            clamped > 30 -> AuroraPurple
            else -> ChaosRed
        },
        animationSpec = tween(400),
        label = "affinity_end_color",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            androidx.compose.material3.Text(
                text = "Affinity",
                color = Color(0xFFBDB3CC),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
            )
            androidx.compose.material3.Text(
                text = "$clamped / 100",
                color = endColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))

        // Canvas-drawn gradient bar with glow
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
        ) {
            val barHeight = size.height
            val barWidth = size.width
            val radius = CornerRadius(barHeight / 2, barHeight / 2)

            // Track
            drawRoundRect(
                color = Color.White.copy(alpha = 0.08f),
                size = Size(barWidth, barHeight),
                cornerRadius = radius,
            )

            // Filled portion with gradient
            val filledWidth = barWidth * progress
            if (filledWidth > 0f) {
                // Subtle glow behind
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            AuroraPurple.copy(alpha = 0.35f),
                            endColor.copy(alpha = 0.25f),
                        ),
                    ),
                    size = Size(filledWidth + 4.dp.toPx(), barHeight + 4.dp.toPx()),
                    topLeft = Offset(-2.dp.toPx(), -2.dp.toPx()),
                    cornerRadius = radius,
                )

                // Actual bar
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(AuroraPurple, endColor),
                    ),
                    size = Size(filledWidth, barHeight),
                    cornerRadius = radius,
                )
            }
        }
    }
}
