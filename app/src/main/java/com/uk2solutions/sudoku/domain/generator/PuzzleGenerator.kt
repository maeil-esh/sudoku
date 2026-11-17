package com.uk2solutions.sudoku.domain.generator

import com.uk2solutions.sudoku.domain.model.Board
import com.uk2solutions.sudoku.domain.model.Difficulty
import com.uk2solutions.sudoku.domain.solver.Solver
import com.uk2solutions.sudoku.domain.solver.TechniqueSolver
import com.uk2solutions.sudoku.domain.technique.Finders
import com.uk2solutions.sudoku.domain.technique.HumanSolver

data class Puzzle(val difficulty: Difficulty, val givens: String, val solution: String)

/**
 * 온디바이스 퍼즐 생성기(SPEC §5.2). 같은 (difficulty, seed)는 항상 같은 퍼즐을 만든다.
 * 쉬움·보통·어려움은 TechniqueSolver 등급으로, 전문가·악마는 HumanSolver(기법 엔진)로 판정한다.
 */
class PuzzleGenerator(private val techniques: TechniqueSolver = TechniqueSolver()) {

    /** 난이도별 필요 기법 등급 범위. EASY=단일 후보만, MEDIUM=숨은 단일까지, HARD=포인팅·쌍·삼중까지(2~3). */
    private fun levelRange(d: Difficulty): IntRange = when (d) {
        Difficulty.EASY -> 0..0
        Difficulty.MEDIUM -> 1..1
        Difficulty.HARD -> 2..3
        else -> 0..0 // 전문가·악마는 사람 기법 엔진으로 판정한다(generateAdvanced)
    }

    fun generate(difficulty: Difficulty, seed: Long): Puzzle {
        if (difficulty == Difficulty.EXPERT || difficulty == Difficulty.DEVIL) return generateAdvanced(difficulty, seed)
        val levels = levelRange(difficulty)
        var attempt = 0L
        while (true) {
            val rnd = SeededRandom(seed * 1_000_003L + attempt++)
            val p = tryGenerate(difficulty, levels, rnd)
            if (p != null) return p
        }
    }

    /**
     * 전문가: 사람 기법만으로 풀리지만 등급 4(X-윙·Y-윙·스워드피시)가 반드시 필요한 퍼즐.
     * 악마: 11개 사람 기법으로도 막히는(추측·고급 추론이 필요한) 유일해 퍼즐(SPEC §5.2). 같은 (난이도, 시드)는 같은 퍼즐.
     */
    private fun generateAdvanced(d: Difficulty, seed: Long): Puzzle {
        val range = d.clueRange
        var attempt = 0L
        while (true) {
            val clues = range.first + ((seed + attempt) % (range.last - range.first + 1)).toInt()
            val p = generateUnique(seed * 1_000_003L + attempt++, clues)
            val level = HumanSolver.solveLevel(Board.parse(p.givens), Finders.ORDER)
            val ok = if (d == Difficulty.EXPERT) level == 4 else level == null
            if (ok && Board.clueCount(Board.parse(p.givens)) <= range.last + 1) return Puzzle(d, p.givens, p.solution)
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
