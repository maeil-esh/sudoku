package com.uk2solutions.sudoku.domain.generator

import com.uk2solutions.sudoku.domain.model.Board
import com.uk2solutions.sudoku.domain.model.Difficulty
import com.uk2solutions.sudoku.domain.solver.Solver
import com.uk2solutions.sudoku.domain.solver.TechniqueSolver

data class Puzzle(val difficulty: Difficulty, val givens: String, val solution: String)

/**
 * 온디바이스 퍼즐 생성기(SPEC §5.2). 같은 (difficulty, seed)는 항상 같은 퍼즐을 만든다.
 * 쉬움·보통·어려움 지원. 전문가·악마는 X-윙 등 기법 추가 후 지원.
 */
class PuzzleGenerator(private val techniques: TechniqueSolver = TechniqueSolver()) {

    fun generate(difficulty: Difficulty, seed: Long): Puzzle {
        val cap = when (difficulty) {
            Difficulty.EASY -> 1
            Difficulty.MEDIUM -> 2
            Difficulty.HARD -> 3
            else -> throw UnsupportedOperationException("$difficulty 는 아직 지원하지 않음")
        }
        var attempt = 0L
        while (true) {
            val rnd = SeededRandom(seed * 1_000_003L + attempt++)
            val p = tryGenerate(difficulty, cap, rnd)
            if (p != null) return p
        }
    }

    private fun tryGenerate(difficulty: Difficulty, cap: Int, rnd: SeededRandom): Puzzle? {
        val solution = Solver.randomFilledGrid(rnd)
        val grid = solution.copyOf()
        val range = difficulty.clueRange
        val target = range.first + rnd.nextInt(range.last - range.first + 1)

        // 중심 대칭 쌍(i, 80-i)을 무작위 순서로 제거
        val order = IntArray(41) { it }.also { rnd.shuffle(it) }
        var clues = 81
        for (i in order) {
            if (clues <= target) break
            val j = 80 - i
            val a = grid[i]; val b = grid[j]
            grid[i] = 0; grid[j] = 0
            val ok = Solver.countSolutions(grid, 2) == 1 && techniques.requiredLevel(grid, cap) != null
            if (ok) clues -= if (i == j) 1 else 2 else { grid[i] = a; grid[j] = b }
        }
        if (clues > range.last) return null
        val level = techniques.requiredLevel(grid, cap) ?: return null
        if (level != cap) return null
        return Puzzle(difficulty, Board.format(grid), Board.format(solution))
    }
}
