package com.uk2solutions.sudoku.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uk2solutions.sudoku.data.AppSettings
import com.uk2solutions.sudoku.data.GameEntity
import com.uk2solutions.sudoku.data.GameRepository
import com.uk2solutions.sudoku.data.SettingsRepository
import com.uk2solutions.sudoku.domain.game.Completion
import com.uk2solutions.sudoku.domain.game.Completions
import com.uk2solutions.sudoku.domain.game.GameRules
import com.uk2solutions.sudoku.domain.game.Hint
import com.uk2solutions.sudoku.domain.game.HintPolicy
import com.uk2solutions.sudoku.domain.game.Hints
import com.uk2solutions.sudoku.domain.game.Scoring
import com.uk2solutions.sudoku.domain.game.GameState
import com.uk2solutions.sudoku.domain.game.InputResult
import com.uk2solutions.sudoku.domain.model.Difficulty
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class GameUiState(
    val loading: Boolean = true,
    val missing: Boolean = false,
    val state: GameState? = null,
    val difficulty: Difficulty = Difficulty.EASY,
    val daily: Boolean = false,
    val selectedDigit: Int = 0,
    val selected: Int = -1,
    val noteMode: Boolean = false,
    val paused: Boolean = false,
    val completed: Boolean = false,
    val mistakeLimitReached: Boolean = false,
    val conflicts: Set<Int> = emptySet(),
    val settings: AppSettings = AppSettings(),
    val hint: Hint? = null,
    /** 힌트 단계별 설명(보드 스포트라이트 + 하단 카드)과 현재 단계. */
    val hintPlan: com.uk2solutions.sudoku.domain.game.HintPlan? = null,
    val hintStep: Int = 0,
    val hintsExhausted: Boolean = false,
    val effect: Effect? = null,
    val scoreGain: ScoreGain? = null,
    val premium: Boolean = false,
    /** 도전장으로 시작한 게임이면 상대의 결과. */
    val opponent: com.uk2solutions.sudoku.domain.challenge.ChallengeResult? = null,
    /** 틀린 숫자가 남은 채 입력하려 할 때마다 1씩 늘어 화면이 안내를 강조한다. */
    val blockedNudge: Int = 0,
)

/** 칸을 채운 자리에 잠깐 떠오르는 점수 변화(+10, -30 ...). */
data class ScoreGain(val id: Int, val index: Int, val delta: Int)

/** 완성 연출. [id]가 바뀔 때마다 한 번 재생. */
data class Effect(val id: Int, val cells: Set<Int>, val digit: Int?)

