package com.uk2solutions.sudoku.domain.game

/** 한 번의 이동(입력·지우기·메모)이 바꾼 칸들의 이전 상태. 되돌리기에 사용. */
data class CellChange(val index: Int, val prevValue: Int, val prevNotes: Int)
data class Move(val changes: List<CellChange>)

/** 진행 중인 게임의 불변 스냅샷. 배열은 변경하지 않고 복사해서 새 상태를 만든다. */
data class GameState(
    val givens: IntArray,
    val solution: IntArray,
    val values: IntArray,
    val notes: IntArray,
    val mistakes: Int = 0,
    val elapsedMs: Long = 0,
    val hintsUsed: Int = 0,
    val undo: List<Move> = emptyList(),
) {
    fun isGiven(i: Int) = givens[i] != 0
}
