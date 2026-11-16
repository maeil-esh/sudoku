package com.uk2solutions.sudoku.domain

import com.uk2solutions.sudoku.domain.game.*
import com.uk2solutions.sudoku.domain.generator.PuzzleGenerator
import com.uk2solutions.sudoku.domain.model.Difficulty
import org.junit.Assert.*
import org.junit.Test

class GameRulesTest {
    private val p = PuzzleGenerator().generate(Difficulty.EASY, 11L)
    private fun fresh() = GameRules.newState(p.givens, p.solution)
    private fun firstEmpty(s: GameState) = s.values.indexOfFirst { it == 0 }
    private fun wrongDigit(s: GameState, i: Int) = (1..9).first { it != s.solution[i] }

    @Test fun correctInputFillsAndClearsNotes() {
        val s = fresh(); val i = firstEmpty(s)
        val (n, r) = GameRules.enter(s, i, s.solution[i], false, true)
        assertEquals(InputResult.FILLED, r)
        assertEquals(s.solution[i], n.values[i]); assertEquals(0, n.mistakes)
    }

    @Test fun wrongInputCountsMistake() {
        val s = fresh(); val i = firstEmpty(s)
        val (n, r) = GameRules.enter(s, i, wrongDigit(s, i), false, true)
        assertEquals(InputResult.MISTAKE, r); assertEquals(1, n.mistakes)
    }

    @Test fun givensAreImmutable() {
        val s = fresh(); val g = s.givens.indexOfFirst { it != 0 }
        assertEquals(InputResult.NONE, GameRules.enter(s, g, 1, false, true).second)
        assertEquals(InputResult.NONE, GameRules.erase(s, g).second)
    }

    @Test fun noteToggleAndAutoRemoval() {
        var s = fresh(); val i = firstEmpty(s)
        val peer = com.uk2solutions.sudoku.domain.model.Units.peers[i].first { s.values[it] == 0 }
        val d = s.solution[i]
        s = GameRules.enter(s, peer, d, true, true).first          // 메모 켬
        assertTrue(s.notes[peer] and (1 shl (d - 1)) != 0)
        s = GameRules.enter(s, i, d, false, true).first            // 확정 → 메모 자동 삭제
        assertEquals(0, s.notes[peer] and (1 shl (d - 1)))
        s = GameRules.undo(s)                                      // 되돌리면 이웃 메모도 복구
        assertTrue(s.notes[peer] and (1 shl (d - 1)) != 0)
        assertEquals(0, s.values[i])
    }

    @Test fun undoDoesNotRestoreMistakes() {
        var s = fresh(); val i = firstEmpty(s)
        s = GameRules.enter(s, i, wrongDigit(s, i), false, true).first
        s = GameRules.undo(s)
        assertEquals(0, s.values[i]); assertEquals(1, s.mistakes)
    }

    @Test fun undoStackCappedAt200() {
        var s = fresh(); val i = firstEmpty(s)
        repeat(250) { n -> s = GameRules.enter(s, i, (n % 9) + 1, true, true).first }
        assertEquals(200, s.undo.size)
    }

    @Test fun remainingCountAndComplete() {
        var s = fresh()
        val d = 5
        assertEquals(9 - s.values.count { it == d }, GameRules.remaining(s, d))
        assertFalse(GameRules.isComplete(s))
        for (i in 0 until 81) if (s.values[i] == 0) s = GameRules.enter(s, i, s.solution[i], false, false).first
        assertTrue(GameRules.isComplete(s))
        assertEquals(0, GameRules.remaining(s, d)); assertEquals(100, GameRules.progressPercent(s.values))
    }

    @Test fun conflictsDetectDuplicates() {
        val s = fresh(); val i = firstEmpty(s)
        val peer = com.uk2solutions.sudoku.domain.model.Units.peers[i].first { s.values[it] != 0 }
        val v = s.values.copyOf(); v[i] = v[peer]
        assertTrue(GameRules.conflicts(v).containsAll(listOf(i, peer)))
        assertTrue(GameRules.conflicts(s.values).isEmpty())
    }

    @Test fun codecRoundTripKeepsState() {
        var s = fresh(); val i = firstEmpty(s)
        s = GameRules.enter(s, i, s.solution[i], false, true).first
        s = GameRules.enter(s, s.values.indexOfFirst { it == 0 }, 3, true, true).first
        val back = GameCodec.decodeUndo(GameCodec.encodeUndo(s.undo))
        assertEquals(s.undo, back)
        assertArrayEquals(s.notes, GameCodec.decodeInts(GameCodec.encodeInts(s.notes)))
        assertEquals(emptyList<Move>(), GameCodec.decodeUndo(""))
    }
}
