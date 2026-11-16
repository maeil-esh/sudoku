package com.uk2solutions.sudoku.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** SPEC §5.1·§8 게임 관련 설정(나머지 설정 화면은 M5). */
data class AppSettings(
    val showErrors: Boolean = true,
    val mistakeLimitEnabled: Boolean = true,
    val mistakeLimit: Int = 3,
    val highlightSame: Boolean = true,
    val autoRemoveNotes: Boolean = true,
    val vibration: Boolean = true,
)

private val Context.dataStore by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private object K {
        val showErrors = booleanPreferencesKey("showErrors")
        val limitEnabled = booleanPreferencesKey("mistakeLimitEnabled")
        val limit = intPreferencesKey("mistakeLimit")
        val highlightSame = booleanPreferencesKey("highlightSame")
        val autoRemoveNotes = booleanPreferencesKey("autoRemoveNotes")
        val vibration = booleanPreferencesKey("vibration")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p: Preferences ->
        AppSettings(
            showErrors = p[K.showErrors] ?: true,
            mistakeLimitEnabled = p[K.limitEnabled] ?: true,
            mistakeLimit = p[K.limit] ?: 3,
            highlightSame = p[K.highlightSame] ?: true,
            autoRemoveNotes = p[K.autoRemoveNotes] ?: true,
            vibration = p[K.vibration] ?: true,
        )
    }

    suspend fun update(block: (AppSettings) -> AppSettings) {
        context.dataStore.edit { p ->
            val n = block(AppSettings(
                p[K.showErrors] ?: true, p[K.limitEnabled] ?: true, p[K.limit] ?: 3,
                p[K.highlightSame] ?: true, p[K.autoRemoveNotes] ?: true, p[K.vibration] ?: true,
            ))
            p[K.showErrors] = n.showErrors; p[K.limitEnabled] = n.mistakeLimitEnabled; p[K.limit] = n.mistakeLimit
            p[K.highlightSame] = n.highlightSame; p[K.autoRemoveNotes] = n.autoRemoveNotes; p[K.vibration] = n.vibration
        }
    }
}
