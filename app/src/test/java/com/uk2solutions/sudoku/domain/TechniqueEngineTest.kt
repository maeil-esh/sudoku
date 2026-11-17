package com.uk2solutions.sudoku.domain

import com.uk2solutions.sudoku.domain.generator.PuzzleGenerator
import com.uk2solutions.sudoku.domain.model.Board
import com.uk2solutions.sudoku.domain.technique.*
import org.junit.Assert.*
import org.junit.Test

class TechniqueEngineTest {
    /** 핵심 안전성: 어떤 기법도 정답 숫자를 지우거나 틀린 값을 채우면 안 된다. */
    @Test fun techniquesNeverContradictTheSolution() {
        val gen = PuzzleGenerator()
        val used = HashMap<Technique, Int>()
        var solvedByHuman = 0
        for (seed in 1L..600L) {
            val pz = gen.generateUnique(seed, 22 + (seed % 4).toInt())
            val sol = Board.parse(pz.solution)
            val g = CandGrid.from(Board.parse(pz.givens))!!
            var guard = 0
            while (!g.solved && guard++ < 400) {
                val d = HumanSolver.nextSimplest(g, Finders.ORDER) ?: break
                d.place?.let { (i, v) -> assertEquals("seed=$seed ${d.technique} 틀린 값", sol[i], v) }
                for ((i, v) in d.eliminations) assertNotEquals("seed=$seed ${d.technique} 정답 후보를 지움 칸=$i 숫자=$v", sol[i], v)
                assertTrue("쓸모 없는 추론", d.place != null || d.eliminations.isNotEmpty())
                used[d.technique] = (used[d.technique] ?: 0) + 1
                HumanSolver.apply(g, d)
            }
            if (g.solved) solvedByHuman++
        }
        println("기법 사용 횟수: " + Technique.entries.joinToString { "${it.name}=${used[it] ?: 0}" })
        println("사람 기법(18개)만으로 푼 퍼즐: $solvedByHuman/600")
        for (t in Technique.entries.filter { it.level <= 4 }) assertTrue("$t 이(가) 한 번도 쓰이지 않음", (used[t] ?: 0) > 0)
    }

    @Test fun solveLevelOfGeneratedPuzzlesMatchesOldGrader() {
        val gen = PuzzleGenerator()
        for (seed in 1L..40L) {
            val e = gen.generate(com.uk2solutions.sudoku.domain.model.Difficulty.EASY, seed)
            assertEquals(0, HumanSolver.solveLevel(Board.parse(e.givens), Finders.ORDER))
            val m = gen.generate(com.uk2solutions.sudoku.domain.model.Difficulty.MEDIUM, seed)
            assertEquals(1, HumanSolver.solveLevel(Board.parse(m.givens), Finders.ORDER))
        }
    }
}

class TrainingExamplesTest {
    @Test fun everyLessonHasExamples() {
        val sb = StringBuilder()
        for (lesson in com.uk2solutions.sudoku.domain.training.Lessons.all) {
            val t0 = System.nanoTime()
            val exs = (0..3).map { com.uk2solutions.sudoku.domain.training.TrainingExamples.find(lesson, it) }
            val ms = (System.nanoTime() - t0) / 1_000_000
            sb.append("${lesson.id}: ${ms}ms\n")
            exs.forEachIndexed { k, ex ->
                assertNotNull("${lesson.id} 예제 $k 를 찾지 못함", ex)
                ex!!
                // 퍼즐 정답과 모순되지 않는다
                ex.deduction.place?.let { (i, v) -> assertEquals(ex.solution[i], v) }
                ex.deduction.eliminations.forEach { (i, v) -> assertNotEquals(ex.solution[i], v) }
                assertTrue(ex.expectedCells.isNotEmpty())
                assertEquals(lesson.technique, ex.deduction.technique)
            }
            // 같은 입력이면 같은 예제
            assertEquals(exs[0]!!.deduction, com.uk2solutions.sudoku.domain.training.TrainingExamples.find(lesson, 0)!!.deduction)
        }
        println("레슨 예제 탐색 시간:\n$sb")
    }
}

class QuizGraderTest {
    private fun ex(id: String, i: Int = 1) = com.uk2solutions.sudoku.domain.training.TrainingExamples.find(com.uk2solutions.sudoku.domain.training.Lessons.byId(id)!!, i)!!

