package com.galize.app.ui.overlay

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galize.app.ui.theme.*

/**
 * Displays a thinking/loading indicator while AI is processing.
 * Three bouncing aurora dots + gradient text.
 */
@Composable
fun ThinkingPanel(
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")

    // Three dots bounce with staggered delays
    val dotColors = listOf(AuroraPurple, AuroraCyan, AuroraPink)
    val dotOffsets = (0..2).map { index ->
        val offset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = -12f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 500,
                    delayMillis = index * 150,
                    easing = FastOutSlowInEasing,
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "dot_bounce_$index",
        )
        offset
    }

    val dotScales = (0..2).map { index ->
        val s by infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 500,
                    delayMillis = index * 150,
                    easing = FastOutSlowInEasing,
                ),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "dot_scale_$index",
        )
        s
    }

    // Full-screen background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
    ) {
        // Center glass card
        Box(
            modifier = modifier
                .align(Alignment.Center)
                .padding(40.dp)
                .clip(RoundedCornerShape(24.dp))
                .glassCard()
                .padding(horizontal = 36.dp, vertical = 28.dp),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Three bouncing dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(36.dp),
                ) {
                    dotColors.forEachIndexed { index, color ->
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .scale(dotScales[index])
                                .offset(y = dotOffsets[index].dp)
                                .clip(CircleShape)
                                .background(color),
                        )
                    }
                }

                // Aurora gradient "Thinking..." text
                Text(
                    text = "Thinking...",
                    style = androidx.compose.ui.text.TextStyle(
                        brush = auroraHorizontalBrush(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                    textAlign = TextAlign.Center,
                )

                // Subtitle
                Text(
                    text = "AI is analyzing the conversation",
                    color = Color(0xFFBDB3CC),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
