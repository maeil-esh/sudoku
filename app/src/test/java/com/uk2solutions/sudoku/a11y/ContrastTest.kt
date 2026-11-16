package com.uk2solutions.sudoku.a11y

import androidx.compose.ui.graphics.Color
import com.uk2solutions.sudoku.ui.theme.SudokuColors
import com.uk2solutions.sudoku.ui.theme.sudokuColors
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.pow

/** WCAG 2.x 대비 계산. 글자 4.5:1, 글자가 아닌 UI 요소(테두리·선택 표시) 3:1 이상을 모든 테마에서 확인하고 표를 남긴다. */
class ContrastTest {
    private fun lin(v: Float): Double { val c = v.toDouble(); return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4) }
    private fun lum(c: Color) = 0.2126 * lin(c.red) + 0.7152 * lin(c.green) + 0.0722 * lin(c.blue)
    private fun ratio(a: Color, b: Color): Double {
        val l1 = lum(a); val l2 = lum(b)
        return (maxOf(l1, l2) + 0.05) / (minOf(l1, l2) + 0.05)
    }

    private data class Pair(val name: String, val fg: (SudokuColors) -> Color, val bg: (SudokuColors) -> Color, val min: Double)

    private val pairs = listOf(
        Pair("본문(ink) / 배경", { it.ink }, { it.background }, 4.5),
        Pair("본문(ink) / 카드(surface)", { it.ink }, { it.surface }, 4.5),
        Pair("본문(ink) / 같은 행·열·박스(cellPeer)", { it.ink }, { it.cellPeer }, 4.5),
        Pair("본문(ink) / 같은 숫자(cellSame)", { it.ink }, { it.cellSame }, 4.5),
        Pair("본문(ink) / 선택 칸(cellSelected)", { it.ink }, { it.cellSelected }, 4.5),
        Pair("보조(muted) / 배경", { it.muted }, { it.background }, 4.5),
        Pair("보조(muted) / 카드", { it.muted }, { it.surface }, 4.5),
        Pair("메모 숫자(muted) / cellPeer", { it.muted }, { it.cellPeer }, 4.5),
        Pair("메모 숫자(muted) / cellSelected", { it.muted }, { it.cellSelected }, 4.5),
        Pair("내 숫자·강조 글자(accent) / 배경", { it.accent }, { it.background }, 4.5),
        Pair("accent / 카드", { it.accent }, { it.surface }, 4.5),
        Pair("accent / cellPeer", { it.accent }, { it.cellPeer }, 4.5),
        Pair("accent / cellSame", { it.accent }, { it.cellSame }, 4.5),
        Pair("accent / cellSelected", { it.accent }, { it.cellSelected }, 4.5),
        Pair("accent / accentSoft(보조 버튼·카드)", { it.accent }, { it.accentSoft }, 4.5),
        Pair("버튼 글자(onAccent) / accent", { it.onAccent }, { it.accent }, 4.5),
        Pair("오류 숫자(error) / errorBg", { it.error }, { it.errorBg }, 4.5),
        Pair("error / 카드", { it.error }, { it.surface }, 4.5),
        Pair("[비텍스트] 선택·힌트 테두리(accent) / 칸 배경", { it.accent }, { it.cellPeer }, 3.0),
        Pair("[비텍스트] 굵은 격자선(ink) / 카드", { it.ink }, { it.surface }, 3.0),
    )

    @Test fun allThemesMeetContrast() {
        val names = listOf("블루(기본)", "그린", "오렌지", "퍼플")
        val sb = StringBuilder("# 색 대비 점검표 (자동 생성)\n\n글자 4.5:1, 비텍스트 3:1 기준(WCAG 2.x). `./gradlew testDebugUnitTest --tests '*ContrastTest'`로 다시 만든다.\n\n")
        val failures = mutableListOf<String>()
        for (dark in listOf(false, true)) for (p in 0 until 4) {
            val c = sudokuColors(dark, p)
            sb.append("## ${names[p]} · ${if (dark) "다크" else "라이트"}\n\n| 조합 | 대비 | 기준 | 결과 |\n|---|---|---|---|\n")
            for (pair in pairs) {
                val r = ratio(pair.fg(c), pair.bg(c))
                val ok = r >= pair.min
                if (!ok) failures += "${names[p]}/${if (dark) "다크" else "라이트"}: ${pair.name} = ${"%.2f".format(r)} (< ${pair.min})"
                sb.append("| ${pair.name} | ${"%.2f".format(r)} | ${pair.min} | ${if (ok) "통과" else "**미달**"} |\n")
            }
            sb.append("\n")
        }
        File(System.getProperty("user.dir"), "../docs/CONTRAST_REPORT.md").apply { parentFile.mkdirs() }.writeText(sb.toString())
        assertTrue("대비 미달:\n" + failures.joinToString("\n"), failures.isEmpty())
    }
}
