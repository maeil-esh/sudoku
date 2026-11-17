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
import com.uk2solutions.sudoku.ui.premium.PremiumContent
import com.uk2solutions.sudoku.ui.premium.PremiumMessage
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

    private fun shoot(name: String, dark: Boolean = false, advanceMs: Long = 0, palette: Int = 0, content: @Composable () -> Unit) {
        val show = androidx.compose.runtime.mutableStateOf(advanceMs == 0L)
        rule.setContent {
            SudokuTheme(dark = dark, palette = palette) {
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

    /** 번역이 없는 언어(중국어)로 설정된 폰은 영어 기본 문자열이 나와야 한다. */
    @Test @Config(qualifiers = "zh-w390dp-h844dp-xxhdpi") fun homeUnsupportedLanguageFallsBackToEnglish() = shoot("14_home_unsupported_zh_fallback_english") {
        HomeContent(ResumeInfo(Difficulty.MEDIUM, 725_000, 47), DailyInfo(DailyStatus.NOT_STARTED, 0, 3), {}, {}, {})
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

    @Test fun premiumScreen() = shoot("15_premium") {
        PremiumContent(false, false, "₩3,900", true, null, {}, {}, {}, {})
    }

    @Test fun premiumScreenPendingAndMessage() = shoot("16_premium_promo_not_confirmed") {
        PremiumContent(false, true, "₩3,900", true, PremiumMessage.PROMO_NOT_CONFIRMED, {}, {}, {}, {})
    }

    @Test fun premiumActive() = shoot("17_premium_active", dark = true) {
        PremiumContent(true, false, null, true, PremiumMessage.PROMO_APPLIED, {}, {}, {}, {})
    }

    @Test fun statsPremiumDetail() = shoot("18_stats_premium_detail") {
        StatsContent(
            StatsUi(
                mapOf(
                    "EASY" to DifficultyStat("EASY", 24, 151_000, 232_000.0),
                    "MEDIUM" to DifficultyStat("MEDIUM", 17, 318_000, 441_000.0),
                    "HARD" to DifficultyStat("HARD", 6, 702_000, 913_000.0),
                ),
                streak = 6, weekly = listOf(0, 1, 0, 2, 1, 3, 1), premium = true,
            ),
        )
    }

    @Test fun homePremiumMember() = shoot("19_home_premium_member") {
        HomeContent(ResumeInfo(Difficulty.HARD, 1_203_000, 22), DailyInfo(DailyStatus.DONE, 100, 7), {}, {}, {}, premium = true)
    }

    @Test fun gameGreenPalette() { val (ui, s) = gameUi { copy(premium = true) }; shoot("20_game_green_theme", palette = 1) { Game(ui, s) } }

    @Test fun gameOrangeDark() { val (ui, s) = gameUi { copy(premium = true) }; shoot("21_game_orange_dark", dark = true, palette = 2) { Game(ui, s) } }

    @Test fun gamePurple() { val (ui, s) = gameUi { copy(premium = true) }; shoot("22_game_purple_theme", palette = 3) { Game(ui, s) } }

    @Test @Config(qualifiers = "zh-rCN-w390dp-h844dp-xxhdpi") fun homeChinese() = shoot("23_home_chinese") {
        HomeContent(ResumeInfo(Difficulty.MEDIUM, 725_000, 47), DailyInfo(DailyStatus.IN_PROGRESS, 40, 6), {}, {}, {})
    }

    @Test @Config(qualifiers = "th-w390dp-h844dp-xxhdpi") fun homeThai() = shoot("24_home_thai") {
        HomeContent(ResumeInfo(Difficulty.MEDIUM, 725_000, 47), DailyInfo(DailyStatus.IN_PROGRESS, 40, 6), {}, {}, {})
    }

    @Test @Config(qualifiers = "hi-w390dp-h844dp-xxhdpi") fun homeHindi() = shoot("25_home_hindi") {
        HomeContent(ResumeInfo(Difficulty.MEDIUM, 725_000, 47), DailyInfo(DailyStatus.IN_PROGRESS, 40, 6), {}, {}, {})
    }

    @Test @Config(qualifiers = "tr-w390dp-h844dp-xxhdpi") fun premiumTurkish() = shoot("26_premium_turkish") {
        PremiumContent(false, false, "₺49,99", true, null, {}, {}, {}, {})
    }

    /** 틀린 숫자 하나가 남아 있으면 안내가 뜨고 숫자 패드가 막힌다(기본 게임 상태에 틀린 칸 포함). */
    @Test fun gameWrongNumberBlocked() { val (ui, s) = gameUi { copy(blockedNudge = 1) }; shoot("27_game_wrong_number_blocked", advanceMs = 100) { Game(ui, s) } }

    @Test fun challengeWin() = shoot("28_challenge_result_win") {
        com.uk2solutions.sudoku.ui.challenge.CompleteCard(
            elapsedMs = 612_000, mistakes = 0, score = 2860,
            opponent = com.uk2solutions.sudoku.domain.challenge.ChallengeResult(725, 1, 0, 2340), onSend = {}, onNewGame = {}, onHome = {},
        )
    }

    @Test fun challengeLoseDark() = shoot("29_challenge_result_lose_dark", dark = true) {
        com.uk2solutions.sudoku.ui.challenge.CompleteCard(
            elapsedMs = 905_000, mistakes = 2, score = 1620,
            opponent = com.uk2solutions.sudoku.domain.challenge.ChallengeResult(725, 1, 0, 2340), onSend = {}, onNewGame = {}, onHome = {},
        )
    }

    @Test fun challengeComplete() = shoot("30_complete_send_challenge") {
        com.uk2solutions.sudoku.ui.challenge.CompleteCard(elapsedMs = 725_000, mistakes = 1, score = 2340, opponent = null, onSend = {}, onNewGame = {}, onHome = {})
    }

    @Test fun challengeReceive() = shoot("31_challenge_receive") {
        com.uk2solutions.sudoku.ui.challenge.ReceiveCard(
            initialText = "", error = false, onStart = {}, onDismiss = {},
            pasted = "스도쿠 도전장! 내 기록 12:05 · 점수 2340\n스도쿠 앱을 열고 아래 코드를 붙여 넣어 같은 퍼즐에 도전하세요:\nSDK1-AQEBAAlkIBIJAIAwGAADcAAANwkAADCAQAAIBCAAAGQABXBAgAEGgBaVAAAAAtUBAAAJJM9R",
        )
    }

    @Test fun homeWithReceiveButton() = shoot("32_home_receive_challenge_button") {
        HomeContent(ResumeInfo(Difficulty.MEDIUM, 725_000, 47), DailyInfo(DailyStatus.NOT_STARTED, 0, 2), {}, {}, {})
    }

    /** 글자 크기 200%(접근성 최대 수준)에서도 내용이 잘리거나 겹치지 않는지 확인용. */
    @Test @Config(qualifiers = "ko-w390dp-h844dp-xxhdpi", fontScale = 2.0f) fun gameFontScale2() { val (ui, s) = gameUi(); shoot("33_game_fontscale_200") { Game(ui, s) } }

    @Test @Config(qualifiers = "ko-w390dp-h1400dp-xxhdpi", fontScale = 2.0f) fun homeFontScale2() = shoot("34_home_fontscale_200") {
        HomeContent(ResumeInfo(Difficulty.MEDIUM, 725_000, 47), DailyInfo(DailyStatus.IN_PROGRESS, 40, 6), {}, {}, {})
    }

    @Test @Config(qualifiers = "ko-w390dp-h1400dp-xxhdpi", fontScale = 2.0f) fun premiumFontScale2() = shoot("35_premium_fontscale_200") {
        PremiumContent(false, false, "₩3,900", true, null, {}, {}, {}, {})
    }

    /** 틀린 칸 없이 중간쯤 진행한 게임에서 힌트 단계 [step]을 보여 준다. */
    private fun hintUi(d: Difficulty, wantHidden: Boolean, step: Int): Pair<GameUiState, GameState> {
        var seed = 1L
        while (true) {
            val p = PuzzleGenerator().generate(d, seed++)
            var s = GameRules.newState(p.givens, p.solution)
            s.values.indices.filter { s.values[it] == 0 }.take(if (wantHidden) 6 else 12).forEach { s = GameRules.enter(s, it, s.solution[it], false, true).first }
            val h = com.uk2solutions.sudoku.domain.game.Hints.next(s) ?: continue
            if (wantHidden && h.technique != com.uk2solutions.sudoku.domain.game.HintTechnique.HIDDEN_SINGLE) continue
            val plan = com.uk2solutions.sudoku.domain.game.Hints.plan(s, h)
            return GameUiState(loading = false, state = s, difficulty = d, selected = h.index, hint = h, hintPlan = plan, hintStep = step.coerceAtMost(plan.steps.lastIndex)) to s
        }
    }

    @Test fun hintNaked1() { val (ui, s) = hintUi(Difficulty.EASY, false, 0); shoot("36_hint_naked_step1") { Game(ui, s) } }
    @Test fun hintNaked2() { val (ui, s) = hintUi(Difficulty.EASY, false, 1); shoot("37_hint_naked_step2_eliminated_digits") { Game(ui, s) } }
    @Test fun hintNaked3() { val (ui, s) = hintUi(Difficulty.EASY, false, 2); shoot("38_hint_naked_step3_answer") { Game(ui, s) } }
    @Test fun hintHidden1() { val (ui, s) = hintUi(Difficulty.MEDIUM, true, 0); shoot("39_hint_hidden_step1") { Game(ui, s) } }
    @Test fun hintHidden2Dark() { val (ui, s) = hintUi(Difficulty.MEDIUM, true, 1); shoot("40_hint_hidden_step2_blockers_dark", dark = true) { Game(ui, s) } }

    // ---------------------------------------------------------------- 트레이닝
    private fun lessonUi(id: String, phase: com.uk2solutions.sudoku.ui.training.LessonPhase, demoStep: Int = 0, qIndex: Int = 0, answered: Boolean = false, picked: Set<Int> = emptySet(), correct: Boolean = false): com.uk2solutions.sudoku.ui.training.LessonUi {
        val l = com.uk2solutions.sudoku.domain.training.Lessons.byId(id)!!
        val demo = com.uk2solutions.sudoku.domain.training.TrainingExamples.find(l, 0)
        val quiz = (1..3).mapNotNull { com.uk2solutions.sudoku.domain.training.TrainingExamples.find(l, it) }
        val ex = quiz.getOrNull(qIndex)
        // 정답 칸을 골랐다고 가정(correct=true) 또는 일부만 고른 상태(correct=false)
        val pk = if (picked.isNotEmpty()) picked else if (answered && ex != null) (if (correct) ex.expectedCells else ex.expectedCells.take(1).toSet()) else emptySet()
        return com.uk2solutions.sudoku.ui.training.LessonUi(
            lesson = l, loading = false, phase = phase, demo = demo, demoStep = demoStep, quiz = quiz, qIndex = qIndex,
            picked = pk, answered = answered, correct = correct, chosenDigit = 0, score = 2, nextLessonId = "x",
        )
    }

    @Composable private fun Lesson(ui: com.uk2solutions.sudoku.ui.training.LessonUi) =
        com.uk2solutions.sudoku.ui.training.LessonBody(ui, {}, {}, {}, {}, {}, {}, {}, {}, {}, {})

    @Test fun trainingHub() = shoot("41_training_hub") {
        com.uk2solutions.sudoku.ui.training.TrainingContent(
            mapOf("rules" to 3, "naked_single" to 3, "hidden_single" to 2, "pointing" to 1), premium = false, onOpen = {}, onPremium = {},
        )
    }

    @Test fun trainingHubPremium() = shoot("42_training_hub_premium_dark", dark = true) {
        com.uk2solutions.sudoku.ui.training.TrainingContent(
            mapOf("rules" to 3, "naked_single" to 3, "hidden_single" to 3, "pointing" to 3, "claiming" to 2, "naked_pair" to 3, "hidden_pair" to 1), premium = true, onOpen = {}, onPremium = {},
        )
    }

    @Test fun lessonIntro() { val ui = lessonUi("hidden_single", com.uk2solutions.sudoku.ui.training.LessonPhase.INTRO); shoot("43_lesson_intro") { Lesson(ui) } }
    @Test fun lessonNakedSingleDemo0() { val ui = lessonUi("naked_single", com.uk2solutions.sudoku.ui.training.LessonPhase.DEMO, 0); shoot("44_lesson_naked_single_demo1") { Lesson(ui) } }
    @Test fun lessonNakedSingleQuiz() { val ui = lessonUi("naked_single", com.uk2solutions.sudoku.ui.training.LessonPhase.QUIZ); shoot("45_lesson_naked_single_quiz") { Lesson(ui) } }
    @Test fun lessonHiddenSingleDemo1() { val ui = lessonUi("hidden_single", com.uk2solutions.sudoku.ui.training.LessonPhase.DEMO, 1); shoot("46_lesson_hidden_single_demo2") { Lesson(ui) } }
    @Test fun lessonPointingDemo0() { val ui = lessonUi("pointing", com.uk2solutions.sudoku.ui.training.LessonPhase.DEMO, 0); shoot("47_lesson_pointing_demo1_pattern") { Lesson(ui) } }
    @Test fun lessonPointingDemo1() { val ui = lessonUi("pointing", com.uk2solutions.sudoku.ui.training.LessonPhase.DEMO, 1); shoot("48_lesson_pointing_demo2_remove") { Lesson(ui) } }
    @Test fun lessonXWingDemo1() { val ui = lessonUi("x_wing", com.uk2solutions.sudoku.ui.training.LessonPhase.DEMO, 1); shoot("49_lesson_xwing_demo2") { Lesson(ui) } }
    @Test fun lessonYWingQuiz() { val ui = lessonUi("y_wing", com.uk2solutions.sudoku.ui.training.LessonPhase.QUIZ); shoot("50_lesson_ywing_quiz") { Lesson(ui) } }
    @Test fun lessonNakedPairWrong() { val ui = lessonUi("naked_pair", com.uk2solutions.sudoku.ui.training.LessonPhase.QUIZ, answered = true, correct = false); shoot("51_lesson_naked_pair_wrong_feedback", dark = true) { Lesson(ui) } }
    @Test fun lessonResult() { val ui = lessonUi("naked_pair", com.uk2solutions.sudoku.ui.training.LessonPhase.RESULT); shoot("52_lesson_result") { Lesson(ui) } }

    // ---------------------------------------------------------------- 후킹 장치
    @Test fun hubRankHook() = shoot("53_training_rank_premium_hook") {
        com.uk2solutions.sudoku.ui.training.TrainingContent(
            mapOf("rules" to 3, "naked_single" to 3, "hidden_single" to 3, "pointing" to 2, "claiming" to 3, "naked_pair" to 2), premium = false, onOpen = {}, onPremium = {},
        )
    }

    @Test fun lessonTeaserPattern() {
        val ui = lessonUi("x_wing", com.uk2solutions.sudoku.ui.training.LessonPhase.DEMO, 0).copy(locked = true)
        shoot("54_lesson_teaser_xwing_free_preview") { Lesson(ui) }
    }

    @Test fun lessonPaywall() {
        val ui = lessonUi("x_wing", com.uk2solutions.sudoku.ui.training.LessonPhase.PAYWALL).copy(locked = true)
        shoot("55_lesson_paywall") { Lesson(ui) }
    }

    @Test fun hintLearnOnExpert() {
        var seed = 1L
        while (true) {
            val p = PuzzleGenerator().generate(Difficulty.EXPERT, seed++)
            var s = GameRules.newState(p.givens, p.solution)
            var h = com.uk2solutions.sudoku.domain.game.Hints.next(s)
            while (h != null && h.technique != com.uk2solutions.sudoku.domain.game.HintTechnique.DEDUCTION) {
                s = GameRules.enter(s, h.index, h.digit, false, true).first
                h = com.uk2solutions.sudoku.domain.game.Hints.next(s)
            }
            if (h?.learn == null) continue
            val plan = com.uk2solutions.sudoku.domain.game.Hints.plan(s, h)
            val ui = GameUiState(loading = false, state = s, difficulty = Difficulty.EXPERT, selected = h.index, hint = h, hintPlan = plan, hintStep = 0)
            shoot("56_hint_learn_technique_expert") { Game(ui, s) }
            return
        }
    }

    @Test fun homeWithExpert() = shoot("57_home_expert_open_devil_locked") {
        HomeContent(ResumeInfo(Difficulty.EXPERT, 1_203_000, 22), DailyInfo(DailyStatus.NOT_STARTED, 0, 3), {}, {}, {})
    }
}
