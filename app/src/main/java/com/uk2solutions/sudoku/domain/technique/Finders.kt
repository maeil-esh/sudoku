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
        Technique.SKYSCRAPER -> skyscraper(g)
        Technique.TWO_STRING_KITE -> twoStringKite(g)
        Technique.XYZ_WING -> xyzWing(g)
        Technique.UNIQUE_RECTANGLE -> uniqueRectangle(g)
        Technique.SIMPLE_COLORING -> simpleColoring(g)
        Technique.XY_CHAIN -> xyChain(g)
    }

    private val seesM = Array(81) { i -> BooleanArray(81).also { m -> Units.peers[i].forEach { m[it] = true } } }
    private fun sees(a: Int, b: Int) = seesM[a][b]
    private fun peersWith(g: CandGrid, d: Int, vararg of: Int, exclude: Set<Int>) =
        (0 until 81).filter { c -> c !in exclude && g.has(c, d) && of.all { sees(it, c) } }

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

    /** 스카이스크래퍼: 두 줄에서 숫자가 각각 두 칸뿐이고 한쪽 끝이 같은 줄에 정렬 → 나머지 두 끝(꼭대기)을 모두 보는 칸에서 제거. */
    fun skyscraper(g: CandGrid): Deduction? {
        for (d in 1..9) for (byRow in listOf(true, false)) {
            val lines = if (byRow) Units.rows else Units.cols
            fun cov(c: Int) = if (byRow) Board.col(c) else Board.row(c)
            val two = (0 until 9).map { li -> lines[li].filter { g.has(it, d) } }.filter { it.size == 2 }
            for (x in two.indices) for (y in x + 1 until two.size) {
                val a = two[x]; val b = two[y]
                for (ia in 0..1) for (ib in 0..1) {
                    if (cov(a[ia]) != cov(b[ib]) || cov(a[1 - ia]) == cov(b[1 - ib])) continue
                    val tipA = a[1 - ia]; val tipB = b[1 - ib]
                    val pattern = (a + b).toSet()
                    val el = peersWith(g, d, tipA, tipB, exclude = pattern).map { it to d }
                    if (el.isNotEmpty()) return Deduction(Technique.SKYSCRAPER, pattern, listOf(d), (Units.peers[tipA] + Units.peers[tipB]).toSet() + pattern, eliminations = el)
                }
            }
        }
        return null
    }

    /** 2-스트링 카이트: 한 행과 한 열에서 숫자가 각각 두 칸뿐이고 한 칸씩이 같은 박스 → 박스 밖 두 끝을 모두 보는 칸에서 제거. */
    fun twoStringKite(g: CandGrid): Deduction? {
        for (d in 1..9) {
            val rows = Units.rows.map { r -> r.filter { g.has(it, d) } }.filter { it.size == 2 }
            val cols = Units.cols.map { c -> c.filter { g.has(it, d) } }.filter { it.size == 2 }
            for (r in rows) for (c in cols) {
                val pattern = (r + c).toSet()
                if (pattern.size != 4) continue
                for (i in 0..1) for (j in 0..1) {
                    if (Board.box(r[i]) != Board.box(c[j])) continue
                    val re = r[1 - i]; val ce = c[1 - j]
                    val el = peersWith(g, d, re, ce, exclude = pattern).map { it to d }
                    if (el.isNotEmpty()) return Deduction(Technique.TWO_STRING_KITE, pattern, listOf(d), (Units.peers[re] + Units.peers[ce]).toSet() + pattern, eliminations = el)
                }
            }
        }
        return null
    }

    /** XYZ-윙: 피벗{X,Y,Z}와 날개{X,Z}·{Y,Z}(둘 다 피벗을 봄) → 세 칸을 모두 보는 칸에서 Z 제거. digits = [X, Y, Z]. */
    fun xyzWing(g: CandGrid): Deduction? {
        for (pv in 0 until 81) {
            if (g.v[pv] != 0 || g.count(pv) != 3) continue
            val pd = g.digits(pv)
            val wings = Units.peers[pv].filter { g.v[it] == 0 && g.count(it) == 2 }
            for (z in pd) {
                val (x, y) = pd.filter { it != z }
                val w1s = wings.filter { g.digits(it) == listOf(x, z).sorted() }
                val w2s = wings.filter { g.digits(it) == listOf(y, z).sorted() }
                for (w1 in w1s) for (w2 in w2s) {
                    val pattern = setOf(pv, w1, w2)
                    val el = peersWith(g, z, pv, w1, w2, exclude = pattern).map { it to z }
                    if (el.isNotEmpty()) return Deduction(Technique.XYZ_WING, pattern, listOf(x, y, z), (Units.peers[pv] + Units.peers[w1] + Units.peers[w2]).toSet() + pattern, eliminations = el)
                }
            }
        }
        return null
    }

    /** 유니크 렉탱글(유형 1): 해가 하나뿐이라는 점을 이용. 세 꼭짓점이 {A,B}뿐이면 네 번째 꼭짓점에서 A·B 제거. */
    fun uniqueRectangle(g: CandGrid): Deduction? {
        for (r1 in 0 until 9) for (r2 in r1 + 1 until 9) for (c1 in 0 until 9) for (c2 in c1 + 1 until 9) {
            val cells = listOf(r1 * 9 + c1, r1 * 9 + c2, r2 * 9 + c1, r2 * 9 + c2)
            if (cells.any { g.v[it] != 0 }) continue
            if (cells.map { Board.box(it) }.toSet().size != 2) continue
            for (roof in cells) {
                val floor = cells.filter { it != roof }
                val m = g.c[floor[0]]
                if (Integer.bitCount(m) != 2 || floor.any { g.c[it] != m }) continue
                if (g.c[roof] and m != m || Integer.bitCount(g.c[roof]) <= 2) continue
                val digs = (1..9).filter { m and (1 shl (it - 1)) != 0 }
                val houses = (Units.rows[r1] + Units.rows[r2] + Units.cols[c1] + Units.cols[c2]).toSet()
                return Deduction(Technique.UNIQUE_RECTANGLE, cells.toSet(), digs, houses, eliminations = digs.map { roof to it })
            }
        }
        return null
    }

    /** 단순 컬러링: 한 숫자의 "두 칸뿐" 연결을 두 색으로 칠한다. 같은 색 두 칸이 마주 보면 그 색 제거, 두 색을 모두 보는 칸은 제거. */
    fun simpleColoring(g: CandGrid): Deduction? {
        for (d in 1..9) {
            val adj = Array(81) { mutableListOf<Int>() }
            for (u in Units.all) { val cs = u.filter { g.has(it, d) }; if (cs.size == 2) { adj[cs[0]] += cs[1]; adj[cs[1]] += cs[0] } }
            val color = IntArray(81) { -1 }
            for (s in 0 until 81) {
                if (adj[s].isEmpty() || color[s] != -1) continue
                val comp = ArrayList<Int>(); color[s] = 0; val q = ArrayDeque<Int>(); q.add(s)
                while (q.isNotEmpty()) { val x = q.removeFirst(); comp += x; for (y in adj[x]) if (color[y] == -1) { color[y] = 1 - color[x]; q.add(y) } }
                if (comp.size < 4) continue
                val a = comp.filter { color[it] == 0 }; val b = comp.filter { color[it] == 1 }
                val pattern = comp.toSet()
                val houses = (0 until 81).filter { g.has(it, d) }.toSet()
                for (side in listOf(a, b)) {
                    val clash = side.any { x -> side.any { y -> x < y && sees(x, y) } }
                    if (clash) return Deduction(Technique.SIMPLE_COLORING, pattern, listOf(d), houses, eliminations = side.map { it to d }, secondary = b.toSet())
                }
                val el = (0 until 81).filter { c -> c !in pattern && g.has(c, d) && a.any { sees(it, c) } && b.any { sees(it, c) } }.map { it to d }
                if (el.isNotEmpty()) return Deduction(Technique.SIMPLE_COLORING, pattern, listOf(d), houses, eliminations = el, secondary = b.toSet())
            }
        }
        return null
    }

    /** XY-체인: 후보 2개짜리 칸들을 이어(이웃은 서로 보고 숫자 하나 공유) 양 끝이 같은 숫자 X를 가지면, 양 끝을 모두 보는 칸에서 X 제거. */
    fun xyChain(g: CandGrid): Deduction? {
        val bi = (0 until 81).filter { g.v[it] == 0 && g.count(it) == 2 }
        for (start in bi) for (x in g.digits(start)) {
            val other = g.digits(start).first { it != x }
            var found: Deduction? = null
            fun dfs(path: List<Int>, carry: Int) {
                if (found != null) return
                val cur = path.last()
                for (n in bi) {
                    if (n in path || !sees(cur, n) || !g.has(n, carry)) continue
                    val next = g.digits(n).first { it != carry }
                    val p2 = path + n
                    if (next == x && p2.size >= 4) {
                        val pattern = p2.toSet()
                        val el = peersWith(g, x, start, n, exclude = pattern).map { it to x }
                        if (el.isNotEmpty()) { found = Deduction(Technique.XY_CHAIN, pattern, listOf(x), (Units.peers[start] + Units.peers[n]).toSet() + pattern, eliminations = el); return }
                    }
                    if (p2.size < 8) dfs(p2, next)
                    if (found != null) return
                }
            }
            dfs(listOf(start), other)
            found?.let { return it }
        }
        return null
    }
}
