package com.uk2solutions.sudoku.domain.game

import com.uk2solutions.sudoku.domain.model.Units

/** 이번 입력으로 새로 완성된 행·열·박스의 칸들, 그리고 9개가 모두 맞게 채워진 숫자. */
data class Completion(val cells: Set<Int>, val digit: Int?)

object Completions {
    /** [prev]→[next]로 [index]에 값을 넣었을 때 새로 생긴 완성. 없으면 null. */
    fun between(prev: GameState, next: GameState, index: Int): Completion? {
        val d = next.values[index]
        if (d == 0 || d != next.solution[index]) return null
        val cells = HashSet<Int>()
        for (u in Units.all) {
            if (index !in u) continue
            if (u.all { next.values[it] == next.solution[it] } && !u.all { prev.values[it] == prev.solution[it] }) cells.addAll(u.toList())
        }
        fun correct(s: GameState) = (0 until 81).count { s.values[it] == d && s.solution[it] == d }
        val digit = if (correct(next) == 9 && correct(prev) < 9) d else null
        return if (cells.isEmpty() && digit == null) null else Completion(cells, digit)
    }
}
