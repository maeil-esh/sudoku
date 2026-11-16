package com.uk2solutions.sudoku.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uk2solutions.sudoku.data.GameRepository
import com.uk2solutions.sudoku.data.GameRepository.Companion.toState
import com.uk2solutions.sudoku.domain.game.GameRules
import com.uk2solutions.sudoku.domain.model.Difficulty
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ResumeInfo(val difficulty: Difficulty, val elapsedMs: Long, val percent: Int)

@HiltViewModel
class HomeViewModel @Inject constructor(repo: GameRepository) : ViewModel() {
    val resume: StateFlow<ResumeInfo?> = repo.observeResumable().map { e ->
        e?.let {
            val s = it.toState()
            ResumeInfo(Difficulty.valueOf(it.difficulty), s.elapsedMs, GameRules.progressPercent(s.values))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
