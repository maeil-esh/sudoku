package com.uk2solutions.sudoku.domain

import com.uk2solutions.sudoku.domain.generator.PuzzleGenerator
import com.uk2solutions.sudoku.domain.model.Board
import com.uk2solutions.sudoku.domain.technique.Finders
import com.uk2solutions.sudoku.domain.technique.HumanSolver
import com.uk2solutions.sudoku.domain.technique.Technique
import org.junit.Test

/** "11개 기법으로 얼마나 풀리나"를 재는 통계(단정 없음, 출력용). 무작위 유일해 퍼즐을 단서 수별로 푼다. */
class SolveRateTest {
    @Test fun printSolveRates() {
        val gen = PuzzleGenerator()
        val n = 300
        val upTo = { lvl: Int -> Finders.ORDER.filter { it.level <= lvl } }
        println("단서 수별 해결률(퍼즐 ${n}개씩): 단일만 / ~숨은단일 / ~쌍·포인팅 / ~삼중 / ~11개(X-윙·Y-윙·스워드피시) / 전체18개(초고수 6개 추가)")
        for (clues in listOf(32, 28, 26, 24, 22)) {
            val counts = IntArray(6)
            var actualClues = 0
            for (seed in 1L..n) {
                val p = gen.generateUnique(seed * 31 + clues, clues)
                val v = Board.parse(p.givens); actualClues += Board.clueCount(v)
                for ((i, lvl) in listOf(0, 1, 2, 3, 4, 5).withIndex()) if (HumanSolver.solveLevel(v, upTo(lvl)) != null) counts[i]++
            }
            println("목표 $clues (실제 평균 ${"%.1f".format(actualClues / n.toDouble())}): " + counts.joinToString(" / ") { "${it * 100 / n}%" })
        }
    }
}
