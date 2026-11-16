package com.uk2solutions.sudoku.data

import com.uk2solutions.sudoku.domain.game.GameCodec
import com.uk2solutions.sudoku.domain.game.GameRules
import com.uk2solutions.sudoku.domain.game.GameState
import com.uk2solutions.sudoku.domain.generator.DailyPuzzle
import com.uk2solutions.sudoku.domain.generator.PuzzleGenerator
import com.uk2solutions.sudoku.domain.model.Board
import com.uk2solutions.sudoku.domain.model.Difficulty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameRepository @Inject constructor(private val dao: GameDao) {
    private val generator = PuzzleGenerator()
    private val random = SecureRandom()

    fun observeResumable(): Flow<GameEntity?> = dao.observeInProgress(GameEntity.MODE_NORMAL)

    suspend fun resumableId(): Long? = dao.inProgressId(GameEntity.MODE_NORMAL)

    /** 진행 중이던 일반 게임은 포기 처리하고 새 퍼즐을 만든다(진행 중 게임은 1개만 유지). */
    suspend fun startNew(difficulty: Difficulty): Long {
        val puzzle = withContext(Dispatchers.Default) { generator.generate(difficulty, random.nextLong()) }
        val now = System.currentTimeMillis()
        dao.abandonInProgress(GameEntity.MODE_NORMAL, now)
        val blankNotes = GameCodec.encodeInts(IntArray(Board.SIZE))
        return dao.insert(
            GameEntity(
                mode = GameEntity.MODE_NORMAL, difficulty = difficulty.name,
                givens = puzzle.givens, solution = puzzle.solution,
                cellValues = puzzle.givens, notes = blankNotes, startedAt = now,
            ),
        )
    }

    fun observeDaily(date: String) = dao.observeDaily(date)
    fun observeDailyCompleted() = dao.observeDailyCompleted()
    fun observeStats() = dao.observeStats()

    /** 오늘의 도전: 같은 날짜의 게임이 있으면 그것을, 없으면 날짜 시드로 만든다(모든 사용자 동일 퍼즐). */
    suspend fun startOrGetDaily(date: java.time.LocalDate): Long {
        dao.dailyId(date.toString())?.let { return it }
        val puzzle = withContext(Dispatchers.Default) { DailyPuzzle.generate(date, generator) }
        val blank = GameCodec.encodeInts(IntArray(Board.SIZE))
        return dao.insert(
            GameEntity(
                mode = "DAILY", difficulty = puzzle.difficulty.name, givens = puzzle.givens, solution = puzzle.solution,
                cellValues = puzzle.givens, notes = blank, dailyDate = date.toString(), startedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun load(id: Long): Loaded? = dao.get(id)?.let { Loaded(it.status, Difficulty.valueOf(it.difficulty), it.toState(), it.mode == "DAILY") }

    suspend fun save(id: Long, s: GameState) = dao.updateProgress(
        id, Board.format(s.values), GameCodec.encodeInts(s.notes),
        s.elapsedMs, s.mistakes, s.hintsUsed, GameCodec.encodeUndo(s.undo),
    )

    suspend fun complete(id: Long) = dao.markCompleted(id, System.currentTimeMillis())

    class Loaded(val status: String, val difficulty: Difficulty, val state: GameState, val daily: Boolean)

    companion object {
        fun GameEntity.toState(): GameState = GameRules.newState(givens, solution).copy(
            values = Board.parse(cellValues),
            notes = GameCodec.decodeInts(notes),
            mistakes = mistakes, elapsedMs = elapsedMs, hintsUsed = hintsUsed,
            undo = GameCodec.decodeUndo(undoStack),
        )
    }
}
