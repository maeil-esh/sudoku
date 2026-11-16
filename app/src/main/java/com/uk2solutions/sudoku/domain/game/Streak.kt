package com.uk2solutions.sudoku.domain.game

import java.time.LocalDate

object Streak {
    /** 오늘 또는 어제까지 이어진 연속 완료 일수. 오늘 아직 안 했어도 어제까지의 기록은 유지. */
    fun current(done: Set<LocalDate>, today: LocalDate): Int {
        var d = if (today in done) today else today.minusDays(1)
        var n = 0
        while (d in done) { n++; d = d.minusDays(1) }
        return n
    }
}
