package com.uk2solutions.sudoku.domain.game

import com.uk2solutions.sudoku.domain.model.Difficulty
import com.uk2solutions.sudoku.domain.model.Units

/**
 * 점수는 저장하지 않고 현재 상태에서 계산한다(지우고 다시 넣어 점수를 불리는 꼼수 방지).
 * (맞게 채운 칸×10 + 새로 완성된 행·열·박스×50 − 실수×30 − 힌트×10) × 난이도 배수, 최소 0.
 */
object Scoring {
    fun multiplier(d: Difficulty) = d.ordinal + 1

    fun score(s: GameState, d: Difficulty): Int {
        val correct = (0 until 81).count { !s.isGiven(it) && s.values[it] == s.solution[it] }
        val unitsNow = Units.all.count { u -> u.all { s.values[it] == s.solution[it] } }
        val unitsAtStart = Units.all.count { u -> u.all { s.givens[it] != 0 } }
        val raw = correct * 10 + (unitsNow - unitsAtStart) * 50 - s.mistakes * 30 - s.hintsUsed * 10
        return (raw * multiplier(d)).coerceAtLeast(0)
    }

    /** 사용자가 채워야 할 빈칸 수와 현재 채운 수(진행 표시 "n/m"용). */
    fun progress(s: GameState): Pair<Int, Int> {
        val total = s.givens.count { it == 0 }
        val filled = (0 until 81).count { s.givens[it] == 0 && s.values[it] != 0 }
        return filled to total
    }
}
