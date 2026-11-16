package com.uk2solutions.sudoku.domain.generator

import com.uk2solutions.sudoku.domain.model.Difficulty
import java.time.LocalDate

/** SPEC §5.3: 시드 = yyyyMMdd + 고정 솔트. 난이도는 어려움 고정(열린 결정 4). */
object DailyPuzzle {
    private const val SALT = 0x5D0C0DA7L
    val difficulty = Difficulty.HARD

    fun seed(date: LocalDate): Long =
        (date.year * 10000L + date.monthValue * 100 + date.dayOfMonth) * 1_000_003L xor SALT

    fun generate(date: LocalDate, generator: PuzzleGenerator = PuzzleGenerator()): Puzzle =
        generator.generate(difficulty, seed(date))
}
