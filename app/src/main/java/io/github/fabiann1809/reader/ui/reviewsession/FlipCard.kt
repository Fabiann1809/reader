package io.github.fabiann1809.reader.ui.reviewsession

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.review.DueCard

private const val FLIP_MILLIS = 400

// Far enough that the card doesn't look stretched while it turns.
private const val CAMERA_DISTANCE = 12f

/**
 * The card of a review session (design 7.x "Flashcard 24 dp"): its front on paper and, after a 3D
 * flip, its back on primary-95 with the source. A tap flips it.
 */
@Composable
fun FlipCard(dueCard: DueCard, flipped: Boolean, onFlip: () -> Unit, modifier: Modifier = Modifier) {
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(FLIP_MILLIS),
        label = "cardFlip",
    )
    val showsBack = rotation > 90f
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = if (showsBack) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainerLowest,
        shadowElevation = 2.dp,
        modifier = modifier
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = CAMERA_DISTANCE * density
            }
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.review_flip), onClick = onFlip),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            // The back is drawn turned over again, so its text reads the right way round.
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { rotationY = if (showsBack) 180f else 0f }
                .padding(24.dp),
        ) {
            if (showsBack) CardBack(dueCard) else CardFront(dueCard.card.front)
        }
    }
}

@Composable
private fun CardFront(front: String) {
    Text(
        text = front,
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
        modifier = Modifier.verticalScroll(rememberScrollState()),
    )
}

@Composable
private fun CardBack(dueCard: DueCard) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(R.string.review_answer),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(text = dueCard.card.back, style = MaterialTheme.typography.bodyLarge)
        val page = dueCard.card.page
        Text(
            text = if (page != null) stringResource(R.string.flashcard_source_page, dueCard.bookTitle, page) else dueCard.bookTitle,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
