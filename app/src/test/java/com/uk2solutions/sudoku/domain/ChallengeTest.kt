package com.uk2solutions.sudoku.domain

import com.uk2solutions.sudoku.domain.challenge.*
import com.uk2solutions.sudoku.domain.game.GameRules
import com.uk2solutions.sudoku.domain.generator.PuzzleGenerator
import com.uk2solutions.sudoku.domain.model.Difficulty
import org.junit.Assert.*
import org.junit.Test

class ChallengeTest {
    private val puzzle = PuzzleGenerator().generate(Difficulty.MEDIUM, 21L)
    private val result = ChallengeResult(timeSec = 725, mistakes = 1, hints = 0, score = 2340)

    @Test fun roundTripWithResult() {
        val code = ChallengeCode.encode(Challenge(Difficulty.MEDIUM, puzzle.givens, result))
        println("코드 길이 ${code.length}: $code")
        val back = ChallengeCode.decode(code)!!
        assertEquals(puzzle.givens, back.givens); assertEquals(Difficulty.MEDIUM, back.difficulty); assertEquals(result, back.result)
    }

    @Test fun roundTripWithoutResult() {
        val back = ChallengeCode.decode(ChallengeCode.encode(Challenge(Difficulty.EASY, puzzle.givens, null)))!!
        assertNull(back.result); assertEquals(puzzle.givens, back.givens)
    }

    @Test fun findsCodeInsideShareText() {
        val code = ChallengeCode.encode(Challenge(Difficulty.MEDIUM, puzzle.givens, result))
        val text = "스도쿠 도전장! 내 기록 12:05 · 점수 2340\n코드를 붙여 넣으세요:\n$code\n감사합니다"
        assertTrue(ChallengeCode.containsCode(text)); assertNotNull(ChallengeCode.decode(text))
    }

    @Test fun solutionIsRecoverable() {
        val c = ChallengeCode.decode(ChallengeCode.encode(Challenge(Difficulty.MEDIUM, puzzle.givens, result)))!!
        val s = GameRules.newState(c.givens, puzzle.solution)
        assertTrue(com.uk2solutions.sudoku.domain.solver.Solver.solve(s.givens)!!.contentEquals(s.solution))
    }

    @Test fun rejectsTamperedOrGarbage() {
        val code = ChallengeCode.encode(Challenge(Difficulty.MEDIUM, puzzle.givens, result))
        val i = code.length - 10
        val flipped = code.substring(0, i) + (if (code[i] == 'A') 'B' else 'A') + code.substring(i + 1)
        assertNull(ChallengeCode.decode(flipped))
        assertNull(ChallengeCode.decode(code.dropLast(5)))
        assertNull(ChallengeCode.decode("SDK1-nonsense"))
        assertNull(ChallengeCode.decode(null)); assertNull(ChallengeCode.decode("hello"))
    }

    @Test fun outcomeRules() {
        val a = ChallengeResult(600, 0, 0, 2000)
        assertEquals(Outcome.WIN, ChallengeCompare.outcome(a.copy(score = 2100), a))
        assertEquals(Outcome.LOSE, ChallengeCompare.outcome(a.copy(score = 1900), a))
        assertEquals(Outcome.WIN, ChallengeCompare.outcome(a.copy(timeSec = 500), a))   // 점수 같으면 빠른 쪽
        assertEquals(Outcome.LOSE, ChallengeCompare.outcome(a.copy(timeSec = 700), a))
        assertEquals(Outcome.DRAW, ChallengeCompare.outcome(a, a))
    }

    @Test fun wrongCellsBlockUntilCleared() {
        var s = GameRules.newState(puzzle.givens, puzzle.solution)
        assertTrue(GameRules.wrongCells(s).isEmpty())
        val i = s.values.indexOfFirst { it == 0 }
        s = GameRules.enter(s, i, (1..9).first { it != s.solution[i] }, false, true).first
        assertEquals(setOf(i), GameRules.wrongCells(s))
        s = GameRules.erase(s, i).first
        assertTrue(GameRules.wrongCells(s).isEmpty()); assertEquals(0, s.values[i])
        assertEquals(1, s.mistakes) // 지워도 실수 횟수는 남는다
    }
}
