package com.uk2solutions.sudoku.domain.technique

/** 사람이 푸는 순서(단순 기법 먼저)로 풀며 기법을 적용한다. 트레이닝 예제 탐색과 등급 판정에 쓴다. */
object HumanSolver {
    fun apply(g: CandGrid, d: Deduction) {
        d.place?.let { (i, v) -> g.place(i, v) }
        d.eliminations.forEach { (i, v) -> g.eliminate(i, v) }
    }

    /** 가장 단순한 적용 가능 추론(허용된 기법 안에서). */
    fun nextSimplest(g: CandGrid, allowed: List<Technique>): Deduction? {
        for (t in allowed) Finders.find(g, t)?.let { return it }
        return null
    }

    /** [allowed] 기법만으로 끝까지 풀리는지, 사용한 최대 등급과 함께 반환한다(풀리지 않으면 null). */
    fun solveLevel(values: IntArray, allowed: List<Technique>): Int? {
        val g = CandGrid.from(values) ?: return null
        var level = 0
        while (!g.solved) {
            val d = nextSimplest(g, allowed) ?: return null
            level = maxOf(level, d.technique.level)
            apply(g, d)
        }
        return level
    }
}
