package com.uk2solutions.sudoku.screens

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.uk2solutions.sudoku.R
import com.uk2solutions.sudoku.data.AppSettings
import com.uk2solutions.sudoku.data.DifficultyStat
import com.uk2solutions.sudoku.domain.game.GameRules
import com.uk2solutions.sudoku.domain.game.GameState
import com.uk2solutions.sudoku.domain.generator.PuzzleGenerator
import com.uk2solutions.sudoku.domain.model.Difficulty
import com.uk2solutions.sudoku.domain.model.Units
import com.uk2solutions.sudoku.ui.game.Effect
import com.uk2solutions.sudoku.ui.game.GameBody
import com.uk2solutions.sudoku.ui.game.GameUiState
import com.uk2solutions.sudoku.ui.game.ScoreGain
import com.uk2solutions.sudoku.ui.home.DailyInfo
import com.uk2solutions.sudoku.ui.home.DailyStatus
import com.uk2solutions.sudoku.ui.home.HomeContent
import com.uk2solutions.sudoku.ui.home.ResumeInfo
import com.uk2solutions.sudoku.ui.settings.SettingsBody
import com.uk2solutions.sudoku.ui.stats.StatsContent
import com.uk2solutions.sudoku.ui.stats.StatsUi
import com.uk2solutions.sudoku.ui.theme.SudokuTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** 설치 없이 화면을 PNG로 뽑는다. 결과는 docs/screens 폴더에 저장된다. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "ko-w390dp-h844dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val rule = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    private val outDir = File(System.getProperty("user.dir"), "../docs/screens").apply { mkdirs() }

    private fun shoot(name: String, dark: Boolean = false, advanceMs: Long = 0, content: @Composable () -> Unit) {
        val show = androidx.compose.runtime.mutableStateOf(advanceMs == 0L)
        rule.setContent {
            SudokuTheme(dark = dark) {
                Box(Modifier.fillMaxSize().background(com.uk2solutions.sudoku.ui.theme.SudokuTheme.colors.background)) {
                    if (show.value) content()
                }
            }
        }
        if (advanceMs > 0) {
            // 애니메이션 중간 장면: 시계를 멈춘 뒤 내용을 띄우고 지정한 시간만큼만 진행
            rule.mainClock.autoAdvance = false
            show.value = true
            androidx.compose.runtime.snapshots.Snapshot.sendApplyNotifications()
            rule.mainClock.advanceTimeByFrame()
            rule.mainClock.advanceTimeByFrame()
            rule.mainClock.advanceTimeBy(advanceMs)
        } else rule.waitForIdle()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        val view = rule.activity.window.decorView
        val dm = rule.activity.resources.displayMetrics
        view.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(dm.widthPixels, android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec(dm.heightPixels, android.view.View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, dm.widthPixels, dm.heightPixels)
        val bmp = Bitmap.createBitmap(dm.widthPixels, dm.heightPixels, Bitmap.Config.ARGB_8888)
        view.draw(android.graphics.Canvas(bmp))
        File(outDir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    /** 중간쯤 진행한 게임: 채운 칸, 메모, 실수 1(충돌 표시), 선택 칸의 같은 숫자 강조. */
    private fun midGame(): Pair<GameState, Int> {
        val p = PuzzleGenerator().generate(Difficulty.MEDIUM, 7L)
        var s = GameRules.newState(p.givens, p.solution)
        val empties = s.values.indices.filter { s.values[it] == 0 }
        empties.take(26).forEach { s = GameRules.enter(s, it, s.solution[it], false, false).first }
        val rest = empties.drop(26)
        rest.take(8).forEachIndexed { k, i ->
            for (d in 1..9) if (k % 2 == 0 && (d == s.solution[i] || d % 3 == k % 3)) s = GameRules.enter(s, i, d, true, false).first
        }
        // 이웃과 겹치는 틀린 숫자 하나(충돌 표시용)
        val bad = rest.drop(8).first { i -> Units.peers[i].any { s.values[it] != 0 && s.values[it] != s.solution[i] } }
        val dup = Units.peers[bad].first { s.values[it] != 0 && s.values[it] != s.solution[bad] }
        s = GameRules.enter(s, bad, s.values[dup], false, false).first
        val sel = s.values.indices.first { !s.isGiven(it) && s.values[it] == s.solution[it] }
        return s to sel
    }

    private fun gameUi(extra: GameUiState.() -> GameUiState = { this }): Pair<GameUiState, GameState> {
        val (s, sel) = midGame()
        val ui = GameUiState(loading = false, state = s, difficulty = Difficulty.MEDIUM, selected = sel, conflicts = GameRules.conflicts(s.values))
        return ui.extra() to s
    }

    @Composable private fun Game(ui: GameUiState, s: GameState) =
        GameBody(ui, s, 725_000, {}, {}, {}, {}, {}, {}, {}, {}, {})

    @Test fun gameLight() { val (ui, s) = gameUi(); shoot("01_game_light") { Game(ui, s) } }

    @Test fun gameScoreFloat() {
        val (s0, sel) = midGame()
        val idx = s0.values.indices.first { it != sel && s0.values[it] == s0.solution[it] && !s0.isGiven(it) }
        val (ui, s) = gameUi { copy(scoreGain = ScoreGain(1, idx, 10), effect = Effect(1, (0 until 9).toSet(), 5), hint = null) }
        shoot("02_game_score_and_line_effect", advanceMs = 250) { Game(ui, s) }
    }

    @Test fun gameDark() { val (ui, s) = gameUi(); shoot("03_game_dark", dark = true) { Game(ui, s) } }

    @Test fun gameNumberFirstAndHint() {
        val (s0, _) = midGame()
        val h = com.uk2solutions.sudoku.domain.game.Hints.next(s0)!!
        val (ui, s) = gameUi { copy(settings = AppSettings(numberFirst = true), selectedDigit = 7, hint = h, selected = h.index, noteMode = true) }
        shoot("04_game_numberfirst_hint_notes_on") { Game(ui, s) }
    }

    @Test fun gamePaused() { val (ui, s) = gameUi { copy(paused = true) }; shoot("05_game_paused") { Game(ui, s) } }

    @Test fun home() = shoot("06_home") {
        HomeContent(ResumeInfo(Difficulty.MEDIUM, 725_000, 47), DailyInfo(DailyStatus.IN_PROGRESS, 40, 6), {}, {}, {})
    }

    @Test fun homeDark() = shoot("07_home_dark", dark = true) {
        HomeContent(ResumeInfo(Difficulty.HARD, 1_203_000, 22), DailyInfo(DailyStatus.NOT_STARTED, 0, 6), {}, {}, {})
    }

    @Test @Config(qualifiers = "en-w390dp-h844dp-xxhdpi") fun homeEnglish() = shoot("08_home_english") {
        HomeContent(ResumeInfo(Difficulty.MEDIUM, 725_000, 47), DailyInfo(DailyStatus.DONE, 100, 7), {}, {}, {})
    }

    @Test @Config(qualifiers = "ja-w390dp-h844dp-xxhdpi") fun gameJapanese() { val (ui, s) = gameUi(); shoot("12_game_japanese") { Game(ui, s) } }

    @Test @Config(qualifiers = "de-w390dp-h844dp-xxhdpi") fun homeGerman() = shoot("13_home_german") {
        HomeContent(ResumeInfo(Difficulty.HARD, 1_203_000, 22), DailyInfo(DailyStatus.NOT_STARTED, 0, 6), {}, {}, {})
    }

    @Test fun stats() = shoot("09_stats") {
        StatsContent(
            StatsUi(
                mapOf(
                    "EASY" to DifficultyStat("EASY", 24, 151_000, 232_000.0),
                    "MEDIUM" to DifficultyStat("MEDIUM", 17, 318_000, 441_000.0),
                    "HARD" to DifficultyStat("HARD", 6, 702_000, 913_000.0),
                ),
                streak = 6,
            ),
        )
    }

    @Test @Config(qualifiers = "ko-w390dp-h1900dp-xxhdpi") fun settings() = shoot("10_settings") {
        SettingsBody(
            AppSettings(notifyEnabled = true), "", false, listOf(R.string.theme_system, R.string.theme_light, R.string.theme_dark),
            {}, {}, {}, {}, {},
        )
    }

    @Test @Config(qualifiers = "ko-w390dp-h1900dp-xxhdpi") fun settingsDark() = shoot("11_settings_dark", dark = true) {
        SettingsBody(
            AppSettings(themeMode = 2, numberFirst = true), "ko", false, listOf(R.string.theme_system, R.string.theme_light, R.string.theme_dark),
            {}, {}, {}, {}, {},
        )
    }
}
