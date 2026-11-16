package com.uk2solutions.sudoku.domain.solver

import com.uk2solutions.sudoku.domain.generator.SeededRandom
import com.uk2solutions.sudoku.domain.model.Board

/** 비트마스크 역추적 풀이기. 유일해 판정(해 개수 세기)과 무작위 완성 격자 생성에 사용. */
object Solver {
    private const val ALL = 0x1FF

    /** 해를 최대 [limit]개까지 센다. 입력이 규칙을 이미 위반하면 0. */
    fun countSolutions(grid: IntArray, limit: Int = 2): Int {
        val st = State.from(grid) ?: return 0
        return st.search(limit, null, null)
    }

    /** 첫 번째 해를 반환(없으면 null). */
    fun solve(grid: IntArray): IntArray? {
        val st = State.from(grid) ?: return null
        val out = arrayOfNulls<IntArray>(1)
        st.search(1, out, null)
        return out[0]
    }

    /** 빈 격자에서 [rnd]로 무작위 완성 격자를 만든다. */
    fun randomFilledGrid(rnd: SeededRandom): IntArray {
        val st = State.from(IntArray(Board.SIZE))!!
        val out = arrayOfNulls<IntArray>(1)
        st.search(1, out, rnd)
        return out[0]!!
    }

    private class State(val v: IntArray, val rowM: IntArray, val colM: IntArray, val boxM: IntArray) {
        fun cand(i: Int) = ALL and (rowM[i / 9] or colM[i % 9] or boxM[Board.box(i)]).inv()

        fun set(i: Int, d: Int) {
            val b = 1 shl (d - 1)
            v[i] = d
            rowM[i / 9] = rowM[i / 9] or b
            colM[i % 9] = colM[i % 9] or b
            boxM[Board.box(i)] = boxM[Board.box(i)] or b
        }

        fun unset(i: Int, d: Int) {
            val b = (1 shl (d - 1)).inv()
            v[i] = 0
            rowM[i / 9] = rowM[i / 9] and b
            colM[i % 9] = colM[i % 9] and b
            boxM[Board.box(i)] = boxM[Board.box(i)] and b
        }

        /** 후보가 가장 적은 빈칸(MRV)을 고른다. 빈칸이 없으면 -1, 후보 0인 칸이 있으면 -2. */
        fun pick(): Int {
            var best = -1
            var bestN = 10
            for (i in 0 until 81) {
                if (v[i] != 0) continue
                val n = Integer.bitCount(cand(i))
                if (n == 0) return -2
                if (n < bestN) { bestN = n; best = i; if (n == 1) break }
            }
            return best
        }

        fun search(limit: Int, out: Array<IntArray?>?, rnd: SeededRandom?): Int {
            val i = pick()
            if (i == -2) return 0
            if (i == -1) {
                if (out != null && out[0] == null) out[0] = v.copyOf()
                return 1
            }
            val digits = IntArray(9) { it + 1 }
            if (rnd != null) rnd.shuffle(digits)
            val c = cand(i)
            var found = 0
            for (d in digits) {
                if (c and (1 shl (d - 1)) == 0) continue
                set(i, d)
                found += search(limit - found, out, rnd)
                unset(i, d)
                if (found >= limit) break
            }
            return found
        }

        companion object {
            fun from(grid: IntArray): State? {
                val st = State(IntArray(81), IntArray(9), IntArray(9), IntArray(9))
                for (i in 0 until 81) {
                    val d = grid[i]
                    if (d == 0) continue
                    if (st.cand(i) and (1 shl (d - 1)) == 0) return null
                    st.set(i, d)
                }
                return st
            }
        }
    }
}
