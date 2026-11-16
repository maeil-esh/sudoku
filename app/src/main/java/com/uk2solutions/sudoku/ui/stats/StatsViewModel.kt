package com.uk2solutions.sudoku.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uk2solutions.sudoku.billing.PremiumManager
import com.uk2solutions.sudoku.data.DifficultyStat
import com.uk2solutions.sudoku.data.GameRepository
import com.uk2solutions.sudoku.domain.game.Streak
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class StatsUi(
    val byDifficulty: Map<String, DifficultyStat> = emptyMap(),
    val streak: Int = 0,
    /** 최근 7일(오래된 날 → 오늘) 완료 수. 상세 통계(프리미엄). */
    val weekly: List<Int> = List(7) { 0 },
    val premium: Boolean = false,
) {
    val totalDone get() = byDifficulty.values.sumOf { it.cnt }
    val bestMs get() = byDifficulty.values.minOfOrNull { it.best }
}

/** 완료 시각 목록에서 최근 7일 일별 완료 수를 센다. */
fun weeklyCounts(finishedAt: List<Long>, today: LocalDate, zone: ZoneId = ZoneId.systemDefault()): List<Int> {
    val counts = IntArray(7)
    for (t in finishedAt) {
        val ago = java.time.temporal.ChronoUnit.DAYS.between(Instant.ofEpochMilli(t).atZone(zone).toLocalDate(), today).toInt()
        if (ago in 0..6) counts[6 - ago]++
    }
    return counts.toList()
}

@HiltViewModel
class StatsViewModel @Inject constructor(repo: GameRepository, premium: PremiumManager) : ViewModel() {
    val ui: StateFlow<StatsUi> = combine(
        repo.observeStats(), repo.observeDailyCompleted(), repo.observeFinishedAt(), premium.isPremium,
    ) { stats, days, finished, isPremium ->
        val today = LocalDate.now()
        StatsUi(
            stats.associateBy { it.difficulty },
            Streak.current(days.map { LocalDate.parse(it) }.toSet(), today),
            weeklyCounts(finished, today), isPremium,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUi())
}
