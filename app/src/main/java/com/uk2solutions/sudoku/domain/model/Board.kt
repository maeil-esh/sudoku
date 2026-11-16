package com.uk2solutions.sudoku.domain.model

/** 81칸 보드(0=빈칸)의 직렬화·위치 유틸. 순수 Kotlin. */
object Board {
    const val SIZE = 81

    fun parse(s: String): IntArray {
        require(s.length == SIZE) { "보드 문자열은 81자여야 합니다: ${s.length}" }
        return IntArray(SIZE) { i ->
            val c = s[i]
            require(c in '0'..'9') { "잘못된 문자: $c" }
            c - '0'
        }
    }

    fun format(cells: IntArray): String {
        require(cells.size == SIZE)
        return buildString(SIZE) { cells.forEach { append(('0' + it)) } }
    }

    fun row(i: Int) = i / 9
    fun col(i: Int) = i % 9
    fun box(i: Int) = (i / 27) * 3 + (i % 9) / 3

    fun clueCount(cells: IntArray) = cells.count { it != 0 }
}
