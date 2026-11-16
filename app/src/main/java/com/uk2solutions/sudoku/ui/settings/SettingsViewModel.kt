package com.uk2solutions.sudoku.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uk2solutions.sudoku.data.AppSettings
import com.uk2solutions.sudoku.data.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val repo: SettingsRepository, premiumManager: com.uk2solutions.sudoku.billing.PremiumManager) : ViewModel() {
    val premium: StateFlow<Boolean> = premiumManager.isPremium.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val settings: StateFlow<AppSettings> = repo.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun update(block: (AppSettings) -> AppSettings) { viewModelScope.launch { repo.update(block) } }
}
