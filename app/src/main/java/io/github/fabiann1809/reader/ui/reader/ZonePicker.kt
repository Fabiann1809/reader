package io.github.fabiann1809.reader.ui.reader

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.components.OutlineButton
import io.github.fabiann1809.reader.ui.components.PrimaryButton

// Outside the marked zone the page is dimmed, so the zone stands out.
private const val DIM_ALPHA = 0.45f

// Smaller than this can't hold a line of text worth explaining.
private val MinZoneSize = 32.dp

/**
 * Marking a paragraph of a PDF page to explain it (T11.14): the finger draws a rectangle, then
 * "Explicar" sends that part of the page to the OCR and the explainer. "Atrás" or "Cancelar" goes
 * back to reading. It covers the page, so pages don't turn meanwhile.
 */
@Composable
fun ZonePicker(state: ZonePicking, onExplain: (PageZone) -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    BackHandler(onBack = onCancel)
    var zone by remember { mutableStateOf<PageZone?>(null) }
    var start by remember { mutableStateOf(Offset.Zero) }
    val minSize = with(LocalDensity.current) { MinZoneSize.toPx() }
    val border = MaterialTheme.colorScheme.primary
    Box(modifier.fillMaxSize()) {
        Canvas(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { start = it },
                        onDrag = { change, _ -> zone = PageZone.between(start.x, start.y, change.position.x, change.position.y) },
                    )
                },
        ) {
            val marked = zone
            val dim = Color.Black.copy(alpha = DIM_ALPHA)
            if (marked == null) {
                drawRect(dim)
                return@Canvas
            }
            // Four bands around the zone: the zone itself stays clear.
            drawRect(dim, Offset.Zero, Size(size.width, marked.top))
            drawRect(dim, Offset(0f, marked.bottom), Size(size.width, size.height - marked.bottom))
            drawRect(dim, Offset(0f, marked.top), Size(marked.left, marked.height))
            drawRect(dim, Offset(marked.right, marked.top), Size(size.width - marked.right, marked.height))
            drawRect(border, Offset(marked.left, marked.top), Size(marked.width, marked.height), style = Stroke(width = 2.dp.toPx()))
        }
        Hint(state, Modifier.align(Alignment.TopCenter))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(16.dp),
        ) {
            OutlineButton(text = stringResource(R.string.zone_cancel), onClick = onCancel)
            PrimaryButton(
                text = stringResource(R.string.selection_explain),
                onClick = { zone?.let(onExplain) },
                enabled = zone?.isUsable(minSize) == true && state != ZonePicking.READING,
            )
        }
    }
}

@Composable
private fun Hint(state: ZonePicking, modifier: Modifier = Modifier) {
    val text = when (state) {
        ZonePicking.MARKING -> R.string.zone_hint
        ZonePicking.READING -> R.string.zone_reading
        ZonePicking.NO_TEXT -> R.string.zone_no_text
    }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 6.dp,
        modifier = modifier
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            if (state == ZonePicking.READING) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Text(stringResource(text), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
