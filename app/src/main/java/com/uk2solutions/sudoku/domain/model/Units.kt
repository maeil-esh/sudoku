package com.uk2solutions.sudoku.domain.model

/** 행·열·박스 인덱스 표와 이웃(peer) 표. */
object Units {
    val rows: Array<IntArray> = Array(9) { r -> IntArray(9) { c -> r * 9 + c } }
    val cols: Array<IntArray> = Array(9) { c -> IntArray(9) { r -> r * 9 + c } }
    val boxes: Array<IntArray> = Array(9) { b ->
        IntArray(9) { k -> ((b / 3) * 3 + k / 3) * 9 + (b % 3) * 3 + k % 3 }
    }
    val all: Array<IntArray> = rows + cols + boxes

    /** 각 칸과 같은 행·열·박스에 속한 다른 20칸. */
    val peers: Array<IntArray> = Array(81) { i ->
        val set = LinkedHashSet<Int>()
        for (u in listOf(rows[Board.row(i)], cols[Board.col(i)], boxes[Board.box(i)])) {
            for (j in u) if (j != i) set.add(j)
        }
        set.toIntArray()
    }
}
