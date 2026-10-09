package io.github.fabiann1809.reader.ui.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ai.QUIZ_SIZES
import io.github.fabiann1809.reader.ui.components.PrimaryButton
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

/** Before the quiz: what it is about and how many questions to ask (3, 5 or 10). */
@Composable
fun QuizIntro(title: String, state: QuizUiState.Intro, onSelect: (Int) -> Unit, onStart: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp),
        ) {
            IntroCard(title)
            Text(
                text = stringResource(R.string.quiz_how_many),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
            )
            QUIZ_SIZES.forEach { size ->
                SizeRow(size, selected = size == state.selected, enabled = size <= state.maxSize, onClick = { onSelect(size) })
            }
        }
        PrimaryButton(
            text = stringResource(R.string.reader_quiz_start),
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        )
    }
}

@Composable
private fun IntroCard(title: String) {
    val colors = ReaderTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.linearGradient(colors.pastels[1]))
            .padding(22.dp),
    ) {
        Icon(painterResource(R.drawable.ic_sparkle), contentDescription = null, tint = colors.onPastel, modifier = Modifier.size(36.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            color = colors.onPastel,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            text = stringResource(R.string.quiz_intro_description),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onPastel,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun SizeRow(size: Int, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) scheme.inverseSurface else scheme.surfaceContainer,
        contentColor = if (selected) scheme.inverseOnSurface else scheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .heightIn(min = 60.dp)
                .padding(horizontal = 18.dp),
        ) {
            Text(
                text = stringResource(R.string.reader_quiz_questions, size),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (enabled) stringResource(R.string.quiz_size_estimate, estimatedMinutes(size)) else stringResource(R.string.quiz_needs_more_text),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.alpha(0.75f),
            )
        }
    }
}

// About 20 seconds a question, rounded up.
private fun estimatedMinutes(size: Int): Int = (size * 20 + 59) / 60
