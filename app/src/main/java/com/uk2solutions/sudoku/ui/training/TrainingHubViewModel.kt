package com.uk2solutions.sudoku.ui.training

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uk2solutions.sudoku.billing.PremiumManager
import com.uk2solutions.sudoku.data.TrainingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class TrainingHubViewModel @Inject constructor(repo: TrainingRepository, premiumManager: PremiumManager) : ViewModel() {
    val scores: StateFlow<Map<String, Int>> = repo.scores.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    val premium: StateFlow<Boolean> = premiumManager.isPremium.stateIn(viewModelScope, SharingStarted.Eagerly, false)
}
