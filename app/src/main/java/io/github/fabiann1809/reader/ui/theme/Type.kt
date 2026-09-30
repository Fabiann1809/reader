package io.github.fabiann1809.reader.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.fabiann1809.reader.R

// Both fonts are bundled variable fonts (OFL, see assets/licenses) so they work offline.
private fun variableFont(resId: Int, weight: FontWeight) = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

/** UI font: geometric and friendly, readable at small sizes. */
val PlusJakartaSans = FontFamily(
    variableFont(R.font.plus_jakarta_sans, FontWeight.Normal),
    variableFont(R.font.plus_jakarta_sans, FontWeight.Medium),
    variableFont(R.font.plus_jakarta_sans, FontWeight.SemiBold),
    variableFont(R.font.plus_jakarta_sans, FontWeight.Bold),
)

/** Serif for book titles and display text. */
val Fraunces = FontFamily(
    variableFont(R.font.fraunces, FontWeight.Medium),
    variableFont(R.font.fraunces, FontWeight.SemiBold),
)

private fun ui(size: Int, lineHeight: Int, weight: FontWeight) = TextStyle(
    fontFamily = PlusJakartaSans,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = weight,
)

// Type scale from the design (04-system-design.md, section 3).
val Typography = Typography(
    displayLarge = ui(40, 48, FontWeight.Bold),
    displayMedium = ui(36, 44, FontWeight.Bold),
    displaySmall = ui(32, 40, FontWeight.Bold),
    headlineLarge = ui(28, 36, FontWeight.SemiBold),
    headlineMedium = TextStyle(fontFamily = Fraunces, fontSize = 24.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontFamily = Fraunces, fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = ui(22, 28, FontWeight.SemiBold),
    titleMedium = ui(18, 24, FontWeight.SemiBold),
    titleSmall = ui(16, 22, FontWeight.SemiBold),
    bodyLarge = ui(16, 24, FontWeight.Normal),
    bodyMedium = ui(14, 20, FontWeight.Normal),
    bodySmall = ui(11, 16, FontWeight.Normal),
    labelLarge = ui(14, 20, FontWeight.Medium),
    labelMedium = ui(12, 16, FontWeight.Medium),
    labelSmall = ui(11, 16, FontWeight.Medium),
)
