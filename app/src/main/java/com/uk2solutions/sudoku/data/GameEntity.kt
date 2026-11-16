package com.uk2solutions.sudoku.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** SPEC §8 `game` 테이블. 값/메모/되돌리기는 직렬화 문자열로 저장. (`values`는 SQL 예약어라 cellValues) */
@Entity(tableName = "game")
data class GameEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: String,            // NORMAL / DAILY
    val difficulty: String,
    val givens: String,
    val solution: String,
    val cellValues: String,
    val notes: String,
    val elapsedMs: Long = 0,
    val mistakes: Int = 0,
    val hintsUsed: Int = 0,
    val undoStack: String = "",
    val status: String = STATUS_IN_PROGRESS,
    val dailyDate: String? = null,
    val startedAt: Long,
    val finishedAt: Long? = null,
) {
    companion object {
        const val MODE_NORMAL = "NORMAL"
        const val STATUS_IN_PROGRESS = "IN_PROGRESS"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_ABANDONED = "ABANDONED"
    }
}
