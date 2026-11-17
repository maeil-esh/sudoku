package com.uk2solutions.sudoku.domain.training

import com.uk2solutions.sudoku.domain.generator.PuzzleGenerator
import com.uk2solutions.sudoku.domain.model.Board
import com.uk2solutions.sudoku.domain.technique.CandGrid
import com.uk2solutions.sudoku.domain.technique.Deduction
import com.uk2solutions.sudoku.domain.technique.Finders
import com.uk2solutions.sudoku.domain.technique.HumanSolver
import com.uk2solutions.sudoku.domain.technique.Technique

/**
 * 레슨 예제 하나: 그 기법이 "정말 필요한" 판(더 단순한 기법을 모두 쓴 뒤 막힌 상태)과 추론.
 * [quizDigit]/[expectedCells]: 연습 문제의 정답(지울 숫자와 그 칸들 / 칸 고르기의 칸).
 */
class Example(val grid: CandGrid, val deduction: Deduction, val solution: IntArray, val quizDigit: Int, val expectedCells: Set<Int>)

object TrainingExamples {
    private const val MAX_TRIES = 4000

    /** [index]번째 예제(같은 입력이면 항상 같은 결과). 못 찾으면 null. */
    fun find(lesson: Lesson, index: Int): Example? {
        val gen = PuzzleGenerator()
        var seed = (lesson.id.hashCode().toLong() and 0xFFFF) * 100_000L + index * 7_919L + 1
        repeat(MAX_TRIES) {
            val clues = 23 + (seed % 4).toInt()
            val p = gen.generateUnique(seed++, if (lesson.technique.level >= 2) clues else clues + 12)
            scan(p.givens, p.solution, lesson)?.let { return it }
        }
        return null
    }

    internal fun scan(givens: String, solution: String, lesson: Lesson): Example? {
        val t = lesson.technique
        val g = CandGrid.from(Board.parse(givens)) ?: return null
        val simpler = Finders.ORDER.filter { it.ordinal < t.ordinal }
        var guard = 0
        while (!g.solved && guard++ < 300) {
            // 더 단순한 기법이 있으면 먼저 쓴다. 더 이상 없을 때만 "이 기법이 필요한 순간"이다.
            val easier = HumanSolver.nextSimplest(g, simpler)
            if (easier != null) { HumanSolver.apply(g, easier); continue }
            val d = Finders.find(g, t) ?: return null
            return build(g, d, Board.parse(solution))
        }
        return null
    }

    private fun build(g: CandGrid, d: Deduction, sol: IntArray): Example? {
        d.place?.let { (i, v) -> return Example(g.copy(), d, sol, v, setOf(i)) }
        val digit = d.eliminations.groupBy { it.second }.maxByOrNull { it.value.size }!!.key
        val cells = d.eliminations.filter { it.second == digit }.map { it.first }.toSet()
        if (cells.size !in 1..4) return null
        return Example(g.copy(), d, sol, digit, cells)
    }
}
