package com.uk2solutions.sudoku.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

/** SPEC §6.1 디자인 토큰. 다크 값은 M5에서 같은 역할 이름으로 정의한다. */
@Immutable
data class SudokuColors(
    val background: Color = Color(0xFFF6F4EF),
    val surface: Color = Color(0xFFFFFFFF),
    val ink: Color = Color(0xFF1F2430),
    val muted: Color = Color(0xFF5B6272),
    val line: Color = Color(0xFFD9D5CA),
    val accent: Color = Color(0xFF2F4BCB),
    val accentSoft: Color = Color(0xFFE4E9FB),
    val cellPeer: Color = Color(0xFFECEFF9),
    val cellSelected: Color = Color(0xFFCFD8F8),
    val cellSame: Color = Color(0xFFDCE3FB),
    val error: Color = Color(0xFFB3261E),
    val errorBg: Color = Color(0xFFFBE3E0),
)

val LocalSudokuColors = staticCompositionLocalOf { SudokuColors() }

object SudokuTheme {
    val colors: SudokuColors
        @Composable get() = LocalSudokuColors.current
}

@Composable
fun SudokuTheme(content: @Composable () -> Unit) {
    val c = SudokuColors()
    val scheme = lightColorScheme(
        primary = c.accent,
        onPrimary = Color.White,
        primaryContainer = c.accentSoft,
        background = c.background,
        onBackground = c.ink,
        surface = c.surface,
        onSurface = c.ink,
        onSurfaceVariant = c.muted,
        outline = c.line,
        error = c.error,
        errorContainer = c.errorBg,
    )
    CompositionLocalProvider(LocalSudokuColors provides c) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