@OptIn(FlowPreview::class)
@HiltViewModel
class GameViewModel @Inject constructor(
    private val handle: SavedStateHandle,
    private val repo: GameRepository,
    private val settingsRepo: SettingsRepository,
    private val premium: com.uk2solutions.sudoku.billing.PremiumManager,
) : ViewModel() {
    private val _ui = MutableStateFlow(GameUiState())
    val ui: StateFlow<GameUiState> = _ui.asStateFlow()

    /** 타이머는 보드 전체가 다시 그려지지 않도록 별도 흐름으로 둔다. */
    private val _elapsed = MutableStateFlow(0L)
    val elapsedMs: StateFlow<Long> = _elapsed.asStateFlow()

    private var gameId: Long = handle["gameId"] ?: -1L
    private var limitDismissed = false
    private val saveRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        viewModelScope.launch { premium.isPremium.collect { p -> _ui.update { it.copy(premium = p) } } }
        viewModelScope.launch { settingsRepo.settings.collect { s -> _ui.update { it.copy(settings = s) } } }
        viewModelScope.launch { load(handle.get<String>("difficulty")) }
        // SPEC §5.5: 이동 후 500ms 디바운스 자동 저장
        viewModelScope.launch { saveRequests.debounce(500).collect { saveNow() } }
        viewModelScope.launch { tick() }
    }

    private suspend fun load(difficultyArg: String?) {
        // 프로세스 종료 후 복원돼도 새 게임을 또 만들지 않도록 gameId를 SavedStateHandle에 보관
        val id = when {
            gameId > 0 -> gameId
            difficultyArg == "DAILY" -> repo.startOrGetDaily(java.time.LocalDate.now())
            difficultyArg != null -> repo.startNew(Difficulty.valueOf(difficultyArg))
            else -> repo.resumableId()
        }
        val loaded = id?.let { repo.load(it) }
        if (id == null || loaded == null || loaded.status != GameEntity.STATUS_IN_PROGRESS) {
            _ui.update { it.copy(loading = false, missing = true) }
            return
        }
        gameId = id
        handle["gameId"] = id
        _elapsed.value = loaded.state.elapsedMs
        limitDismissed = false
        _ui.update { cur ->
            cur.copy(
                loading = false, state = loaded.state, difficulty = loaded.difficulty, daily = loaded.daily, opponent = loaded.opponent,
                conflicts = GameRules.conflicts(loaded.state.values),
                mistakeLimitReached = limitReached(loaded.state, cur.settings),
            )
        }
    }

    private suspend fun tick() {
        var last = System.nanoTime()
        while (true) {
            delay(250)
            val now = System.nanoTime()
            val dt = (now - last) / 1_000_000
            last = now
            val u = _ui.value
            if (u.state != null && !u.paused && !u.completed && !u.mistakeLimitReached) _elapsed.update { it + dt }
        }
    }

    private fun limitReached(s: GameState, st: AppSettings) =
        st.mistakeLimitEnabled && !limitDismissed && s.mistakes >= st.mistakeLimit && !GameRules.isComplete(s)

    /** 숫자 우선 모드에서 숫자를 고른 상태라면 칸을 누르는 즉시 입력한다. */
    fun onCell(i: Int): InputResult {
        val u = _ui.value
        val s = u.state ?: return InputResult.NONE
        if (u.paused || u.completed || u.mistakeLimitReached) return InputResult.NONE
        if (u.settings.numberFirst && u.selectedDigit > 0) {
            if (blockedByWrong(s)) { _ui.update { it.copy(selected = i) }; return InputResult.NONE }
            _ui.update { it.copy(selected = i) }
            val (next, r) = GameRules.enter(s, i, u.selectedDigit, u.noteMode, u.settings.autoRemoveNotes)
            if (r != InputResult.NONE) { apply(next, i, s); if (r == InputResult.MISTAKE) _ui.update { it.copy(selected = i) } }
            return r
        }
        _ui.update { it.copy(selected = if (it.selected == i) -1 else i) }
        return InputResult.NONE
    }

    fun onToggleNote() = _ui.update { it.copy(noteMode = !it.noteMode) }

    fun onDigit(d: Int): InputResult {
        val u = _ui.value
        val s = u.state ?: return InputResult.NONE
        if (u.paused || u.completed || u.mistakeLimitReached) return InputResult.NONE
        if (u.settings.numberFirst) {
            _ui.update { it.copy(selectedDigit = if (it.selectedDigit == d) 0 else d) }
            return InputResult.NONE
        }
        if (u.selected < 0) return InputResult.NONE
        if (blockedByWrong(s)) return InputResult.NONE
        val (next, r) = GameRules.enter(s, u.selected, d, u.noteMode, u.settings.autoRemoveNotes)
        if (r != InputResult.NONE) apply(next, u.selected, s)
        return r
    }

    fun onErase(): InputResult {
        val u = _ui.value
        val s = u.state ?: return InputResult.NONE
        if (blocked(u)) return InputResult.NONE
        val (next, r) = GameRules.erase(s, u.selected)
        if (r != InputResult.NONE) apply(next)
        return r
    }

    fun onUndo() {
        val u = _ui.value
        val s = u.state ?: return
        if (u.paused || u.completed || u.mistakeLimitReached || s.undo.isEmpty()) return
        apply(GameRules.undo(s))
    }

    /** 틀린 숫자가 있으면 지울 때까지 새 입력·힌트를 막는다. 막았으면 true. */
    private fun blockedByWrong(s: GameState): Boolean {
        if (GameRules.wrongCells(s).isEmpty()) return false
        _ui.update { it.copy(blockedNudge = it.blockedNudge + 1) }
        return true
    }

    /** 틀린 숫자를 취소(지우기)해 빈칸으로 만든다. */
    fun onCancelWrong(): InputResult {
        val u = _ui.value
        val s = u.state ?: return InputResult.NONE
        if (u.paused || u.completed) return InputResult.NONE
        val i = GameRules.wrongCells(s).firstOrNull() ?: return InputResult.NONE
        val (next, r) = GameRules.erase(s, i)
        if (r != InputResult.NONE) { apply(next); _ui.update { it.copy(selected = i) } }
        return r
    }

    private fun blocked(u: GameUiState) = u.selected < 0 || u.paused || u.completed || u.mistakeLimitReached

    private var effectId = 0
    private var gainId = 0

    private fun apply(next: GameState, changed: Int = -1, prev: GameState? = null) {
        val done = GameRules.isComplete(next)
        val c: Completion? =
            if (changed >= 0 && prev != null && _ui.value.settings.completeAnimation) Completions.between(prev, next, changed) else null
        val gain: ScoreGain? = if (changed >= 0 && prev != null && _ui.value.settings.scoreAnimation) {
            val diff = _ui.value.difficulty
            val d = Scoring.score(next, diff) - Scoring.score(prev, diff)
            if (d != 0) ScoreGain(++gainId, changed, d) else null
        } else null
        _ui.update {
            it.copy(
                scoreGain = gain ?: it.scoreGain,
                selectedDigit = nextDigit(it, next),
                state = next, hint = null, hintPlan = null, hintStep = 0, hintsExhausted = false,
                effect = c?.let { x -> Effect(++effectId, x.cells, x.digit) } ?: it.effect, conflicts = GameRules.conflicts(next.values), completed = done,
                mistakeLimitReached = limitReached(next, it.settings),
            )
        }
        if (done) viewModelScope.launch { withContext(NonCancellable) { saveNow(); repo.complete(gameId) } }
        else saveRequests.tryEmit(Unit)
    }

    /** 힌트 시작(하루 한도 1회 소모): 풀 수 있는 칸을 찾아 단계별 설명을 연다. 이미 열려 있으면 무시. */
    fun onHint() {
        val u = _ui.value
        val s = u.state ?: return
        if (u.paused || u.completed || u.mistakeLimitReached || u.hintPlan != null) return
        if (blockedByWrong(s)) return
        viewModelScope.launch {
            if (settingsRepo.consumeHint(HintPolicy.FREE_PER_DAY, unlimited = u.premium)) {
                val h = Hints.next(s, u.selected) ?: return@launch
                _ui.update { it.copy(hint = h, hintPlan = Hints.plan(s, h), hintStep = 0, selected = h.index, hintsExhausted = false) }
            } else _ui.update { it.copy(hintsExhausted = true) }
        }
    }

    fun onHintNext() = _ui.update { u ->
        val last = (u.hintPlan?.steps?.size ?: 1) - 1
        u.copy(hintStep = (u.hintStep + 1).coerceAtMost(last))
    }

    /** 마지막 단계에서 정답을 채운다(힌트로 채운 칸으로 기록). */
    fun onHintApply() {
        val u = _ui.value
        val s = u.state ?: return
        val h = u.hint ?: return
        val (next, _) = GameRules.enter(s, h.index, h.digit, false, u.settings.autoRemoveNotes)
        apply(next.copy(hintsUsed = next.hintsUsed + 1), h.index, s)
        _ui.update { it.copy(selected = h.index) }
    }

    fun onHintClose() = _ui.update { it.copy(hint = null, hintPlan = null, hintStep = 0) }

    /** 숫자 우선 모드: 고른 숫자를 다 놓았으면(남은 0) 남은 개수가 있는 다음 숫자로 자동 이동. */
    private fun nextDigit(u: GameUiState, s: GameState): Int {
        val cur = u.selectedDigit
        if (cur == 0 || !u.settings.numberFirst || !u.settings.autoNextNumber || GameRules.remaining(s, cur) > 0) return cur
        for (k in 1..9) {
            val d = (cur - 1 + k) % 9 + 1
            if (GameRules.remaining(s, d) > 0) return d
        }
        return 0
    }

    fun continueWithoutLimit() {
        limitDismissed = true
        _ui.update { it.copy(mistakeLimitReached = false) }
    }

    fun pause() = _ui.update { if (it.completed) it else it.copy(paused = true) }

    fun resume() = _ui.update { it.copy(paused = false) }

    /** 백그라운드 진입: 타이머 정지(백그라운드 시간 제외) + 즉시 저장. */
    fun onStop() {
        pause()
        viewModelScope.launch { withContext(NonCancellable) { saveNow() } }
    }

    suspend fun saveNow() {
        val s = _ui.value.state ?: return
        if (gameId <= 0) return
        repo.save(gameId, s.copy(elapsedMs = _elapsed.value))
    }
}
