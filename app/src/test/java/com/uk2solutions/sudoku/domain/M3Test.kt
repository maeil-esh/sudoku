package com.uk2solutions.sudoku.domain

import com.uk2solutions.sudoku.domain.game.*
import com.uk2solutions.sudoku.domain.generator.DailyPuzzle
import com.uk2solutions.sudoku.domain.generator.PuzzleGenerator
import com.uk2solutions.sudoku.domain.model.Board
import com.uk2solutions.sudoku.domain.model.Difficulty
import com.uk2solutions.sudoku.domain.solver.Solver
import com.uk2solutions.sudoku.domain.solver.TechniqueSolver
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class M3Test {
    @Test fun hardPuzzlesAreUniqueAndNeedLevel3() {
        val gen = PuzzleGenerator(); val tech = TechniqueSolver()
        val t0 = System.nanoTime()
        for (seed in 1L..100L) {
            val p = gen.generate(Difficulty.HARD, seed)
            val g = Board.parse(p.givens)
            assertEquals(1, Solver.countSolutions(g, 2))
            assertTrue("어려움은 등급 2~3", tech.requiredLevel(g, 3) in 2..3)
            assertNull("숨은 단일까지로는 풀리면 안 됨", tech.requiredLevel(g, 1))
            assertTrue(Board.clueCount(g) <= 32)
        }
        println("평균 생성 시간 HARD: ${(System.nanoTime() - t0) / 100 / 1_000_000.0} ms")
    }

    @Test fun dailyIsDeterministicPerDate() {
        val d = LocalDate.of(2026, 11, 16)
        assertEquals(DailyPuzzle.generate(d), DailyPuzzle.generate(d))
        assertNotEquals(DailyPuzzle.generate(d).givens, DailyPuzzle.generate(d.plusDays(1)).givens)
    }

    @Test fun hintFindsFillableCell() {
        val p = PuzzleGenerator().generate(Difficulty.EASY, 5L)
        val s = GameRules.newState(p.givens, p.solution)
        val h = Hints.next(s)!!
        assertEquals(0, s.values[h.index]); assertEquals(s.solution[h.index], h.digit)
        assertNotEquals(HintTechnique.DEDUCTION, h.technique) // 쉬움은 단일 기법으로 풀림
    }

    @Test fun hintTreatsWrongValueAsEmpty() {
        val p = PuzzleGenerator().generate(Difficulty.EASY, 5L)
        var s = GameRules.newState(p.givens, p.solution)
        val i = s.values.indexOfFirst { it == 0 }
        s = GameRules.enter(s, i, (1..9).first { it != s.solution[i] }, false, true).first
        val h = Hints.next(s, preferred = i)!!
        assertEquals(s.solution[h.index], h.digit)
    }

    @Test fun streakCounting() {
        val t = LocalDate.of(2026, 11, 16)
        assertEquals(0, Streak.current(emptySet(), t))
        assertEquals(3, Streak.current(setOf(t, t.minusDays(1), t.minusDays(2), t.minusDays(4)), t))
        assertEquals(2, Streak.current(setOf(t.minusDays(1), t.minusDays(2)), t)) // 오늘 미완료여도 유지
        assertEquals(0, Streak.current(setOf(t.minusDays(2)), t))
    }
}

class CompletionsTest {
    @Test fun detectsLineAndDigitCompletion() {
        val p = PuzzleGenerator().generate(Difficulty.EASY, 3L)
        var s = GameRules.newState(p.givens, p.solution)
        // 1행을 마지막 한 칸만 남기고 채운 뒤 마지막 칸 입력 → 행 완성
        for (i in 0 until 9) if (s.values[i] == 0) s = GameRules.enter(s, i, s.solution[i], false, false).first
        val s0 = GameRules.newState(p.givens, p.solution)
        val empties = (0 until 9).filter { s0.values[it] == 0 }
        var t = s0
        for (i in empties.dropLast(1)) t = GameRules.enter(t, i, t.solution[i], false, false).first
        val last = empties.last()
        val after = GameRules.enter(t, last, t.solution[last], false, false).first
        val c = Completions.between(t, after, last)!!
        assertTrue((0 until 9).all { it in c.cells })
        assertNull(Completions.between(t, t, last))
    }
}

class ScoringTest {
    @Test fun scoreIsDerivedAndNotFarmable() {
        val p = PuzzleGenerator().generate(Difficulty.EASY, 9L)
        var s = GameRules.newState(p.givens, p.solution)
        assertEquals(0, Scoring.score(s, Difficulty.EASY))
        val i = s.values.indexOfFirst { it == 0 }
        s = GameRules.enter(s, i, s.solution[i], false, false).first
        val one = Scoring.score(s, Difficulty.EASY)
        assertTrue(one >= 10)
        s = GameRules.erase(s, i).first
        assertEquals(0, Scoring.score(s, Difficulty.EASY))      // 지우면 점수도 사라짐
        s = GameRules.enter(s, i, s.solution[i], false, false).first
        assertEquals(one, Scoring.score(s, Difficulty.EASY))     // 다시 넣어도 불어나지 않음
        assertEquals(one * 2, Scoring.score(s, Difficulty.MEDIUM))
        val wrongI = s.values.indexOfFirst { it == 0 }
        s = GameRules.enter(s, wrongI, (1..9).first { it != s.solution[wrongI] }, false, false).first
        assertTrue(Scoring.score(s, Difficulty.EASY) >= 0)
    }

