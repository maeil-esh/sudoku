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
}
