package com.uk2solutions.sudoku.domain.solver

import com.uk2solutions.sudoku.domain.model.Board
import com.uk2solutions.sudoku.domain.model.Units

/**
 * 사람 기법 기반 난이도 평가기.
 * 등급 1: 단일 후보, 숨은 단일 / 등급 2: + 포인팅·박스-라인 축소, 노출 쌍.
 * (등급 3 이상은 M3에서 확장)
 */
class TechniqueSolver {
    /**
     * [grid]를 [maxLevel] 이하 기법만으로 완전히 풀 수 있으면 실제로 필요했던 최대 등급(1 이상)을,
     * 풀 수 없으면 null을 반환한다.
     */
    fun requiredLevel(grid: IntArray, maxLevel: Int): Int? {
        val v = grid.copyOf()
        val cand = IntArray(81)
        for (i in 0 until 81) {
            if (v[i] != 0) continue
            var m = 0x1FF
            for (p in Units.peers[i]) if (v[p] != 0) m = m and (1 shl (v[p] - 1)).inv()
            if (m == 0) return null
            cand[i] = m
        }
        var level = 1

        fun place(i: Int, d: Int) {
            v[i] = d
            cand[i] = 0
            val clear = (1 shl (d - 1)).inv()
            for (p in Units.peers[i]) cand[p] = cand[p] and clear
        }

        while (true) {
            var progress = false
            for (i in 0 until 81) {
                if (v[i] == 0 && Integer.bitCount(cand[i]) == 1) {
                    place(i, Integer.numberOfTrailingZeros(cand[i]) + 1); progress = true
                }
            }
            if (!progress) {
                for (u in Units.all) {
                    for (d in 0 until 9) {
                        val bit = 1 shl d
                        var cnt = 0; var at = -1
                        for (i in u) if (cand[i] and bit != 0) { cnt++; at = i }
                        if (cnt == 1) { place(at, d + 1); progress = true }
                    }
                }
            }
            if (progress) continue
            if (v.none { it == 0 }) return level
            if (maxLevel >= 2 && (pointingClaiming(cand) || nakedPair(cand))) {
                level = maxOf(level, 2); continue
            }
            return null
        }
    }

    /** 포인팅(박스→행/열)과 박스-라인 축소(행/열→박스). 제거가 있었으면 true. */
    private fun pointingClaiming(cand: IntArray): Boolean {
        var changed = false
        for (d in 0 until 9) {
            val bit = 1 shl d
            // 박스 안의 후보가 한 행/열에만 있으면 그 줄의 박스 밖 후보 제거
            for (b in 0 until 9) {
                val cells = Units.boxes[b].filter { cand[it] and bit != 0 }
                if (cells.size < 2) continue
                val line: IntArray? = when {
                    cells.all { Board.row(it) == Board.row(cells[0]) } -> Units.rows[Board.row(cells[0])]
                    cells.all { Board.col(it) == Board.col(cells[0]) } -> Units.cols[Board.col(cells[0])]
                    else -> null
                }
                if (line != null) for (i in line) {
                    if (Board.box(i) != b && cand[i] and bit != 0) { cand[i] = cand[i] and bit.inv(); changed = true }
                }
            }
            // 행/열 안의 후보가 한 박스에만 있으면 그 박스의 줄 밖 후보 제거
            for (line in Units.rows + Units.cols) {
                val cells = line.filter { cand[it] and bit != 0 }
                if (cells.size < 2) continue
                val b = Board.box(cells[0])
                if (!cells.all { Board.box(it) == b }) continue
                for (i in Units.boxes[b]) {
                    if (i !in line && cand[i] and bit != 0) { cand[i] = cand[i] and bit.inv(); changed = true }
                }
            }
        }
        return changed
    }

    /** 노출 쌍: 한 유닛에서 같은 2후보를 가진 두 칸이 있으면 그 유닛의 다른 칸에서 두 값을 제거. */
    private fun nakedPair(cand: IntArray): Boolean {
        var changed = false
        for (u in Units.all) {
            for (a in 0 until 9) {
                val ca = cand[u[a]]
                if (Integer.bitCount(ca) != 2) continue
                for (b in a + 1 until 9) {
                    if (cand[u[b]] != ca) continue
                    for (k in 0 until 9) {
                        if (k == a || k == b) continue
                        if (cand[u[k]] and ca != 0) { cand[u[k]] = cand[u[k]] and ca.inv(); changed = true }
                    }
                }
            }
        }
        return changed
    }
}
