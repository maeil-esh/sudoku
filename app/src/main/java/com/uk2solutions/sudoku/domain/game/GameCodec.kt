package com.uk2solutions.sudoku.domain.game

/** 저장용 직렬화(SPEC §5.1). 이동 하나 = "idx:값:메모;..." , 이동 사이는 "|". */
object GameCodec {
    fun encodeInts(a: IntArray) = a.joinToString(",")
    fun decodeInts(s: String): IntArray = if (s.isEmpty()) IntArray(0) else s.split(",").map { it.toInt() }.toIntArray()

    fun encodeUndo(moves: List<Move>): String =
        moves.joinToString("|") { m -> m.changes.joinToString(";") { "${it.index}:${it.prevValue}:${it.prevNotes}" } }

    fun decodeUndo(s: String): List<Move> =
        if (s.isEmpty()) emptyList() else s.split("|").map { mv ->
            Move(mv.split(";").map { c ->
                val p = c.split(":")
                CellChange(p[0].toInt(), p[1].toInt(), p[2].toInt())
            })
        }
}
