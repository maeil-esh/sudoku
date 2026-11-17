package com.uk2solutions.sudoku.domain.technique

import com.uk2solutions.sudoku.domain.model.Units

/** 사람이 푸는 방식의 판: 칸 값과 후보(9비트 마스크). 기법 엔진과 트레이닝이 공유한다. */
class CandGrid(val v: IntArray, val c: IntArray) {
    fun copy() = CandGrid(v.copyOf(), c.copyOf())
    fun has(i: Int, d: Int) = c[i] and (1 shl (d - 1)) != 0
    fun count(i: Int) = Integer.bitCount(c[i])
    fun digits(i: Int): List<Int> = (1..9).filter { has(i, it) }
    val solved get() = v.none { it == 0 }

    fun place(i: Int, d: Int) {
        v[i] = d; c[i] = 0
        val clear = (1 shl (d - 1)).inv()
        for (p in Units.peers[i]) c[p] = c[p] and clear
    }

    fun eliminate(i: Int, d: Int): Boolean {
        if (!has(i, d)) return false
        c[i] = c[i] and (1 shl (d - 1)).inv(); return true
    }

    companion object {
        /** 칸 값만으로 후보를 계산한다(규칙 충돌이 있으면 null). */
        fun from(values: IntArray): CandGrid? {
            val c = IntArray(81)
            for (i in 0 until 81) if (values[i] == 0) {
                var m = 0x1FF
                for (p in Units.peers[i]) if (values[p] != 0) m = m and (1 shl (values[p] - 1)).inv()
                if (m == 0) return null
                c[i] = m
            }
            return CandGrid(values.copyOf(), c)
        }
    }
}

/** 트레이닝 대상 기법(필요 등급 순). [level]: 0 단일 후보, 1 숨은 단일, 2 포인팅·쌍, 3 삼중·숨은 쌍, 4 X-윙·Y-윙·스워드피시. */
enum class Technique(val level: Int) {
    NAKED_SINGLE(0), HIDDEN_SINGLE(1),
    POINTING(2), CLAIMING(2), NAKED_PAIR(2),
    HIDDEN_PAIR(3), NAKED_TRIPLE(3), HIDDEN_TRIPLE(3),
    X_WING(4), Y_WING(4), SWORDFISH(4),
}

/**
 * 기법이 찾아낸 추론. [pattern]은 패턴을 이루는 칸, [digits]는 패턴의 숫자(Y-윙은 [피벗A, 피벗B, 지울 C]),
 * [houses]는 함께 강조할 행·열·박스 칸, [place]는 채울 값(단일 기법), [eliminations]는 지울 후보 (칸, 숫자).
 */
data class Deduction(
    val technique: Technique, val pattern: Set<Int>, val digits: List<Int>, val houses: Set<Int>,
    val place: Pair<Int, Int>? = null, val eliminations: List<Pair<Int, Int>> = emptyList(),
)
