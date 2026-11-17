package com.uk2solutions.sudoku.domain.game

import com.uk2solutions.sudoku.domain.model.Board
import com.uk2solutions.sudoku.domain.model.Units

object HintPolicy { const val FREE_PER_DAY = 3 }

enum class HintTechnique { NAKED_SINGLE, HIDDEN_SINGLE, DEDUCTION }
enum class UnitKind { ROW, COL, BOX }

/** [unit]: 숨은 단일일 때 그 숫자가 들어갈 곳이 하나뿐인 행/열/박스. */
data class Hint(val index: Int, val digit: Int, val technique: HintTechnique, val unit: UnitKind? = null)

enum class StepType { INTRO, ELIMINATION, ANSWER }

/**
 * 힌트 설명의 한 단계. [highlight]는 밝게 남길 칸(나머지는 어둡게), [emphasis]는 그중 특히 강조할 칸(후보를 지우는 숫자가 있는 칸),
 * [digits]는 설명에 쓰는 숫자들.
 */
data class HintStep(
    val type: StepType, val highlight: Set<Int>, val emphasis: Set<Int> = emptySet(),
    val digits: List<Int> = emptyList(), val unit: UnitKind? = null,
)

data class HintPlan(val hint: Hint, val steps: List<HintStep>)

/** SPEC §5.4 힌트. 정답과 다른 입력은 빈칸으로 보고, 다음에 풀 수 있는 칸을 찾아 단계별 설명을 만든다. */
object Hints {
    /** 정답과 같은 값만 인정한 보드(틀린 값은 빈칸 취급). */
    private fun trusted(s: GameState) = IntArray(81) { if (s.values[it] == s.solution[it]) s.values[it] else 0 }

    private fun candidates(v: IntArray): IntArray {
        val cand = IntArray(81)
        for (i in 0 until 81) if (v[i] == 0) {
            var m = 0x1FF
            for (p in Units.peers[i]) if (v[p] != 0) m = m and (1 shl (v[p] - 1)).inv()
            cand[i] = m
        }
        return cand
    }

    fun next(s: GameState, preferred: Int = -1): Hint? {
        val v = trusted(s)
        if (v.none { it == 0 }) return null
        val cand = candidates(v)
        for (i in 0 until 81) if (v[i] == 0 && Integer.bitCount(cand[i]) == 1)
            return Hint(i, s.solution[i], HintTechnique.NAKED_SINGLE)
        Units.all.forEachIndexed { ui, u ->
            for (d in 0 until 9) {
                val cells = u.filter { cand[it] and (1 shl d) != 0 }
                if (cells.size == 1) {
                    val kind = when { ui < 9 -> UnitKind.ROW; ui < 18 -> UnitKind.COL; else -> UnitKind.BOX }
                    return Hint(cells[0], s.solution[cells[0]], HintTechnique.HIDDEN_SINGLE, kind)
                }
            }
        }
        val i = if (preferred >= 0 && v[preferred] == 0) preferred else v.indexOfFirst { it == 0 }
        return Hint(i, s.solution[i], HintTechnique.DEDUCTION)
    }

    private fun unitCells(kind: UnitKind, index: Int): Set<Int> = when (kind) {
        UnitKind.ROW -> Units.rows[Board.row(index)]
        UnitKind.COL -> Units.cols[Board.col(index)]
        UnitKind.BOX -> Units.boxes[Board.box(index)]
    }.toSet()

    /** [hint]를 단계별로 설명하는 계획. 단계 수는 2(추론) 또는 3(단일 후보·숨은 단일). */
    fun plan(s: GameState, hint: Hint): HintPlan {
        val v = trusted(s)
        val t = hint.index
        val answer = HintStep(StepType.ANSWER, highlight = setOf(t), digits = listOf(hint.digit))
        val steps = when (hint.technique) {
            HintTechnique.NAKED_SINGLE -> {
                val around = Units.peers[t].toSet() + t
                val blockers = Units.peers[t].filter { v[it] != 0 }
                listOf(
                    HintStep(StepType.INTRO, highlight = around),
                    HintStep(StepType.ELIMINATION, highlight = blockers.toSet() + t, emphasis = blockers.toSet(), digits = blockers.map { v[it] }.distinct().sorted()),
                    answer,
                )
            }
            HintTechnique.HIDDEN_SINGLE -> {
                val kind = hint.unit ?: UnitKind.BOX
                val unit = unitCells(kind, t)
                // 같은 단위의 다른 빈칸마다, 그 칸에 해당 숫자를 못 넣게 막는 칸(같은 행·열·박스의 같은 숫자)을 하나씩 찾는다.
                val blockers = unit.filter { it != t && v[it] == 0 }
                    .mapNotNull { c -> Units.peers[c].firstOrNull { v[it] == hint.digit } }.toSet()
                listOf(
                    HintStep(StepType.INTRO, highlight = unit, digits = listOf(hint.digit), unit = kind),
                    HintStep(StepType.ELIMINATION, highlight = unit + blockers, emphasis = blockers, digits = listOf(hint.digit), unit = kind),
                    answer,
                )
            }
            HintTechnique.DEDUCTION -> listOf(
                HintStep(StepType.INTRO, highlight = Units.peers[t].toSet() + t),
                answer,
            )
        }
        return HintPlan(hint, steps)
    }
}
