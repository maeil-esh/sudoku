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

    /** 난이도별 필요 기법 등급 범위. EASY=단일 후보만, MEDIUM=숨은 단일까지, HARD=포인팅·쌍·삼중까지(2~3). */
    private fun levelRange(d: Difficulty): IntRange = when (d) {
        Difficulty.EASY -> 0..0
        Difficulty.MEDIUM -> 1..1
        Difficulty.HARD -> 2..3
        else -> throw UnsupportedOperationException("$d 는 아직 지원하지 않음")
    }

    fun generate(difficulty: Difficulty, seed: Long): Puzzle {
        val levels = levelRange(difficulty)
        var attempt = 0L
        while (true) {
            val rnd = SeededRandom(seed * 1_000_003L + attempt++)
            val p = tryGenerate(difficulty, levels, rnd)
            if (p != null) return p
        }
    }

    /** 기법 등급 제약 없이 유일해만 보장하는 퍼즐(단서 수 [clues] 이하까지 대칭 제거). 트레이닝 예제 탐색용. */
    fun generateUnique(seed: Long, clues: Int = 25): Puzzle {
        val rnd = SeededRandom(seed * 7_000_003L + 11)
        val solution = Solver.randomFilledGrid(rnd)
        val grid = solution.copyOf()
        var n = 81
        for (i in IntArray(41) { it }.also { rnd.shuffle(it) }) {
            if (n <= clues) break
            val j = 80 - i
            val a = grid[i]; val b = grid[j]
            grid[i] = 0; grid[j] = 0
            if (Solver.countSolutions(grid, 2) == 1) n -= if (i == j) 1 else 2 else { grid[i] = a; grid[j] = b }
        }
        return Puzzle(Difficulty.DEVIL, Board.format(grid), Board.format(solution))
    }

    private fun tryGenerate(difficulty: Difficulty, levels: IntRange, rnd: SeededRandom): Puzzle? {
        val cap = levels.last
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
        if (level !in levels) return null
        return Puzzle(difficulty, Board.format(grid), Board.format(solution))
    }
}
