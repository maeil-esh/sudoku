package com.uk2solutions.sudoku.domain.game

import com.uk2solutions.sudoku.domain.model.Units

object HintPolicy { const val FREE_PER_DAY = 3 }

enum class HintTechnique { NAKED_SINGLE, HIDDEN_SINGLE, DEDUCTION }

data class Hint(val index: Int, val digit: Int, val technique: HintTechnique)

/** SPEC §5.4 힌트. 정답과 다른 입력은 빈칸으로 보고, 다음에 풀 수 있는 칸을 찾는다. */
object Hints {
    fun next(s: GameState, preferred: Int = -1): Hint? {
        val v = IntArray(81) { if (s.values[it] == s.solution[it]) s.values[it] else 0 }
        if (v.none { it == 0 }) return null
        val cand = IntArray(81)
        for (i in 0 until 81) if (v[i] == 0) {
            var m = 0x1FF
            for (p in Units.peers[i]) if (v[p] != 0) m = m and (1 shl (v[p] - 1)).inv()
            cand[i] = m
        }
        for (i in 0 until 81) if (v[i] == 0 && Integer.bitCount(cand[i]) == 1)
            return Hint(i, s.solution[i], HintTechnique.NAKED_SINGLE)
        for (u in Units.all) for (d in 0 until 9) {
            val cells = u.filter { cand[it] and (1 shl d) != 0 }
            if (cells.size == 1) return Hint(cells[0], s.solution[cells[0]], HintTechnique.HIDDEN_SINGLE)
        }
        val i = if (preferred >= 0 && v[preferred] == 0) preferred else v.indexOfFirst { it == 0 }
        return Hint(i, s.solution[i], HintTechnique.DEDUCTION)
    }
}
