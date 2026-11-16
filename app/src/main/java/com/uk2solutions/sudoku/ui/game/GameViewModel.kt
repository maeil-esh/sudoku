package com.uk2solutions.sudoku.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uk2solutions.sudoku.data.AppSettings
import com.uk2solutions.sudoku.data.GameEntity
import com.uk2solutions.sudoku.data.GameRepository
import com.uk2solutions.sudoku.data.SettingsRepository
import com.uk2solutions.sudoku.domain.game.GameRules
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
    val selected: Int = -1,
    val noteMode: Boolean = false,
    val paused: Boolean = false,
    val completed: Boolean = false,
    val mistakeLimitReached: Boolean = false,
    val conflicts: Set<Int> = emptySet(),
    val settings: AppSettings = AppSettings(),
)

@OptIn(FlowPreview::class)
@HiltViewModel
class GameViewModel @Inject constructor(
    private val handle: SavedStateHandle,
    private val repo: GameRepository,
    settingsRepo: SettingsRepository,
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
                loading = false, state = loaded.state, difficulty = loaded.difficulty,
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

    fun onCell(i: Int) = _ui.update { it.copy(selected = if (it.selected == i) -1 else i) }

    fun onToggleNote() = _ui.update { it.copy(noteMode = !it.noteMode) }

    fun onDigit(d: Int): InputResult {
        val u = _ui.value
        val s = u.state ?: return InputResult.NONE
        if (blocked(u)) return InputResult.NONE
        val (next, r) = GameRules.enter(s, u.selected, d, u.noteMode, u.settings.autoRemoveNotes)
        if (r != InputResult.NONE) apply(next)
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

    private fun blocked(u: GameUiState) = u.selected < 0 || u.paused || u.completed || u.mistakeLimitReached

    private fun apply(next: GameState) {
        val done = GameRules.isComplete(next)
        _ui.update {
            it.copy(
                state = next, conflicts = GameRules.conflicts(next.values), completed = done,
                mistakeLimitReached = limitReached(next, it.settings),
            )
        }
        if (done) viewModelScope.launch { withContext(NonCancellable) { saveNow(); repo.complete(gameId) } }
        else saveRequests.tryEmit(Unit)
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
