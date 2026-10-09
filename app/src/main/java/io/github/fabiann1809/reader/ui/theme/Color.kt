package io.github.fabiann1809.reader.ui.theme

import androidx.compose.ui.graphics.Color

// Design tokens of the v2 redesign ("Reader Rediseño" prototype, CSS variables at the top of the file).
// Screens should use MaterialTheme.colorScheme or ReaderTheme.colors, not these values directly.

// Light mode
val LightBg = Color(0xFFF6EEE2)
val LightSurface = Color(0xFFFFFAF3)
val LightChip = Color(0xFFEDE3D5)
val LightLine = Color(0xFFE3D6C4)
val LightInk = Color(0xFF241B15)
val LightInk2 = Color(0xFF4D4038)
val LightMuted = Color(0xFF7D6F64)
val LightAccent = Color(0xFFA95C24)
val LightOnAccent = Color(0xFFFFFFFF)
val LightAccentSoft = Color(0xFFF2DFC8)
val LightAccentInk = Color(0xFF8E4C1C)
val LightAi = Color(0xFF5B4BC0)
val LightOnAi = Color(0xFFFFFFFF)
val LightAiSoft = Color(0xFFECE7FB)
val LightAiInk = Color(0xFF4A3BA8)
val LightOkSoft = Color(0xFFDDEBE2)
val LightOkInk = Color(0xFF244F37)
val LightOk = Color(0xFF2F6B4A)
val LightWarnSoft = Color(0xFFFBEFD3)
val LightWarnInk = Color(0xFF5A420E)
val LightWarn = Color(0xFF9A6A12)
val LightErrSoft = Color(0xFFF6DDD8)
val LightErrInk = Color(0xFF7A2B22)
val LightErr = Color(0xFFB9473A)
val LightWall = Color(0xFFF2E8DA)
val LightWallShade = Color(0xFFE4D7C5)
val LightShelfTop = Color(0xFF5B463A)
val LightShelfFront = Color(0xFF2A1F19)
val LightNavBar = Color(0xFFFFFAF3)
val LightNavActive = Color(0xFFEFE3D2)

// Dark mode
val DarkBg = Color(0xFF15110E)
val DarkSurface = Color(0xFF221C18)
val DarkChip = Color(0xFF2E2621)
val DarkLine = Color(0xFF3A2F28)
val DarkInk = Color(0xFFF3EAE0)
val DarkInk2 = Color(0xFFD3C6B8)
val DarkMuted = Color(0xFFA79888)
val DarkAccent = Color(0xFFE39556)
val DarkOnAccent = Color(0xFF1E120A)
val DarkAccentSoft = Color(0xFF3B2A1D)
val DarkAccentInk = Color(0xFFF4BC8C)
val DarkAi = Color(0xFFA99BF0)
val DarkOnAi = Color(0xFF17122E)
val DarkAiSoft = Color(0xFF2A2542)
val DarkAiInk = Color(0xFFCFC6FA)
val DarkOkSoft = Color(0xFF1F3328)
val DarkOkInk = Color(0xFFA9DDBF)
val DarkOk = Color(0xFF7FC79E)
val DarkWarnSoft = Color(0xFF3A2F17)
val DarkWarnInk = Color(0xFFF1D79A)
val DarkWarn = Color(0xFFE2B550)
val DarkErrSoft = Color(0xFF3D211D)
val DarkErrInk = Color(0xFFF2B5AB)
val DarkErr = Color(0xFFE07766)
val DarkWall = Color(0xFF2A211B)
val DarkWallShade = Color(0xFF211A15)
val DarkShelfTop = Color(0xFF4A382D)
val DarkShelfFront = Color(0xFF0E0A08)
val DarkNavBar = Color(0xFF261F1B)
val DarkNavActive = Color(0xFF3D3027)

// Colors of a generated cover: the cover itself, the ink of its texts and the band across the top.
class CoverStyle(val cover: Color, val ink: Color, val band: Color)

// Picked by title hash, so a book keeps its look (prototype BOOKS and IMPORTED).
val CoverStyles = listOf(
    CoverStyle(Color(0xFF2F4858), Color(0xFFF4E9D8), Color(0xFFE0A458)),
    CoverStyle(Color(0xFFB9473A), Color(0xFFFFF4E6), Color(0xFFF2C14E)),
    CoverStyle(Color(0xFF1F2A36), Color(0xFFF2C14E), Color(0xFF5C7A99)),
    CoverStyle(Color(0xFFE8D9BF), Color(0xFF3B2A1E), Color(0xFFB4652B)),
    CoverStyle(Color(0xFF5B4B8A), Color(0xFFF4ECFF), Color(0xFFE7B7C8)),
    CoverStyle(Color(0xFF3E5C47), Color(0xFFF1EBD9), Color(0xFFD9B26F)),
    CoverStyle(Color(0xFFC27C3A), Color(0xFF2A1A0E), Color(0xFF2A1A0E)),
    CoverStyle(Color(0xFFF3EFE6), Color(0xFF1F2A36), Color(0xFFB9473A)),
    CoverStyle(Color(0xFF7A8F9C), Color(0xFF0F1A22), Color(0xFFF4E9D8)),
    CoverStyle(Color(0xFF6E3B5C), Color(0xFFFBE9F1), Color(0xFFF2C14E)),
    CoverStyle(Color(0xFF203A43), Color(0xFFBFE3EA), Color(0xFFE07A5F)),
    CoverStyle(Color(0xFFF2C14E), Color(0xFF2A1A0E), Color(0xFF2F4858)),
)

// Pastel gradients of collection bands and highlighted cards (prototype --pA..--pD), two stops each.
val LightPastels = listOf(
    listOf(Color(248, 170, 180, 204), Color(252, 222, 160, 204)),
    listOf(Color(205, 170, 240, 204), Color(150, 205, 240, 204)),
    listOf(Color(160, 215, 190, 209), Color(235, 225, 150, 209)),
    listOf(Color(252, 200, 150, 191), Color(252, 226, 160, 191)),
)
val DarkPastels = listOf(
    listOf(Color(190, 95, 110, 153), Color(200, 150, 80, 153)),
    listOf(Color(130, 95, 190, 153), Color(70, 130, 180, 153)),
    listOf(Color(70, 140, 110, 153), Color(160, 150, 70, 153)),
    listOf(Color(200, 120, 60, 140), Color(200, 160, 70, 140)),
)
