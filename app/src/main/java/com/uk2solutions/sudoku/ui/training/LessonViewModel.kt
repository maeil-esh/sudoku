package com.uk2solutions.sudoku.ui.training

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uk2solutions.sudoku.billing.PremiumManager
import com.uk2solutions.sudoku.data.TrainingRepository
import com.uk2solutions.sudoku.domain.training.Example
import com.uk2solutions.sudoku.domain.training.Lesson
import com.uk2solutions.sudoku.domain.training.Lessons
import com.uk2solutions.sudoku.domain.training.QuizGrader
import com.uk2solutions.sudoku.domain.training.QuizKind
import com.uk2solutions.sudoku.domain.training.TrainingExamples
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class LessonPhase { INTRO, DEMO, QUIZ, PAYWALL, RESULT }

const val QUIZ_COUNT = 3

data class LessonUi(
    val lesson: Lesson? = null,
    val loading: Boolean = true,
    val failed: Boolean = false,
    val phase: LessonPhase = LessonPhase.INTRO,
    val demo: Example? = null,
    val demoStep: Int = 0,
    val quiz: List<Example> = emptyList(),
    val qIndex: Int = 0,
    /** 칸 고르기/지울 칸 고르기에서 고른 칸. */
    val picked: Set<Int> = emptySet(),
    val answered: Boolean = false,
    val correct: Boolean = false,
    val chosenDigit: Int = 0,
    val score: Int = 0,
    val nextLessonId: String? = null,
    val locked: Boolean = false,
)

@HiltViewModel
class LessonViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val repo: TrainingRepository,
    premiumManager: PremiumManager,
) : ViewModel() {
    private val lesson: Lesson? = Lessons.byId(handle.get<String>("id") ?: "")
    private val _ui = MutableStateFlow(LessonUi(lesson = lesson, nextLessonId = lesson?.let { Lessons.next(it.id)?.id }))
    val ui: StateFlow<LessonUi> = _ui.asStateFlow()
    val premium: StateFlow<Boolean> = premiumManager.isPremium.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        // 프리미엄 상태를 따라 잠금을 갱신한다. 맛보기 도중 구매하면 막힌 지점부터 이어서 배운다.
        viewModelScope.launch {
            this@LessonViewModel.premium.collect { p ->
                _ui.update {
                    val locked = lesson?.premium == true && !p
                    if (!locked && it.phase == LessonPhase.PAYWALL) it.copy(locked = false, phase = LessonPhase.DEMO, demoStep = 1) else it.copy(locked = locked)
                }
            }
        }
        viewModelScope.launch {
            val l = lesson
            if (l == null) { _ui.update { it.copy(loading = false, failed = true) }; return@launch }
            // 예제 탐색은 무거울 수 있어(스워드피시 등) 백그라운드에서 한다.
            val (demo, quiz) = withContext(Dispatchers.Default) {
                TrainingExamples.find(l, 0) to (1..QUIZ_COUNT).mapNotNull { TrainingExamples.find(l, it) }
            }
            _ui.update { it.copy(loading = false, failed = demo == null || quiz.size < QUIZ_COUNT, demo = demo, quiz = quiz) }
        }
    }

    fun start() = _ui.update { it.copy(phase = LessonPhase.DEMO, demoStep = 0) }

    fun demoNext() = _ui.update {
        if (it.locked && it.demoStep == 0) it.copy(phase = LessonPhase.PAYWALL)      // 맛보기는 패턴 보기까지 무료
        else if (it.demoStep < 1) it.copy(demoStep = it.demoStep + 1) else it.copy(phase = LessonPhase.QUIZ, qIndex = 0, picked = emptySet(), answered = false, score = 0) }

    fun paywallLater() = _ui.update { it.copy(phase = LessonPhase.DEMO, demoStep = 0) }

    fun demoBack() = _ui.update { if (it.demoStep > 0) it.copy(demoStep = it.demoStep - 1) else it.copy(phase = LessonPhase.INTRO) }

    private fun current(u: LessonUi): Example? = u.quiz.getOrNull(u.qIndex)

    /** 숫자 고르기(단일 후보). */
    fun pickDigit(d: Int) = _ui.update { u ->
        val ex = current(u) ?: return@update u
        if (u.answered || u.lesson?.quiz != QuizKind.PICK_DIGIT) u
        else u.copy(chosenDigit = d, answered = true, correct = QuizGrader.digit(ex, d), score = u.score + if (QuizGrader.digit(ex, d)) 1 else 0)
    }

    /** 칸 누르기: 숨은 단일은 즉시 채점, 지울 칸 고르기는 선택/해제. */
    fun tapCell(i: Int) = _ui.update { u ->
        val ex = current(u) ?: return@update u
        if (u.answered) return@update u
        when (u.lesson?.quiz) {
            QuizKind.PICK_CELL -> {
                val ok = QuizGrader.cell(ex, i)
                u.copy(picked = setOf(i), answered = true, correct = ok, score = u.score + if (ok) 1 else 0)
            }
            QuizKind.PICK_ELIMINATIONS -> u.copy(picked = if (i in u.picked) u.picked - i else u.picked + i)
            else -> u
        }
    }

    fun check() = _ui.update { u ->
        val ex = current(u) ?: return@update u
        if (u.answered || u.lesson?.quiz != QuizKind.PICK_ELIMINATIONS || u.picked.isEmpty()) u
        else { val ok = QuizGrader.eliminations(ex, u.picked); u.copy(answered = true, correct = ok, score = u.score + if (ok) 1 else 0) }
    }

    fun nextQuestion() {
        val u = _ui.value
        if (u.qIndex + 1 < u.quiz.size) {
            _ui.update { it.copy(qIndex = it.qIndex + 1, picked = emptySet(), answered = false, correct = false, chosenDigit = 0) }
        } else {
            _ui.update { it.copy(phase = LessonPhase.RESULT) }
            lesson?.let { l -> viewModelScope.launch { repo.record(l.id, u.score) } }
        }
    }

    fun retry() = _ui.update { it.copy(phase = LessonPhase.DEMO, demoStep = 0, qIndex = 0, picked = emptySet(), answered = false, correct = false, chosenDigit = 0, score = 0) }
}
