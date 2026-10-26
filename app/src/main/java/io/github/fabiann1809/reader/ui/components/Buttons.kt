package io.github.fabiann1809.reader.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R

// Button styles of the redesign: 54 dp tall pills.
// Only one PrimaryButton per screen; AI actions always use AiButton (lavender).

private val ButtonMinHeight = 54.dp

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
    loading: Boolean = false,
) = FilledButton(text, onClick, modifier, enabled, icon, ButtonDefaults.buttonColors(), loading)

@Composable
fun TonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
) = FilledButton(
    text, onClick, modifier, enabled, icon,
    ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ),
)

@Composable
fun AiButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = R.drawable.ic_sparkle,
    loading: Boolean = false,
) = FilledButton(
    text, onClick, modifier, enabled, icon,
    ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    ),
    loading,
)

@Composable
fun OutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    @DrawableRes icon: Int? = null,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
        modifier = modifier.heightIn(min = ButtonMinHeight),
    ) {
        ButtonContent(text, icon)
    }
}

@Composable
private fun FilledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    @DrawableRes icon: Int?,
    colors: ButtonColors,
    loading: Boolean = false,
) {
    Button(
        // While it works, another tap does nothing; the button keeps its colors so the spinner stays readable.
        onClick = { if (!loading) onClick() },
        enabled = enabled,
        shape = CircleShape,
        colors = colors,
        modifier = modifier.heightIn(min = ButtonMinHeight),
    ) {
        ButtonContent(text, icon, loading)
    }
}

@Composable
private fun ButtonContent(text: String, @DrawableRes icon: Int?, loading: Boolean = false) {
    if (loading) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
        Spacer(Modifier.width(8.dp))
    } else if (icon != null) {
        Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
    }
    Text(text)
}
