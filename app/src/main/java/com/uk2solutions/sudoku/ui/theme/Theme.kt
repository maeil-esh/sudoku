package com.uk2solutions.sudoku.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** SPEC §6.1 디자인 토큰(역할 이름 고정). 값은 docs/DECISIONS.md 참고. */
@Immutable
data class SudokuColors(
    val background: Color, val surface: Color, val ink: Color, val muted: Color, val line: Color,
    val accent: Color, val onAccent: Color, val accentSoft: Color,
    val cellPeer: Color, val cellSelected: Color, val cellSame: Color,
    val error: Color, val errorBg: Color,
)

val LightSudokuColors = SudokuColors(
    background = Color(0xFFF4F6FB), surface = Color(0xFFFFFFFF), ink = Color(0xFF1F2430),
    muted = Color(0xFF5B6272), line = Color(0xFFD5DAE6),
    accent = Color(0xFF2F4BCB), onAccent = Color(0xFFFFFFFF), accentSoft = Color(0xFFE4E9FB),
    cellPeer = Color(0xFFECEEF3), cellSelected = Color(0xFFCFDCFB), cellSame = Color(0xFFBFD3FA),
    error = Color(0xFFB3261E), errorBg = Color(0xFFFBE3E0),
)

val DarkSudokuColors = SudokuColors(
    background = Color(0xFF12141A), surface = Color(0xFF1C1F28), ink = Color(0xFFE8EAF0),
    muted = Color(0xFFA3A9BA), line = Color(0xFF3A3F4E),
    accent = Color(0xFF8FA5FF), onAccent = Color(0xFF101524), accentSoft = Color(0xFF262D4D),
    cellPeer = Color(0xFF242834), cellSelected = Color(0xFF3F4C86), cellSame = Color(0xFF323F78),
    error = Color(0xFFFF8A80), errorBg = Color(0xFF4A2523),
)

val LocalSudokuColors = staticCompositionLocalOf { LightSudokuColors }

object SudokuTheme {
    val colors: SudokuColors
        @Composable get() = LocalSudokuColors.current
}

@Composable
fun SudokuTheme(dark: Boolean = false, content: @Composable () -> Unit) {
    val c = if (dark) DarkSudokuColors else LightSudokuColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.accent, onPrimary = c.onAccent, primaryContainer = c.accentSoft, onPrimaryContainer = c.ink,
            background = c.background, onBackground = c.ink, surface = c.surface, onSurface = c.ink,
            surfaceVariant = c.accentSoft, onSurfaceVariant = c.muted, outline = c.line,
            error = c.error, errorContainer = c.errorBg,
        )
    } else {
        lightColorScheme(
            primary = c.accent, onPrimary = c.onAccent, primaryContainer = c.accentSoft, onPrimaryContainer = c.ink,
            background = c.background, onBackground = c.ink, surface = c.surface, onSurface = c.ink,
            surfaceVariant = c.accentSoft, onSurfaceVariant = c.muted, outline = c.line,
            error = c.error, errorContainer = c.errorBg,
        )
    }
    CompositionLocalProvider(LocalSudokuColors provides c) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