    @Test fun progressCountsFilledEmptyCells() {
        val p = PuzzleGenerator().generate(Difficulty.EASY, 9L)
        val s = GameRules.newState(p.givens, p.solution)
        val (f, t) = Scoring.progress(s)
        assertEquals(0, f); assertEquals(81 - Board.clueCount(Board.parse(p.givens)), t)
    }
}

class HintPlanTest {
    private fun fresh(d: Difficulty, seed: Long) = PuzzleGenerator().generate(d, seed).let { GameRules.newState(it.givens, it.solution) }

    @Test fun nakedSinglePlanExplainsWithEightEliminatedDigits() {
        for (seed in 1L..40L) {
            val s = fresh(Difficulty.EASY, seed)
            val h = Hints.next(s)!!
            assertEquals(HintTechnique.NAKED_SINGLE, h.technique)
            val plan = Hints.plan(s, h)
            assertEquals(3, plan.steps.size)
            val (intro, elim, answer) = plan.steps
            assertTrue(h.index in intro.highlight && intro.highlight.size == 21)     // 행+열+박스(자기 포함)
            assertEquals(8, elim.digits.size); assertFalse(h.digit in elim.digits)  // 8개 숫자를 빼면 하나만 남는다
            assertTrue(elim.emphasis.all { s.values[it] in elim.digits })
            assertEquals(listOf(h.digit), answer.digits)
        }
    }

    @Test fun hiddenSinglePlanShowsBlockersForEveryOtherEmptyCell() {
        var checked = 0
        for (seed in 1L..40L) {
            val s = fresh(Difficulty.MEDIUM, seed)
            val h = Hints.next(s)!!
            if (h.technique != HintTechnique.HIDDEN_SINGLE) continue
            val plan = Hints.plan(s, h)
            val (intro, elim, _) = plan.steps
            assertNotNull(h.unit); assertTrue(h.index in intro.highlight && intro.highlight.size == 9)
            assertTrue(elim.emphasis.all { s.values[it] == h.digit })
            val others = intro.highlight.filter { it != h.index && s.values[it] == 0 }
            for (c in others) assertTrue("칸 $c 를 막는 숫자가 보여야 함",
                com.uk2solutions.sudoku.domain.model.Units.peers[c].any { it in elim.emphasis })
            checked++
        }
        assertTrue("숨은 단일 힌트가 한 번은 나와야 함", checked > 0)
    }

    @Test fun wrongValueIsTreatedAsEmptyInPlan() {
        var s = fresh(Difficulty.EASY, 5L)
        val i = s.values.indexOfFirst { it == 0 }
        s = GameRules.enter(s, i, (1..9).first { it != s.solution[i] }, false, true).first
        val h = Hints.next(s, preferred = i)!!
        val plan = Hints.plan(s, h)
        assertTrue(plan.steps.isNotEmpty()); assertEquals(s.solution[h.index], h.digit)
    }
}


class AdvancedGenerationTest {
    @Test fun expertNeedsLevel4AndDevilStumpsHumanTechniques() {
        val gen = PuzzleGenerator()
        val tExpert = LongArray(1); val tDevil = LongArray(1)
        for (seed in 1L..12L) {
            var t0 = System.nanoTime()
            val e = gen.generate(Difficulty.EXPERT, seed)
            tExpert[0] += System.nanoTime() - t0
            val eg = Board.parse(e.givens)
            assertEquals("전문가 유일해", 1, Solver.countSolutions(eg, 2))
            assertEquals("전문가는 등급 4 필요", 4, com.uk2solutions.sudoku.domain.technique.HumanSolver.solveLevel(eg, com.uk2solutions.sudoku.domain.technique.Finders.ORDER))
            assertTrue("전문가 단서 수", Board.clueCount(eg) <= 28)
            assertEquals("결정성", e, gen.generate(Difficulty.EXPERT, seed))

            t0 = System.nanoTime()
            val d = gen.generate(Difficulty.DEVIL, seed)
            tDevil[0] += System.nanoTime() - t0
            val dg = Board.parse(d.givens)
            assertEquals("악마 유일해", 1, Solver.countSolutions(dg, 2))
            val dl = com.uk2solutions.sudoku.domain.technique.HumanSolver.solveLevel(dg, com.uk2solutions.sudoku.domain.technique.Finders.ORDER)
            assertTrue("악마는 초고수 기법(등급 5)이 필요하거나 기법으로도 막혀야 함: $dl", dl == null || dl >= 5)
            assertTrue("악마 단서 수", Board.clueCount(dg) <= 26)
        }
        println("평균 생성 시간 EXPERT: ${tExpert[0] / 12 / 1_000_000.0} ms, DEVIL: ${tDevil[0] / 12 / 1_000_000.0} ms")
    }
}
