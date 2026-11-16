package com.uk2solutions.sudoku.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Insert suspend fun insert(game: GameEntity): Long

    @Query("SELECT * FROM game WHERE id = :id") suspend fun get(id: Long): GameEntity?

    @Query("SELECT * FROM game WHERE mode = :mode AND status = 'IN_PROGRESS' ORDER BY id DESC LIMIT 1")
    fun observeInProgress(mode: String): Flow<GameEntity?>

    @Query("SELECT id FROM game WHERE mode = :mode AND status = 'IN_PROGRESS' ORDER BY id DESC LIMIT 1")
    suspend fun inProgressId(mode: String): Long?

    @Query("UPDATE game SET cellValues = :values, notes = :notes, elapsedMs = :elapsedMs, mistakes = :mistakes, hintsUsed = :hints, undoStack = :undo WHERE id = :id AND status = 'IN_PROGRESS'")
    suspend fun updateProgress(id: Long, values: String, notes: String, elapsedMs: Long, mistakes: Int, hints: Int, undo: String)

    @Query("UPDATE game SET status = 'COMPLETED', finishedAt = :at WHERE id = :id")
    suspend fun markCompleted(id: Long, at: Long)

    @Query("UPDATE game SET status = 'ABANDONED', finishedAt = :at WHERE mode = :mode AND status = 'IN_PROGRESS'")
    suspend fun abandonInProgress(mode: String, at: Long)

    @Query("SELECT * FROM game WHERE mode = 'DAILY' AND dailyDate = :date ORDER BY id DESC LIMIT 1")
    fun observeDaily(date: String): Flow<GameEntity?>

    @Query("SELECT id FROM game WHERE mode = 'DAILY' AND dailyDate = :date LIMIT 1")
    suspend fun dailyId(date: String): Long?

    @Query("SELECT dailyDate FROM game WHERE mode = 'DAILY' AND status = 'COMPLETED' AND dailyDate IS NOT NULL")
    fun observeDailyCompleted(): Flow<List<String>>

    @Query("SELECT finishedAt FROM game WHERE status = 'COMPLETED' AND finishedAt IS NOT NULL")
    fun observeFinishedAt(): Flow<List<Long>>

    @Query("SELECT difficulty, COUNT(*) AS cnt, MIN(elapsedMs) AS best, AVG(elapsedMs) AS avg FROM game WHERE status = 'COMPLETED' GROUP BY difficulty")
    fun observeStats(): Flow<List<DifficultyStat>>
}

data class DifficultyStat(val difficulty: String, val cnt: Int, val best: Long, val avg: Double)
