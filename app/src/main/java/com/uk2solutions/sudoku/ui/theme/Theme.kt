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
    /** 트레이닝에서 두 번째 색(단순 컬러링의 주황 쪽). */
    val second: Color = Color(0xFF9A5B00),
)

val LightSudokuColors = SudokuColors(
    background = Color(0xFFF4F6FB), surface = Color(0xFFFFFFFF), ink = Color(0xFF1F2430),
    muted = Color(0xFF5B6272), line = Color(0xFFD5DAE6),
    accent = Color(0xFF2F4BCB), onAccent = Color(0xFFFFFFFF), accentSoft = Color(0xFFE4E9FB),
    cellPeer = Color(0xFFECEEF3), cellSelected = Color(0xFFD6E1FC), cellSame = Color(0xFFBFD3FA),
    error = Color(0xFFB3261E), errorBg = Color(0xFFFBE3E0),
)

val DarkSudokuColors = SudokuColors(
    background = Color(0xFF12141A), surface = Color(0xFF1C1F28), ink = Color(0xFFE8EAF0),
    muted = Color(0xFFA3A9BA), line = Color(0xFF3A3F4E),
    accent = Color(0xFF8FA5FF), onAccent = Color(0xFF101524), accentSoft = Color(0xFF262D4D),
    cellPeer = Color(0xFF242834), cellSelected = Color(0xFF2C3664), cellSame = Color(0xFF26305A),
    error = Color(0xFFFF8A80), errorBg = Color(0xFF4A2523), second = Color(0xFFFFB26B),
)

/** 추가 컬러 테마(프리미엄). 0=기본(블루). 강조색 위 텍스트 대비 4.5:1을 유지하도록 연한 톤을 쓴다. */
fun sudokuColors(dark: Boolean, palette: Int): SudokuColors {
    val base = if (dark) DarkSudokuColors else LightSudokuColors
    fun c(v: Long) = Color(v)
    return when (palette) {
        1 -> if (dark) base.copy(accent = c(0xFF6FD3A0), accentSoft = c(0xFF1F3A2E), cellSelected = c(0xFF22402F), cellSame = c(0xFF1B3528))
        else base.copy(accent = c(0xFF17663F), accentSoft = c(0xFFE1F2E8), cellSelected = c(0xFFD3EBDD), cellSame = c(0xFFBFE3CE))
        2 -> if (dark) base.copy(accent = c(0xFFFFB26B), accentSoft = c(0xFF3F2C1A), cellSelected = c(0xFF4A3015), cellSame = c(0xFF3C2812))
        else base.copy(accent = c(0xFF9A3F05), accentSoft = c(0xFFFBE9D8), cellSelected = c(0xFFF8DEC4), cellSame = c(0xFFF6D5B5))
        3 -> if (dark) base.copy(accent = c(0xFFC0A2FF), accentSoft = c(0xFF33284D), cellSelected = c(0xFF3A2D66), cellSame = c(0xFF2F2454))
        else base.copy(accent = c(0xFF6D28D9), accentSoft = c(0xFFEDE5FB), cellSelected = c(0xFFE4D8FA), cellSame = c(0xFFDCCBFA))
        else -> base
    }
}

val LocalSudokuColors = staticCompositionLocalOf { LightSudokuColors }

object SudokuTheme {
    val colors: SudokuColors
        @Composable get() = LocalSudokuColors.current
}

@Composable
fun SudokuTheme(dark: Boolean = false, palette: Int = 0, content: @Composable () -> Unit) {
    val c = sudokuColors(dark, palette)
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
