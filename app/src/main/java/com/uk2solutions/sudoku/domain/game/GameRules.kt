package com.uk2solutions.sudoku.domain.game

import com.uk2solutions.sudoku.domain.model.Board
import com.uk2solutions.sudoku.domain.model.Units

enum class InputResult { NONE, FILLED, MISTAKE, NOTE_TOGGLED, ERASED }

/** SPEC §5.1 입력 규칙. 순수 함수만 둔다. */
object GameRules {
    const val MAX_UNDO = 200

    fun newState(givens: String, solution: String): GameState {
        val g = Board.parse(givens)
        return GameState(g, Board.parse(solution), g.copyOf(), IntArray(Board.SIZE))
    }

    fun enter(s: GameState, i: Int, d: Int, noteMode: Boolean, autoRemoveNotes: Boolean): Pair<GameState, InputResult> {
        require(d in 1..9)
        if (s.isGiven(i)) return s to InputResult.NONE
        val values = s.values.copyOf()
        val notes = s.notes.copyOf()
        val changes = LinkedHashMap<Int, CellChange>()
        fun record(j: Int) { changes.getOrPut(j) { CellChange(j, s.values[j], s.notes[j]) } }

        if (noteMode) {
            if (s.values[i] != 0) return s to InputResult.NONE
            record(i)
            notes[i] = notes[i] xor (1 shl (d - 1))
            return s.withMove(values, notes, changes.values.toList(), s.mistakes) to InputResult.NOTE_TOGGLED
        }
        if (s.values[i] == d) return s to InputResult.NONE
        record(i)
        values[i] = d
        notes[i] = 0
        val mistake = d != s.solution[i]
        if (autoRemoveNotes && !mistake) {
            val bit = 1 shl (d - 1)
            for (p in Units.peers[i]) if (notes[p] and bit != 0) { record(p); notes[p] = notes[p] and bit.inv() }
        }
        val next = s.withMove(values, notes, changes.values.toList(), s.mistakes + if (mistake) 1 else 0)
        return next to if (mistake) InputResult.MISTAKE else InputResult.FILLED
    }

    fun erase(s: GameState, i: Int): Pair<GameState, InputResult> {
        if (s.isGiven(i) || (s.values[i] == 0 && s.notes[i] == 0)) return s to InputResult.NONE
        val values = s.values.copyOf(); val notes = s.notes.copyOf()
        values[i] = 0; notes[i] = 0
        val change = CellChange(i, s.values[i], s.notes[i])
        return s.withMove(values, notes, listOf(change), s.mistakes) to InputResult.ERASED
    }

    /** 가장 최근 이동을 되돌린다. 실수 횟수는 되돌리지 않는다. */
    fun undo(s: GameState): GameState {
        val move = s.undo.lastOrNull() ?: return s
        val values = s.values.copyOf(); val notes = s.notes.copyOf()
        for (c in move.changes) { values[c.index] = c.prevValue; notes[c.index] = c.prevNotes }
        return s.copy(values = values, notes = notes, undo = s.undo.dropLast(1))
    }

    /** 숫자 패드의 "남은 개수". */
    fun remaining(s: GameState, d: Int) = 9 - s.values.count { it == d }

    fun isComplete(s: GameState) = s.values.contentEquals(s.solution)

    /** 같은 행·열·박스에 같은 숫자가 둘 이상 있는 칸들(규칙 충돌). */
    fun conflicts(values: IntArray): Set<Int> {
        val out = HashSet<Int>()
        for (u in Units.all) {
            for (d in 1..9) {
                val cells = u.filter { values[it] == d }
                if (cells.size > 1) out.addAll(cells)
            }
        }
        return out
    }

    /** 채워진 칸 비율(0..100). */
    fun progressPercent(values: IntArray) = values.count { it != 0 } * 100 / Board.SIZE

    private fun GameState.withMove(values: IntArray, notes: IntArray, changes: List<CellChange>, mistakes: Int) =
        copy(
            values = values, notes = notes, mistakes = mistakes,
            undo = (undo + Move(changes)).takeLast(MAX_UNDO),
        )
}
