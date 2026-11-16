package com.uk2solutions.sudoku.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uk2solutions.sudoku.data.GameEntity
import com.uk2solutions.sudoku.data.GameRepository
import com.uk2solutions.sudoku.data.GameRepository.Companion.toState
import com.uk2solutions.sudoku.domain.game.GameRules
import com.uk2solutions.sudoku.domain.game.Streak
import com.uk2solutions.sudoku.domain.model.Difficulty
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class ResumeInfo(val difficulty: Difficulty, val elapsedMs: Long, val percent: Int)
enum class DailyStatus { NOT_STARTED, IN_PROGRESS, DONE }
data class DailyInfo(val status: DailyStatus = DailyStatus.NOT_STARTED, val percent: Int = 0, val streak: Int = 0)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repo: GameRepository,
    premiumManager: com.uk2solutions.sudoku.billing.PremiumManager,
    private val inbox: com.uk2solutions.sudoku.data.ChallengeInbox,
) : ViewModel() {
    /** 다른 앱에서 공유로 넘어온 도전장 문구(있으면 홈이 받기 창을 연다). */
    val pendingChallenge: StateFlow<String?> = inbox.pending

    fun clearPendingChallenge() = inbox.clear()

    /** 도전장 문구(코드만 또는 공유 문구 전체)로 같은 퍼즐을 시작한다. 코드가 올바르지 않으면 false. */
    fun acceptChallenge(text: String, onStarted: () -> Unit, onInvalid: () -> Unit) {
        val c = com.uk2solutions.sudoku.domain.challenge.ChallengeCode.decode(text)
        if (c == null) { onInvalid(); return }
        viewModelScope.launch {
            repo.startChallenge(c)
            inbox.clear()
            onStarted()
        }
    }

    val premium: StateFlow<Boolean> = premiumManager.isPremium.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val resume: StateFlow<ResumeInfo?> = repo.observeResumable().map { e ->
        e?.let {
            val s = it.toState()
            ResumeInfo(Difficulty.valueOf(it.difficulty), s.elapsedMs, GameRules.progressPercent(s.values))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val today = LocalDate.now()

    val daily: StateFlow<DailyInfo> = kotlinx.coroutines.flow.combine(
        repo.observeDaily(today.toString()), repo.observeDailyCompleted(),
    ) { e, done ->
        val streak = Streak.current(done.map { LocalDate.parse(it) }.toSet(), today)
        when (e?.status) {
            GameEntity.STATUS_COMPLETED -> DailyInfo(DailyStatus.DONE, 100, streak)
            GameEntity.STATUS_IN_PROGRESS -> DailyInfo(DailyStatus.IN_PROGRESS, GameRules.progressPercent(e.toState().values), streak)
            else -> DailyInfo(DailyStatus.NOT_STARTED, 0, streak)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DailyInfo())
}
