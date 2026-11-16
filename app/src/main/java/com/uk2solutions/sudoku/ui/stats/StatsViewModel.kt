package com.uk2solutions.sudoku.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uk2solutions.sudoku.data.DifficultyStat
import com.uk2solutions.sudoku.data.GameRepository
import com.uk2solutions.sudoku.domain.game.Streak
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import javax.inject.Inject

data class StatsUi(val byDifficulty: Map<String, DifficultyStat> = emptyMap(), val streak: Int = 0) {
    val totalDone get() = byDifficulty.values.sumOf { it.cnt }
    val bestMs get() = byDifficulty.values.minOfOrNull { it.best }
}

@HiltViewModel
class StatsViewModel @Inject constructor(repo: GameRepository) : ViewModel() {
    val ui: StateFlow<StatsUi> = combine(repo.observeStats(), repo.observeDailyCompleted()) { stats, days ->
        StatsUi(stats.associateBy { it.difficulty }, Streak.current(days.map { LocalDate.parse(it) }.toSet(), LocalDate.now()))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StatsUi())
}
