package com.uk2solutions.sudoku.domain.technique

import com.uk2solutions.sudoku.domain.model.Board
import com.uk2solutions.sudoku.domain.model.Units

/** 각 기법의 탐색기. 지울 후보가 실제로 있는(쓸모 있는) 추론만 돌려준다. */
object Finders {
    /** 단순 → 복잡 순서. 사람이 푸는 순서이자 등급 판정 순서. */
    val ORDER = Technique.entries.toList()

    fun find(g: CandGrid, t: Technique): Deduction? = when (t) {
        Technique.NAKED_SINGLE -> nakedSingle(g)
        Technique.HIDDEN_SINGLE -> hiddenSingle(g)
        Technique.POINTING -> pointing(g)
        Technique.CLAIMING -> claiming(g)
        Technique.NAKED_PAIR -> nakedSubset(g, 2, Technique.NAKED_PAIR)
        Technique.NAKED_TRIPLE -> nakedSubset(g, 3, Technique.NAKED_TRIPLE)
        Technique.HIDDEN_PAIR -> hiddenSubset(g, 2, Technique.HIDDEN_PAIR)
        Technique.HIDDEN_TRIPLE -> hiddenSubset(g, 3, Technique.HIDDEN_TRIPLE)
        Technique.X_WING -> fish(g, 2, Technique.X_WING)
        Technique.SWORDFISH -> fish(g, 3, Technique.SWORDFISH)
        Technique.Y_WING -> yWing(g)
    }

    fun nakedSingle(g: CandGrid): Deduction? {
        for (i in 0 until 81) if (g.v[i] == 0 && g.count(i) == 1) {
            val d = g.digits(i)[0]
            return Deduction(Technique.NAKED_SINGLE, setOf(i), listOf(d), Units.peers[i].toSet() + i, place = i to d)
        }
        return null
    }

    fun hiddenSingle(g: CandGrid): Deduction? {
        for (u in Units.all) for (d in 1..9) {
            val cells = u.filter { g.has(it, d) }
            if (cells.size == 1) return Deduction(Technique.HIDDEN_SINGLE, setOf(cells[0]), listOf(d), u.toSet(), place = cells[0] to d)
        }
        return null
    }

    fun pointing(g: CandGrid): Deduction? {
        for (b in 0 until 9) for (d in 1..9) {
            val cells = Units.boxes[b].filter { g.has(it, d) }
            if (cells.size < 2) continue
            val line = when {
                cells.all { Board.row(it) == Board.row(cells[0]) } -> Units.rows[Board.row(cells[0])]
                cells.all { Board.col(it) == Board.col(cells[0]) } -> Units.cols[Board.col(cells[0])]
                else -> continue
            }
            val el = line.filter { Board.box(it) != b && g.has(it, d) }.map { it to d }
            if (el.isNotEmpty()) return Deduction(Technique.POINTING, cells.toSet(), listOf(d), Units.boxes[b].toSet() + line.toList(), eliminations = el)
        }
        return null
    }

    fun claiming(g: CandGrid): Deduction? {
        for (line in Units.rows + Units.cols) for (d in 1..9) {
            val cells = line.filter { g.has(it, d) }
            if (cells.size < 2) continue
            val b = Board.box(cells[0])
            if (!cells.all { Board.box(it) == b }) continue
            val el = Units.boxes[b].filter { it !in line && g.has(it, d) }.map { it to d }
            if (el.isNotEmpty()) return Deduction(Technique.CLAIMING, cells.toSet(), listOf(d), Units.boxes[b].toSet() + line.toList(), eliminations = el)
        }
        return null
    }

    private fun combos(n: Int, k: Int): List<List<Int>> {
        val out = ArrayList<List<Int>>()
        fun rec(start: Int, cur: List<Int>) { if (cur.size == k) { out += cur; return }; for (i in start until n) rec(i + 1, cur + i) }
        rec(0, emptyList()); return out
    }

