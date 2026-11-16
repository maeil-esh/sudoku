package com.uk2solutions.sudoku.domain.generator

/**
 * SplitMix64 기반 PRNG. java.util.Random 대신 자체 구현해
 * 기기·OS·JVM 버전과 무관하게 같은 시드에서 같은 수열을 보장한다(SPEC §5.2).
 */
class SeededRandom(seed: Long) {
    private var state: Long = seed

    fun nextLong(): Long {
        state += -0x61c8864680b583ebL
        var z = state
        z = (z xor (z ushr 30)) * -0x40a7b892e31b1a47L
        z = (z xor (z ushr 27)) * -0x6b2fb644ecceee15L
        return z xor (z ushr 31)
    }

    /** [0, bound) 범위 정수. */
    fun nextInt(bound: Int): Int {
        require(bound > 0)
        return ((nextLong() ushr 1) % bound).toInt()
    }

    fun shuffle(a: IntArray) {
        for (i in a.size - 1 downTo 1) {
            val j = nextInt(i + 1)
            val t = a[i]; a[i] = a[j]; a[j] = t
        }
    }
}
