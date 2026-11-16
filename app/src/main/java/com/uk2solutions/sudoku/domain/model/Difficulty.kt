package com.uk2solutions.sudoku.domain.model

/** SPEC §5.2 난이도. [maxTechniqueLevel]은 판정에 허용되는 사람 기법 등급 상한. */
enum class Difficulty(val clueRange: IntRange) {
    EASY(41..46),
    MEDIUM(33..38),
    HARD(27..32),
    EXPERT(24..27),
    DEVIL(22..25),
}