    /** 노출 쌍/삼중: k칸의 후보 합집합이 k개 숫자 → 같은 단위 다른 칸에서 그 숫자 제거. */
    fun nakedSubset(g: CandGrid, k: Int, t: Technique): Deduction? {
        for (u in Units.all) {
            val cs = u.filter { g.v[it] == 0 && g.count(it) in 2..k }
            for (combo in combos(cs.size, k)) {
                val cells = combo.map { cs[it] }
                val union = cells.fold(0) { a, i -> a or g.c[i] }
                if (Integer.bitCount(union) != k) continue
                val el = u.filter { it !in cells && g.v[it] == 0 }.flatMap { i -> (1..9).filter { union and (1 shl (it - 1)) != 0 && g.has(i, it) }.map { d -> i to d } }
                if (el.isNotEmpty()) return Deduction(t, cells.toSet(), (1..9).filter { union and (1 shl (it - 1)) != 0 }, u.toSet(), eliminations = el)
            }
        }
        return null
    }

    /** 숨은 쌍/삼중: k개 숫자가 정확히 k칸에만 들어감 → 그 칸들의 다른 후보 제거. */
    fun hiddenSubset(g: CandGrid, k: Int, t: Technique): Deduction? {
        for (u in Units.all) {
            val ds = (1..9).filter { d -> u.count { g.has(it, d) } in 1..k }
            for (combo in combos(ds.size, k)) {
                val digs = combo.map { ds[it] }
                val cells = u.filter { i -> digs.any { g.has(i, it) } }
                if (cells.size != k) continue
                val el = cells.flatMap { i -> g.digits(i).filter { it !in digs }.map { d -> i to d } }
                if (el.isNotEmpty()) return Deduction(t, cells.toSet(), digs, u.toSet(), eliminations = el)
            }
        }
        return null
    }

    /** X-윙(k=2)·스워드피시(k=3): 같은 숫자가 k개 행(열)에서 k개 열(행)에만 있음 → 그 열(행)의 다른 칸에서 제거. */
    fun fish(g: CandGrid, k: Int, t: Technique): Deduction? {
        for (d in 1..9) for (byRow in listOf(true, false)) {
            val lines = if (byRow) Units.rows else Units.cols
            val cover = if (byRow) Units.cols else Units.rows
            fun covIdx(cell: Int) = if (byRow) Board.col(cell) else Board.row(cell)
            val cand = (0 until 9).filter { li -> lines[li].count { g.has(it, d) } in 2..k }
            for (combo in combos(cand.size, k)) {
                val ls = combo.map { cand[it] }
                val covers = ls.flatMap { li -> lines[li].filter { g.has(it, d) }.map(::covIdx) }.toSet()
                if (covers.size != k) continue
                val patternCells = ls.flatMap { li -> lines[li].filter { g.has(it, d) } }.toSet()
                val el = covers.flatMap { ci -> cover[ci].filter { (if (byRow) Board.row(it) else Board.col(it)) !in ls && g.has(it, d) } }.map { it to d }
                if (el.isNotEmpty()) {
                    val houses = (ls.flatMap { lines[it].toList() } + covers.flatMap { cover[it].toList() }).toSet()
                    return Deduction(t, patternCells, listOf(d), houses, eliminations = el)
                }
            }
        }
        return null
    }

    /** Y-윙: 피벗{A,B}, 날개1{A,C}, 날개2{B,C} (둘 다 피벗을 봄) → 두 날개가 모두 보는 칸에서 C 제거. */
    fun yWing(g: CandGrid): Deduction? {
        for (pv in 0 until 81) {
            if (g.v[pv] != 0 || g.count(pv) != 2) continue
            val (a, b) = g.digits(pv)
            val wings = Units.peers[pv].filter { g.v[it] == 0 && g.count(it) == 2 }
            for (w1 in wings) {
                val d1 = g.digits(w1)
                if (a !in d1 || b in d1) continue
                val cdig = d1.first { it != a }
                for (w2 in wings) {
                    if (w2 == w1) continue
                    val d2 = g.digits(w2)
                    if (b !in d2 || a in d2 || cdig !in d2) continue
                    val el = Units.peers[w1].filter { it in Units.peers[w2] && it != pv && g.has(it, cdig) }.map { it to cdig }
                    if (el.isNotEmpty()) return Deduction(Technique.Y_WING, setOf(pv, w1, w2), listOf(a, b, cdig), (Units.peers[pv] + Units.peers[w1] + Units.peers[w2]).toSet() + setOf(pv, w1, w2), eliminations = el)
                }
            }
        }
        return null
    }
}
