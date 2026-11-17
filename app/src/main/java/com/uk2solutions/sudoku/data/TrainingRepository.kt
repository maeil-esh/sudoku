package com.uk2solutions.sudoku.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** 레슨별 최고 점수(0~3). 기기에만 저장한다("id:점수,id:점수"). */
@Singleton
class TrainingRepository @Inject constructor(@ApplicationContext private val context: Context) {
    private val key = stringPreferencesKey("trainingScores")

    val scores: Flow<Map<String, Int>> = context.dataStore.data.map { parse(it[key]) }

    suspend fun record(lessonId: String, score: Int) {
        context.dataStore.edit { p ->
            val cur = parse(p[key]).toMutableMap()
            cur[lessonId] = maxOf(cur[lessonId] ?: -1, score)
            p[key] = cur.entries.joinToString(",") { "${it.key}:${it.value}" }
        }
    }

    private fun parse(s: String?): Map<String, Int> =
        s?.split(",")?.mapNotNull { e -> e.split(":").takeIf { it.size == 2 }?.let { it[0] to (it[1].toIntOrNull() ?: return@mapNotNull null) } }?.toMap() ?: emptyMap()
}
