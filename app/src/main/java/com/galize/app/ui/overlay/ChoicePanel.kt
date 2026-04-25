package com.galize.app.ui.overlay

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galize.app.model.Choice
import com.galize.app.model.ChoiceResult
import com.galize.app.model.ChoiceType
import com.galize.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ChoicePanel(
    choiceResult: ChoiceResult,
    currentAffinity: Int,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Track which button just copied (index or null)
    var copiedIndex by remember { mutableStateOf<Int?>(null) }

    // Full-screen clickable background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable { onDismiss() },
    ) {
        // Bottom panel – glass card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(12.dp)
                .clip(RoundedCornerShape(24.dp))
                .glassCard()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { /* block propagation */ }
                .padding(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Affinity bar
                AffinityBar(currentAffinity = currentAffinity)

                // Subtext analysis
                if (choiceResult.subtext.isNotBlank()) {
                    Text(
                        text = choiceResult.subtext,
                        color = Color(0xFFBDB3CC),
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }

                // Three choices with left accent bar + icon
                val choices = listOf(
                    Triple(choiceResult.pureHeart, PureHeartGreen, Icons.Default.Favorite),
                    Triple(choiceResult.chaos, ChaosRed, Icons.Default.LocalFireDepartment),
                    Triple(choiceResult.philosopher, PhilosopherPurple, Icons.Default.AutoStories),
                )

                choices.forEachIndexed { index, (choice, color, icon) ->
                    ChoiceButton(
                        choice = choice,
                        color = color,
                        icon = icon,
                        isCopied = copiedIndex == index,
                        onClick = {
                            copyToClipboard(context, choice.text)
                            copiedIndex = index
                            scope.launch {
                                delay(1200)
                                if (copiedIndex == index) copiedIndex = null
                            }
                        },
                    )
                }

                // Dismiss hint
                Text(
                    text = "Tap outside to dismiss",
                    color = Color(0xFFBDB3CC).copy(alpha = 0.4f),
                    fontSize = 10.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

@Composable
private fun ChoiceButton(
    choice: Choice,
    color: Color,
    icon: ImageVector,
    isCopied: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "choice_scale",
    )

    val label = when (choice.type) {
        ChoiceType.PURE_HEART -> "Pure Heart"
        ChoiceType.CHAOS -> "Chaos"
        ChoiceType.PHILOSOPHER -> "Philosopher"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.10f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
            ) { onClick() }
            .padding(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Left accent bar
        Box(
            modifier = Modifier
                .width(4.dp)
                .fillMaxHeight()
                .defaultMinSize(minHeight = 48.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color),
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Icon
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(18.dp),
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Text content
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 10.dp),
        ) {
            Text(
                text = if (isCopied) "Copied!" else label,
                color = if (isCopied) AuroraGreen else color,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = choice.text,
                color = Color(0xFFE8E0F0),
                fontSize = 14.sp,
            )
            if (choice.description.isNotBlank()) {
                Text(
                    text = choice.description,
                    color = Color(0xFFBDB3CC).copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Galize Reply", text)
    clipboard.setPrimaryClip(clip)
}
