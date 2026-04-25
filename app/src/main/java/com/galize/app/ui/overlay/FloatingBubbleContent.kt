package com.galize.app.ui.overlay

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galize.app.ui.theme.AuroraCyan
import com.galize.app.ui.theme.AuroraPink
import com.galize.app.ui.theme.AuroraPurple

@Composable
fun FloatingBubbleContent(
    isProcessing: Boolean = false,
    onTap: () -> Unit,
    onDrag: (Float, Float) -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bubble")

    // ── Breathing glow (always running, subtle in idle) ──
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isProcessing) 1.35f else 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isProcessing) 900 else 2400,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow_scale",
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = if (isProcessing) 0.55f else 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isProcessing) 900 else 2400,
                easing = FastOutSlowInEasing,
            ),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow_alpha",
    )

    // ── Gradient rotation (processing = fast spin, idle = slow drift) ──
    val gradientRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isProcessing) 1200 else 8000,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "gradient_rotation",
    )

    // ── Elastic scale for processing ──
    val bubbleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isProcessing) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "bubble_scale",
    )

    val auroraColors = listOf(AuroraPurple, AuroraCyan, AuroraPink, AuroraPurple)

    // Outer box with padding to prevent glow clipping by WindowManager WRAP_CONTENT
    Box(
        modifier = Modifier
            .padding(16.dp) // room for glow halo
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onTap() })
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    onDrag(dragAmount.x, dragAmount.y)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // Inner bubble
        Box(
            modifier = Modifier
                .size(52.dp)
                .scale(bubbleScale)
                // Outer glow halo (drawn on the padded canvas, won't clip)
                .drawBehind {
                    val glowRadius = size.minDimension / 2 * glowScale
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AuroraPurple.copy(alpha = glowAlpha),
                                AuroraCyan.copy(alpha = glowAlpha * 0.4f),
                                Color.Transparent,
                            ),
                            center = center,
                            radius = glowRadius,
                        ),
                        radius = glowRadius,
                        center = center,
                    )
                }
                // Aurora sweep border
                .border(
                    width = 2.dp,
                    brush = Brush.sweepGradient(
                        colors = auroraColors,
                    ),
                    shape = CircleShape,
                )
                .clip(CircleShape)
                // Aurora gradient background (rotates)
                .drawBehind {
                    rotate(gradientRotation) {
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(AuroraPurple, AuroraCyan, AuroraPink),
                                start = Offset.Zero,
                                end = Offset(size.width, size.height),
                            ),
                            size = size,
                        )
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "G",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}
