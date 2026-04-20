package com.example.galize.ui.overlay

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.galize.model.Choice
import com.example.galize.model.ChoiceResult
import com.example.galize.model.ChoiceType
import com.example.galize.ui.theme.*

@Composable
fun ChoicePanel(
    choiceResult: ChoiceResult,
    currentAffinity: Int,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CyberDark.copy(alpha = 0.95f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Affinity bar
            AffinityBar(currentAffinity = currentAffinity)

            // Subtext analysis
            if (choiceResult.subtext.isNotBlank()) {
                Text(
                    text = choiceResult.subtext,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Three choices
            ChoiceButton(
                choice = choiceResult.pureHeart,
                color = PureHeartGreen,
                onClick = { copyToClipboard(context, choiceResult.pureHeart.text) }
            )
            ChoiceButton(
                choice = choiceResult.chaos,
                color = ChaosRed,
                onClick = { copyToClipboard(context, choiceResult.chaos.text) }
            )
            ChoiceButton(
                choice = choiceResult.philosopher,
                color = PhilosopherPurple,
                onClick = { copyToClipboard(context, choiceResult.philosopher.text) }
            )

            // Dismiss
            Text(
                text = "Tap outside to dismiss",
                color = Color.Gray.copy(alpha = 0.5f),
                fontSize = 10.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable { onDismiss() }
            )
        }
    }
}

@Composable
private fun ChoiceButton(
    choice: Choice,
    color: Color,
    onClick: () -> Unit
) {
    val label = when (choice.type) {
        ChoiceType.PURE_HEART -> "[Pure Heart]"
        ChoiceType.CHAOS -> "[Chaos]"
        ChoiceType.PHILOSOPHER -> "[Philosopher]"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.15f))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = choice.text,
            color = Color.White,
            fontSize = 14.sp
        )
        if (choice.description.isNotBlank()) {
            Text(
                text = choice.description,
                color = Color.Gray,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Galize Reply", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
}
