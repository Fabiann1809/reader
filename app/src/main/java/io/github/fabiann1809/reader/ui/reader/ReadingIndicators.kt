package io.github.fabiann1809.reader.ui.reader

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.data.prefs.ReadingSettings
import kotlinx.coroutines.delay
import java.util.Date

/** Height of the strip under the page that holds the indicators. */
val IndicatorsHeight: Dp = 24.dp

// Discreet: the text color at half strength (design 02 §3.3: 40–60 %).
private const val INDICATOR_ALPHA = 0.5f

private const val MINUTE_MILLIS = 60_000L

/** True when the strip under the page is needed. */
val ReadingSettings.showsIndicators: Boolean get() = showClock || showBattery || showPage

/**
 * The discreet indicators under the page (T11.9): the time on the left, the page in the middle
 * and the battery on the right, each one only when it is turned on in "Aa".
 */
@Composable
fun ReadingIndicators(
    settings: ReadingSettings,
    position: Int?,
    positionCount: Int?,
    textColor: Color,
    modifier: Modifier = Modifier,
) {
    val color = textColor.copy(alpha = INDICATOR_ALPHA)
    val style = MaterialTheme.typography.labelSmall
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(IndicatorsHeight)
            .padding(horizontal = 16.dp),
    ) {
        if (settings.showClock) Text(rememberClock(), color = color, style = style, modifier = Modifier.align(Alignment.CenterStart))
        if (settings.showPage && position != null && positionCount != null) {
            Text(
                stringResource(R.string.reader_page_of, position, positionCount),
                color = color,
                style = style,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        if (settings.showBattery) {
            rememberBatteryPercent()?.let { percent ->
                Text(
                    stringResource(R.string.reader_battery, percent),
                    color = color,
                    style = style,
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        }
    }
}

/** Keeps the screen from turning off while this is shown and [enabled] ("Aa" setting). */
@Composable
fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}

// The time in the phone's 12 or 24 hour format, refreshed at the start of each minute.
@Composable
private fun rememberClock(): String {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(MINUTE_MILLIS - now % MINUTE_MILLIS)
            now = System.currentTimeMillis()
        }
    }
    return remember(now) { DateFormat.getTimeFormat(context).format(Date(now)) }
}

// The battery level, from the system's sticky broadcast and its updates; null if unknown.
@Composable
private fun rememberBatteryPercent(): Int? {
    val context = LocalContext.current
    var percent by remember { mutableIntStateOf(-1) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                percent = batteryPercent(intent)
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.let { percent = batteryPercent(it) }
        onDispose { context.unregisterReceiver(receiver) }
    }
    return percent.takeIf { it >= 0 }
}

private fun batteryPercent(intent: Intent): Int {
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    return if (level < 0 || scale <= 0) -1 else level * 100 / scale
}
