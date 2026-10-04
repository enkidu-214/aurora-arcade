package com.aurora.arcade.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF0A101D)
val Panel = Color(0xFF141E2E)
val Mint = Color(0xFF9CF4CC)
val Cream = Color(0xFFF3F0E7)
val Muted = Color(0xFF8290A7)
val Outline = Color(0xFF273449)
// Official web game palette, sampled from normal 25 × 25 minos on 2026-10-04.
// The center is the main color; the renderer preserves the sampled light/dark range.
internal data class BlockPalette(val face: Color,val upper: Color,val lower: Color,val rim: Color,val outline: Color)
internal val PiecePalette = listOf(
    BlockPalette(Color(0xFF00B0C5),Color(0xFF00A2B3),Color(0xFF00D6F4),Color(0xFF00727F),Color(0xFF438C95)), // I
    BlockPalette(Color(0xFFC5B300),Color(0xFFB3A400),Color(0xFFF4DC00),Color(0xFF7F7500),Color(0xFF958E43)), // O
    BlockPalette(Color(0xFFA400C5),Color(0xFF9800B3),Color(0xFFC600F4),Color(0xFF6A007F),Color(0xFF864395)), // T
    BlockPalette(Color(0xFF00C546),Color(0xFF00B33F),Color(0xFF00F45C),Color(0xFF007F23),Color(0xFF43955C)), // S
    BlockPalette(Color(0xFFC50000),Color(0xFFB30000),Color(0xFFF40000),Color(0xFF7F0000),Color(0xFF954343)), // Z
    BlockPalette(Color(0xFF007BC5),Color(0xFF006FB3),Color(0xFF0091F4),Color(0xFF00437F),Color(0xFF437095)), // J
    BlockPalette(Color(0xFFC58E00),Color(0xFFB38200),Color(0xFFF4A700),Color(0xFF7F5500),Color(0xFF957B43)), // L
)
val PieceColors = listOf(Color.Transparent)+PiecePalette.map { it.face }
val GameText = Color(0xFFDFE7EC)
val GameMuted = Color(0xFFAFBEC9)
val GameAccent = Color(0xFFA6C9BF)
val GameGrid = Color(0xFF242323)
val GamePanel = Color(0xFF18232D)
val GameBackground = Color(0xFF263540)
val GameBackgroundLight = Color(0xFF324554)
val GameFrame = Color(0xFF435664)
val GameFrameLight = Color(0xFF627A89)
@Composable fun AuroraTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme(primary=Mint,onPrimary=Ink,background=Ink,surface=Panel,
        onBackground=Cream,onSurface=Cream,secondary=Mint,outline=Outline),
        typography=Typography(bodyLarge=TextStyle(fontFamily=FontFamily.SansSerif,fontSize=14.sp))) {
        CompositionLocalProvider(LocalContentColor provides Cream) { content() }
    }
}
