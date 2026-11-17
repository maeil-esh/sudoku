package com.uk2solutions.sudoku.a11y

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.uk2solutions.sudoku.data.AppSettings
import com.uk2solutions.sudoku.data.DifficultyStat
import com.uk2solutions.sudoku.domain.game.GameRules
import com.uk2solutions.sudoku.domain.generator.PuzzleGenerator
import com.uk2solutions.sudoku.domain.model.Difficulty
import com.uk2solutions.sudoku.R
import com.uk2solutions.sudoku.ui.challenge.CompleteCard
import com.uk2solutions.sudoku.ui.challenge.ReceiveCard
import com.uk2solutions.sudoku.domain.challenge.ChallengeResult
import com.uk2solutions.sudoku.ui.game.GameBody
import com.uk2solutions.sudoku.ui.game.GameUiState
import com.uk2solutions.sudoku.ui.home.DailyInfo
import com.uk2solutions.sudoku.ui.home.DailyStatus
import com.uk2solutions.sudoku.ui.home.HomeContent
import com.uk2solutions.sudoku.ui.home.ResumeInfo
import com.uk2solutions.sudoku.ui.premium.PremiumContent
import com.uk2solutions.sudoku.ui.settings.SettingsBody
import com.uk2solutions.sudoku.ui.stats.StatsContent
import com.uk2solutions.sudoku.ui.stats.StatsUi
import com.uk2solutions.sudoku.ui.theme.SudokuTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** TalkBack이 보는 병합된 의미 트리에서, 누를 수 있는 모든 요소(버튼·스위치)에 읽을 이름이 있는지 확인한다. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class, qualifiers = "ko-w390dp-h844dp-xxhdpi")
class SemanticsTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

    private fun assertAllLabeled(name: String, content: @Composable () -> Unit): Int {
        rule.setContent { SudokuTheme { content() } }
        rule.waitForIdle()
        val interactive = hasClickAction() or SemanticsMatcher.keyIsDefined(SemanticsProperties.ToggleableState)
        val nodes = rule.onAllNodes(interactive).fetchSemanticsNodes()
        assertTrue("$name: 조작 요소가 없음", nodes.isNotEmpty())
        val unlabeled = nodes.filter { n ->
            val cd = n.config.getOrNull(SemanticsProperties.ContentDescription)?.any { it.isNotBlank() } == true
            val tx = n.config.getOrNull(SemanticsProperties.Text)?.any { it.text.isNotBlank() } == true
            !(cd || tx)
        }
        assertTrue("$name: 이름 없는 조작 요소 ${unlabeled.size}개", unlabeled.isEmpty())
        return nodes.size
    }

    @Test fun game() {
        val p = PuzzleGenerator().generate(Difficulty.MEDIUM, 7L)
        val s = GameRules.newState(p.givens, p.solution)
        val n = assertAllLabeled("게임") {
            GameBody(GameUiState(loading = false, state = s, difficulty = Difficulty.MEDIUM, selected = 40), s, 1000, {}, {}, {}, {}, {}, {}, {}, {}, {})
        }
        assertTrue("칸 81개 + 도구·패드·상단 버튼 (실제 $n)", n >= 81 + 4 + 9)
    }

    @Test fun home() { assertAllLabeled("홈") { HomeContent(ResumeInfo(Difficulty.EASY, 1000, 10), DailyInfo(DailyStatus.NOT_STARTED, 0, 1), {}, {}, {}) } }

    @Test fun stats() {
        assertAllLabeled("기록") {
            StatsContent(StatsUi(mapOf("EASY" to DifficultyStat("EASY", 1, 1000, 1000.0)), 1))
        }
    }

    @Test fun settings() {
        assertAllLabeled("설정") {
            SettingsBody(AppSettings(), "", false, listOf(R.string.theme_system, R.string.theme_light, R.string.theme_dark), {}, {}, {}, {}, {})
        }
    }

    @Test fun premium() { assertAllLabeled("프리미엄") { PremiumContent(false, false, "₩4,900", true, null, {}, {}, {}, {}) } }

    @Test fun challengeResult() {
        assertAllLabeled("도전 결과") { CompleteCard(1000, 0, 100, ChallengeResult(10, 1, 0, 90), {}, {}, {}) }
    }

    @Test fun trainingHub() {
        assertAllLabeled("트레이닝 목록") { com.uk2solutions.sudoku.ui.training.TrainingContent(mapOf("rules" to 3, "naked_single" to 1), false, {}, {}) }
    }

    @Test fun lessonQuiz() {
        val l = com.uk2solutions.sudoku.domain.training.Lessons.byId("naked_single")!!
        val quiz = (1..3).mapNotNull { com.uk2solutions.sudoku.domain.training.TrainingExamples.find(l, it) }
        val ui = com.uk2solutions.sudoku.ui.training.LessonUi(lesson = l, loading = false, phase = com.uk2solutions.sudoku.ui.training.LessonPhase.QUIZ, quiz = quiz)
        val n = assertAllLabeled("레슨 문제") { com.uk2solutions.sudoku.ui.training.LessonBody(ui, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}) }
        assertTrue("칸 81개 + 숫자 9개 + 뒤로", n >= 81 + 9)
    }

    @Test fun challengeReceive() { assertAllLabeled("도전장 받기") { ReceiveCard("", false, {}, {}) } }
}
