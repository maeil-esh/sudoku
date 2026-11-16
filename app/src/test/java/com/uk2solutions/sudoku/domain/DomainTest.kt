package com.uk2solutions.sudoku.domain

import com.uk2solutions.sudoku.domain.generator.PuzzleGenerator
import com.uk2solutions.sudoku.domain.generator.SeededRandom
import com.uk2solutions.sudoku.domain.model.Board
import com.uk2solutions.sudoku.domain.model.Difficulty
import com.uk2solutions.sudoku.domain.solver.Solver
import com.uk2solutions.sudoku.domain.solver.TechniqueSolver
import org.junit.Assert.*
import org.junit.Test

class DomainTest {
    private val known =
        "530070000600195000098000060800060003400803001700020006060000280000419005000080079"

    @Test fun boardRoundTrip() {
        assertEquals(known, Board.format(Board.parse(known)))
    }

    @Test fun prngIsDeterministic() {
        val a = SeededRandom(42); val b = SeededRandom(42)
        repeat(100) { assertEquals(a.nextLong(), b.nextLong()) }
        // 고정 기대값: 구현이 바뀌어 기기 간 결정성이 깨지는 것을 막는다.
        assertEquals(0xE220A8397B1DCDAFuL.toLong(), SeededRandom(0).nextLong())
    }

    @Test fun solverFindsUniqueSolution() {
        val g = Board.parse(known)
        assertEquals(1, Solver.countSolutions(g, 2))
        val s = Solver.solve(g)!!
        assertTrue(s.none { it == 0 })
    }

    @Test fun solverDetectsMultipleAndNone() {
        assertEquals(2, Solver.countSolutions(IntArray(81), 2))
        val bad = Board.parse(known); bad[2] = 5 // 같은 행에 5 중복
        assertEquals(0, Solver.countSolutions(bad, 2))
    }

    @Test fun techniqueSolverGradesKnownPuzzle() {
        assertNotNull(TechniqueSolver().requiredLevel(Board.parse(known), 1))
        // 쉬움 퍼즐은 숨은 단일 없이(단일 후보만으로) 풀려야 한다
        val easy = PuzzleGenerator().generate(Difficulty.EASY, 3L)
        assertEquals(0, TechniqueSolver().requiredLevel(Board.parse(easy.givens), 0))
    }

    @Test fun sameSeedSamePuzzle() {
        val g = PuzzleGenerator()
        for (d in listOf(Difficulty.EASY, Difficulty.MEDIUM)) {
            assertEquals(g.generate(d, 7L), g.generate(d, 7L))
        }
    }

    @Test fun uniqueSolutionOn1000Seeds() {
        val gen = PuzzleGenerator()
        val tech = TechniqueSolver()
        val times = mutableMapOf(Difficulty.EASY to 0L, Difficulty.MEDIUM to 0L)
        var n = 0
        val clueSum = mutableMapOf(Difficulty.EASY to 0, Difficulty.MEDIUM to 0)
        for (seed in 1L..500L) for (d in times.keys.toList()) {
            val t0 = System.nanoTime()
            val p = gen.generate(d, seed)
            times[d] = times.getValue(d) + (System.nanoTime() - t0)
            val givens = Board.parse(p.givens)
            val sol = Board.parse(p.solution)
            assertEquals("seed=$seed $d 유일해", 1, Solver.countSolutions(givens, 2))
            for (i in 0 until 81) if (givens[i] != 0) assertEquals(sol[i], givens[i])
            assertTrue("단서 수 상한", Board.clueCount(givens) <= d.clueRange.last)
            val lvl = tech.requiredLevel(givens, 1)
            assertEquals("seed=$seed $d 등급", if (d == Difficulty.EASY) 0 else 1, lvl)
            clueSum[d] = clueSum.getValue(d) + Board.clueCount(givens)
            n++
        }
        assertEquals(1000, n)
        clueSum.forEach { (d, c) -> println("평균 단서 수 $d: ${c / 500.0}") }
        times.forEach { (d, ns) -> println("평균 생성 시간 $d: ${ns / 500 / 1_000_000.0} ms") }
        times.values.forEach { assertTrue("평균 500ms 이하", it / 500 / 1_000_000 <= 500) }
    }
}