    @Test fun gradesEachKind() {
        val e1 = ex("naked_single")
        assertTrue(com.uk2solutions.sudoku.domain.training.QuizGrader.digit(e1, e1.quizDigit))
        assertFalse(com.uk2solutions.sudoku.domain.training.QuizGrader.digit(e1, e1.quizDigit % 9 + 1))
        val e2 = ex("hidden_single")
        assertTrue(com.uk2solutions.sudoku.domain.training.QuizGrader.cell(e2, e2.expectedCells.first()))
        assertFalse(com.uk2solutions.sudoku.domain.training.QuizGrader.cell(e2, (0 until 81).first { it !in e2.expectedCells }))
        val e3 = ex("x_wing")
        assertTrue(com.uk2solutions.sudoku.domain.training.QuizGrader.eliminations(e3, e3.expectedCells))
        assertFalse("일부만 고르면 오답", e3.expectedCells.size > 1 && com.uk2solutions.sudoku.domain.training.QuizGrader.eliminations(e3, setOf(e3.expectedCells.first())))
        assertFalse("더 고르면 오답", com.uk2solutions.sudoku.domain.training.QuizGrader.eliminations(e3, e3.expectedCells + (0 until 81).first { it !in e3.expectedCells }))
        assertFalse("아무것도 안 고르면 오답", com.uk2solutions.sudoku.domain.training.QuizGrader.eliminations(e3, emptySet()))
    }

    @Test fun catalogIsConsistent() {
        val ids = com.uk2solutions.sudoku.domain.training.Lessons.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        assertEquals("rules", ids.first())
        // 무료 레슨은 앞쪽에 연속으로 있고, 프리미엄은 그 뒤
        val firstPremium = com.uk2solutions.sudoku.domain.training.Lessons.all.indexOfFirst { it.premium }
        assertTrue(com.uk2solutions.sudoku.domain.training.Lessons.all.drop(firstPremium).all { it.premium })
        // 난이도(등급)가 앞에서 뒤로 단조 증가
        val levels = com.uk2solutions.sudoku.domain.training.Lessons.all.map { it.technique.level }
        assertEquals(levels.sorted(), levels)
    }
}

class SkillRankTest {
    private fun scores(vararg ids: String, score: Int = 3) = ids.associateWith { score }

    @Test fun ranksRiseWithMasteredLessons() {
        val r = com.uk2solutions.sudoku.domain.training.SkillRanks
        assertEquals(com.uk2solutions.sudoku.domain.training.Rank.ROOKIE, r.of(emptyMap()).rank)
        assertEquals(com.uk2solutions.sudoku.domain.training.Rank.APPRENTICE, r.of(scores("rules", "naked_single")).rank)
        assertEquals("별 1개는 마스터가 아님", com.uk2solutions.sudoku.domain.training.Rank.ROOKIE, r.of(scores("rules", "naked_single", score = 1)).rank)
        val free = scores("rules", "naked_single", "hidden_single", "pointing", "claiming", "naked_pair")
        val info = r.of(free)
        assertEquals(com.uk2solutions.sudoku.domain.training.Rank.SOLVER, info.rank)
        assertTrue("무료 레슨을 다 하면 다음 단계는 프리미엄이 필요", info.nextNeedsPremium)
        assertEquals(3, info.toNext)
        val all = com.uk2solutions.sudoku.domain.training.Lessons.all.associate { it.id to 3 }
        val top = r.of(all)
        assertEquals(com.uk2solutions.sudoku.domain.training.Rank.GRANDMASTER, top.rank); assertNull(top.next); assertEquals(1f, top.progress, 0f)
    }

    @Test fun earlyRanksDoNotNeedPremium() {
        val info = com.uk2solutions.sudoku.domain.training.SkillRanks.of(scores("rules"))
        assertFalse(info.nextNeedsPremium)
    }

    @Test fun hintOnExpertPuzzleNamesTheNeededTechnique() {
        val gen = PuzzleGenerator()
        var withLearn = 0
        for (seed in 1L..10L) {
            val p = gen.generate(com.uk2solutions.sudoku.domain.model.Difficulty.EXPERT, seed)
            var s = com.uk2solutions.sudoku.domain.game.GameRules.newState(p.givens, p.solution)
            // 사람 기법으로 단일을 모두 채워 단일 힌트가 없는 상태까지 진행
            while (true) {
                val h = com.uk2solutions.sudoku.domain.game.Hints.next(s) ?: break
                if (h.technique == com.uk2solutions.sudoku.domain.game.HintTechnique.DEDUCTION) {
                    if (h.learn != null) { withLearn++; assertTrue(h.learn!!.level >= 2) }
                    break
                }
                s = com.uk2solutions.sudoku.domain.game.GameRules.enter(s, h.index, h.digit, false, false).first
            }
        }
        assertTrue("전문가 퍼즐에서 힌트가 필요한 기술을 알려 줘야 함", withLearn > 0)
    }
}
