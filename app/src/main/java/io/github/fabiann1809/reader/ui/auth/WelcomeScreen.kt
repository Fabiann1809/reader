package io.github.fabiann1809.reader.ui.auth

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.fabiann1809.reader.R
import io.github.fabiann1809.reader.ui.components.fadeUp
import io.github.fabiann1809.reader.ui.theme.ReaderTheme

private val LightbulbColor = Color(0xFFE0A43A)

/** First screen without a session: the mark, what the app does and the way in (design "Bienvenida"). */
@Composable
fun WelcomeScreen(onStart: () -> Unit, onHaveAccount: () -> Unit, modifier: Modifier = Modifier) {
    val wave = ReaderTheme.colors.authWave
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(Modifier.fillMaxSize()) {
            WaveBackdrop(wave)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(start = 26.dp, end = 26.dp, bottom = 22.dp),
            ) {
                // What scrolls on a short screen; the way in below it stays where the thumb is.
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(top = 40.dp),
                ) {
                    ReaderLogo(iconSize = 76.dp, modifier = Modifier.fadeUp(50))
                    Text(
                        text = stringResource(R.string.welcome_tagline),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 10.dp).fadeUp(120),
                    )
                    WelcomeIllustration(Modifier.padding(top = 6.dp))
                    Features(Modifier.padding(top = 14.dp).fadeUp(450))
                }
                Button(
                    onClick = onStart,
                    shape = CircleShape,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).heightIn(min = 58.dp).fadeUp(550),
                ) {
                    Text(stringResource(R.string.welcome_start), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(10.dp))
                    Icon(painterResource(R.drawable.ic_caret_right), contentDescription = null, modifier = Modifier.size(18.dp))
                }
                HaveAccountLink(onHaveAccount, Modifier.padding(top = 4.dp))
            }
        }
    }
}

/** The soft circles behind the screen: two at the top, one arc at the bottom. */
@Composable
private fun WaveBackdrop(wave: Color) {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.offset((-150).dp, (-170).dp).size(360.dp).clip(CircleShape).background(wave))
        Box(Modifier.align(Alignment.TopEnd).offset(90.dp, (-110).dp).size(220.dp).clip(CircleShape).background(wave.copy(alpha = 0.6f)))
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 110.dp)
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(wave),
        )
    }
}

@Composable
private fun Features(modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Feature(R.drawable.ic_book_open_bold, R.string.welcome_feature_explain, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
        Feature(R.drawable.ic_lightbulb, R.string.welcome_feature_quiz, LightbulbColor, Modifier.weight(1f))
        Feature(R.drawable.ic_bookmark_simple_fill, R.string.welcome_feature_ideas, MaterialTheme.colorScheme.primary, Modifier.weight(1f))
    }
}

@Composable
private fun Feature(@DrawableRes icon: Int, @StringRes label: Int, tint: Color, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(ReaderTheme.colors.authField),
        ) {
            Icon(painterResource(icon), contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
        }
        Text(
            text = stringResource(label),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun HaveAccountLink(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = modifier.height(48.dp).clickable(role = Role.Button, onClick = onClick),
    ) {
        Text(
            text = buildAnnotatedString {
                append(stringResource(R.string.auth_have_account))
                append(" ")
                withStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)) {
                    append(stringResource(R.string.auth_sign_in_link))
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WelcomePreview() {
    ReaderTheme { WelcomeScreen(onStart = {}, onHaveAccount = {}) }
}
