package com.uk2solutions.sudoku

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import com.uk2solutions.sudoku.notify.ReminderScheduler
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.uk2solutions.sudoku.billing.PremiumManager
import com.uk2solutions.sudoku.data.AppSettings
import com.uk2solutions.sudoku.domain.premium.Entitlement
import kotlinx.coroutines.launch
import com.uk2solutions.sudoku.data.SettingsRepository
import com.uk2solutions.sudoku.ui.SudokuNavHost
import com.uk2solutions.sudoku.ui.theme.SudokuTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var settingsRepo: SettingsRepository
    @Inject lateinit var premiumManager: PremiumManager

    /** 포그라운드 복귀마다 보유 구매를 조회한다(Play 스토어에서 먼저 코드를 상환한 경우도 여기서 반영, SPEC §7.2-1). */
    override fun onResume() {
        super.onResume()
        lifecycleScope.launch { premiumManager.refresh() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val settings by settingsRepo.settings.collectAsState(initial = AppSettings())
            val premium by premiumManager.isPremium.collectAsState(initial = false)
            val palette = if (Entitlement(premium).canUsePalette(settings.palette)) settings.palette else 0
            val dark = when (settings.themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }
            // 알림 설정이 바뀌거나 앱이 시작될 때 예약을 맞춘다(끄면 취소).
            LaunchedEffect(settings.notifyEnabled, settings.notifyHour) {
                if (settings.notifyEnabled) ReminderScheduler.schedule(applicationContext, settings.notifyHour)
                else ReminderScheduler.cancel(applicationContext)
            }
            DisposableEffect(dark) {
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }
            SudokuTheme(dark = dark, palette = palette) { SudokuNavHost() }
        }
    }
}
