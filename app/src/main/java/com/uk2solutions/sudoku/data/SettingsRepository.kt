package com.uk2solutions.sudoku.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** 설정 전체(SPEC §5.1·§8, 설정 화면 S5). themeMode: 0=시스템 1=라이트 2=다크. */
data class AppSettings(
    val themeMode: Int = 0,
    val sound: Boolean = true,
    val vibration: Boolean = true,
    val showTimer: Boolean = true,
    val mistakeLimitEnabled: Boolean = true,
    val mistakeLimit: Int = 3,
    val numberFirst: Boolean = false,
    val highlightArea: Boolean = true,
    val highlightSame: Boolean = true,
    val showErrors: Boolean = true,
    val autoRemoveNotes: Boolean = true,
    val autoNextNumber: Boolean = true,
    val showProgress: Boolean = true,
    val showScore: Boolean = true,
    val completeAnimation: Boolean = true,
    val showRemaining: Boolean = true,
    val scoreAnimation: Boolean = true,
    val notifyEnabled: Boolean = false,
    val notifyHour: Int = 20,
    val palette: Int = 0,
    val hintsUsedToday: Int = 0,
)

internal val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private object K {
        val themeMode = intPreferencesKey("themeMode")
        val sound = booleanPreferencesKey("sound")
        val vibration = booleanPreferencesKey("vibration")
        val showTimer = booleanPreferencesKey("showTimer")
        val limitEnabled = booleanPreferencesKey("mistakeLimitEnabled")
        val limit = intPreferencesKey("mistakeLimit")
        val numberFirst = booleanPreferencesKey("numberFirst")
        val highlightArea = booleanPreferencesKey("highlightArea")
        val highlightSame = booleanPreferencesKey("highlightSame")
        val showErrors = booleanPreferencesKey("showErrors")
        val autoRemoveNotes = booleanPreferencesKey("autoRemoveNotes")
        val autoNextNumber = booleanPreferencesKey("autoNextNumber")
        val showProgress = booleanPreferencesKey("showProgress")
        val showScore = booleanPreferencesKey("showScore")
        val completeAnimation = booleanPreferencesKey("completeAnimation")
        val showRemaining = booleanPreferencesKey("showRemaining")
        val scoreAnimation = booleanPreferencesKey("scoreAnimation")
        val notifyEnabled = booleanPreferencesKey("notifyEnabled")
        val notifyHour = intPreferencesKey("notifyHour")
        val palette = intPreferencesKey("palette")
        val hintsUsed = intPreferencesKey("hintsUsedToday")
        val hintsDate = stringPreferencesKey("hintsDate")
    }

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings()
        return AppSettings(
            themeMode = this[K.themeMode] ?: d.themeMode, sound = this[K.sound] ?: d.sound,
            vibration = this[K.vibration] ?: d.vibration, showTimer = this[K.showTimer] ?: d.showTimer,
            mistakeLimitEnabled = this[K.limitEnabled] ?: d.mistakeLimitEnabled, mistakeLimit = this[K.limit] ?: d.mistakeLimit,
            numberFirst = this[K.numberFirst] ?: d.numberFirst, highlightArea = this[K.highlightArea] ?: d.highlightArea,
            highlightSame = this[K.highlightSame] ?: d.highlightSame, showErrors = this[K.showErrors] ?: d.showErrors,
            autoRemoveNotes = this[K.autoRemoveNotes] ?: d.autoRemoveNotes, autoNextNumber = this[K.autoNextNumber] ?: d.autoNextNumber,
            showProgress = this[K.showProgress] ?: d.showProgress, showScore = this[K.showScore] ?: d.showScore,
            completeAnimation = this[K.completeAnimation] ?: d.completeAnimation, showRemaining = this[K.showRemaining] ?: d.showRemaining,
            scoreAnimation = this[K.scoreAnimation] ?: d.scoreAnimation, notifyEnabled = this[K.notifyEnabled] ?: d.notifyEnabled,
            notifyHour = this[K.notifyHour] ?: d.notifyHour, palette = this[K.palette] ?: d.palette,
            hintsUsedToday = if (this[K.hintsDate] == LocalDate.now().toString()) this[K.hintsUsed] ?: 0 else 0,
        )
    }

    private fun MutablePreferences.write(s: AppSettings) {
        this[K.themeMode] = s.themeMode; this[K.sound] = s.sound; this[K.vibration] = s.vibration
        this[K.showTimer] = s.showTimer; this[K.limitEnabled] = s.mistakeLimitEnabled; this[K.limit] = s.mistakeLimit
        this[K.numberFirst] = s.numberFirst; this[K.highlightArea] = s.highlightArea; this[K.highlightSame] = s.highlightSame
        this[K.showErrors] = s.showErrors; this[K.autoRemoveNotes] = s.autoRemoveNotes; this[K.autoNextNumber] = s.autoNextNumber
        this[K.showProgress] = s.showProgress; this[K.showScore] = s.showScore
        this[K.completeAnimation] = s.completeAnimation; this[K.showRemaining] = s.showRemaining
        this[K.scoreAnimation] = s.scoreAnimation; this[K.notifyEnabled] = s.notifyEnabled; this[K.notifyHour] = s.notifyHour; this[K.palette] = s.palette
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { it.toSettings() }

    suspend fun update(block: (AppSettings) -> AppSettings) {
        context.dataStore.edit { p -> p.write(block(p.toSettings())) }
    }

    /** 오늘 힌트 한도(로컬 날짜 기준 초기화) 안이면 1회 소모하고 true. [unlimited]면 소모 없이 true. */
    suspend fun consumeHint(freePerDay: Int, unlimited: Boolean = false): Boolean {
        if (unlimited) return true
        var ok = false
        context.dataStore.edit { p ->
            val today = LocalDate.now().toString()
            val used = if (p[K.hintsDate] == today) p[K.hintsUsed] ?: 0 else 0
            if (used < freePerDay) { p[K.hintsDate] = today; p[K.hintsUsed] = used + 1; ok = true }
        }
        return ok
    }
}
