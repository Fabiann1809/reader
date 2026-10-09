package io.github.fabiann1809.reader.ui.reviewsession

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.components.Motion
import io.github.fabiann1809.reader.ui.components.rememberReduceMotion
import io.github.fabiann1809.reader.ui.review.DueCard
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

private const val FLIP_MILLIS = 400

// Far enough that the card doesn't look stretched while it turns.
private const val CAMERA_DISTANCE = 12f

private val CardShape = RoundedCornerShape(32.dp)

/**
 * The card of a review session: its question on paper and, after a 3D flip, its answer on a pastel
 * gradient with the source. A tap flips it.
 */
@Composable
fun FlipCard(dueCard: DueCard, flipped: Boolean, onFlip: () -> Unit, modifier: Modifier = Modifier) {
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(Motion.duration(FLIP_MILLIS, rememberReduceMotion())),
        label = "cardFlip",
    )
    val showsBack = rotation > 90f
    val face = if (showsBack) {
        Modifier.background(Brush.linearGradient(ReaderTheme.colors.pastels[1]))
    } else {
        Modifier.background(MaterialTheme.colorScheme.surfaceContainerLowest)
    }
    Box(
        modifier = modifier
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = CAMERA_DISTANCE * density
            }
            .shadow(14.dp, CardShape)
            .clip(CardShape)
            .then(face)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.review_flip), onClick = onFlip),
    ) {
        Column(
            // The back is drawn turned over again, so its text reads the right way round.
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationY = if (showsBack) 180f else 0f }
                .padding(26.dp),
        ) {
            if (showsBack) CardBack(dueCard) else CardFront(dueCard.card.front)
        }
    }
}

@Composable
private fun ColumnScope.CardFront(front: String) {
    CardLabel(stringResource(R.string.review_question), MaterialTheme.colorScheme.primary)
    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Text(
            text = front,
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.ExtraBold),
            modifier = Modifier.verticalScroll(rememberScrollState()),
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(
            painterResource(R.drawable.ic_hand),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Text(
            stringResource(R.string.review_tap_to_flip),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ColumnScope.CardBack(dueCard: DueCard) {
    val ink = ReaderTheme.colors.onPastel
    CardLabel(stringResource(R.string.review_answer), ink)
    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
        Text(
            text = dueCard.card.back,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
            color = ink,
            modifier = Modifier.verticalScroll(rememberScrollState()),
        )
    }
    HorizontalDivider(color = ink.copy(alpha = 0.25f), modifier = Modifier.padding(bottom = 12.dp))
    val page = dueCard.card.page
    Text(
        text = if (page != null) stringResource(R.string.flashcard_source_page, dueCard.bookTitle, page) else dueCard.bookTitle,
        style = MaterialTheme.typography.labelMedium,
        color = ink,
    )
}

@Composable
private fun CardLabel(text: String, color: Color) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.12.em),
        color = color,
    )
}
