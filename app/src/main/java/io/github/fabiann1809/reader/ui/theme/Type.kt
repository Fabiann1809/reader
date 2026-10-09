package io.github.fabiann1809.reader.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.fabiann1809.reader.R

// Fonts are bundled (OFL, see assets/licenses) so they work offline. The reading fonts live in the reader.
private fun variableFont(resId: Int, weight: FontWeight) = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** UI font: clean and geometric, heavy weights for titles. */
val Manrope = FontFamily(
    variableFont(R.font.manrope, FontWeight.Normal),
    variableFont(R.font.manrope, FontWeight.Medium),
    variableFont(R.font.manrope, FontWeight.SemiBold),
    variableFont(R.font.manrope, FontWeight.Bold),
    variableFont(R.font.manrope, FontWeight.ExtraBold),
)

/** Serif for the titles printed on generated book covers. */
val DmSerifDisplay = FontFamily(Font(R.font.dm_serif_display, FontWeight.Normal))

private fun ui(size: Int, lineHeight: Int, weight: FontWeight, tracking: TextUnit = 0.em) = TextStyle(
    fontFamily = Manrope,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = weight,
    letterSpacing = tracking,
)

// Type scale of the redesign: ExtraBold titles with tight tracking, as in the prototype.
val Typography = Typography(
    displayLarge = ui(48, 52, FontWeight.ExtraBold, (-0.04).em),
    displayMedium = ui(40, 44, FontWeight.ExtraBold, (-0.04).em),
    displaySmall = ui(32, 38, FontWeight.ExtraBold, (-0.03).em),
    headlineLarge = ui(30, 36, FontWeight.ExtraBold, (-0.03).em),
    headlineMedium = ui(24, 30, FontWeight.ExtraBold, (-0.02).em),
    headlineSmall = ui(20, 26, FontWeight.ExtraBold, (-0.01).em),
    titleLarge = ui(22, 28, FontWeight.Bold),
    titleMedium = ui(18, 24, FontWeight.Bold),
    titleSmall = ui(16, 22, FontWeight.Bold),
    bodyLarge = ui(16, 24, FontWeight.Medium),
    bodyMedium = ui(14, 20, FontWeight.Medium),
    bodySmall = ui(12, 16, FontWeight.Medium),
    labelLarge = ui(14, 20, FontWeight.Bold),
    labelMedium = ui(12, 16, FontWeight.Bold),
    labelSmall = ui(11, 16, FontWeight.Bold),
)
