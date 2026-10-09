package io.github.fabiann1809.reader.ui.quiz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

/** How an option looks: before answering, or once the answer is shown. */
enum class OptionLook { NORMAL, CORRECT, WRONG, DIMMED }

/** One of the four answers: its letter (or ✓ / ✗ once answered) and its text. */
@Composable
fun QuizOption(letter: Char, text: String, look: OptionLook, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = ReaderTheme.colors
    val scheme = MaterialTheme.colorScheme
    val accent = when (look) {
        OptionLook.CORRECT -> colors.success
        OptionLook.WRONG -> scheme.error
        else -> null
    }
    val container = when (look) {
        OptionLook.CORRECT -> colors.successContainer
        OptionLook.WRONG -> scheme.errorContainer
        else -> scheme.surfaceContainerLowest
    }
    Surface(
        onClick = onClick,
        enabled = look == OptionLook.NORMAL,
        shape = RoundedCornerShape(18.dp),
        color = container,
        border = BorderStroke(1.5.dp, accent ?: scheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .alpha(if (look == OptionLook.DIMMED) 0.5f else 1f),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .heightIn(min = 58.dp)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            OptionBadge(letter, look, accent)
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun OptionBadge(letter: Char, look: OptionLook, accent: Color?) {
    val badge = Modifier.size(30.dp)
    if (accent != null) {
        Box(badge.background(accent, CircleShape), contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(if (look == OptionLook.CORRECT) R.drawable.ic_check else R.drawable.ic_x),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
        }
    } else {
        Box(badge.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape), contentAlignment = Alignment.Center) {
            Text(letter.toString(), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold))
        }
    }
}

/** The look of option [option] when [chosen] was picked and [correct] is the right one. */
fun optionLook(option: Int, chosen: Int?, correct: Int): OptionLook = when {
    chosen == null -> OptionLook.NORMAL
    option == correct -> OptionLook.CORRECT
    option == chosen -> OptionLook.WRONG
    else -> OptionLook.DIMMED
}
