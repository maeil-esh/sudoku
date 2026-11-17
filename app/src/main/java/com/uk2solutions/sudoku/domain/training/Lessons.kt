package com.uk2solutions.sudoku.domain.training

import com.uk2solutions.sudoku.domain.technique.Technique

/** 레슨 묶음(난이도 사다리). */
enum class LessonGroup { BASICS, BEGINNER, INTERMEDIATE, EXPERT }

/** 연습 문제 형식: 숫자 고르기 / 칸 고르기 / 지울 수 있는 칸 모두 고르기. */
enum class QuizKind { PICK_DIGIT, PICK_CELL, PICK_ELIMINATIONS }

data class Lesson(val id: String, val technique: Technique, val group: LessonGroup, val premium: Boolean, val rules: Boolean = false) {
    val quiz: QuizKind get() = when (technique) {
        Technique.NAKED_SINGLE -> QuizKind.PICK_DIGIT
        Technique.HIDDEN_SINGLE -> QuizKind.PICK_CELL
        else -> QuizKind.PICK_ELIMINATIONS
    }
    /** 후보(메모)를 보드에 보여 줄지. 단일 기법은 후보 없이 칸만 보여 주는 것이 더 쉽다. */
    val showCandidates: Boolean get() = quiz == QuizKind.PICK_ELIMINATIONS
}

/**
 * 트레이닝 교과 과정. 앞 5개(규칙~노출 쌍)는 무료, 그 뒤는 프리미엄(경계는 [premium] 값만 바꾸면 된다).
 * 순서 = 사람이 배우는 순서.
 */
object Lessons {
    val all: List<Lesson> = listOf(
        Lesson("rules", Technique.NAKED_SINGLE, LessonGroup.BASICS, premium = false, rules = true),
        Lesson("naked_single", Technique.NAKED_SINGLE, LessonGroup.BASICS, premium = false),
        Lesson("hidden_single", Technique.HIDDEN_SINGLE, LessonGroup.BEGINNER, premium = false),
        Lesson("pointing", Technique.POINTING, LessonGroup.BEGINNER, premium = false),
        Lesson("claiming", Technique.CLAIMING, LessonGroup.BEGINNER, premium = false),
        Lesson("naked_pair", Technique.NAKED_PAIR, LessonGroup.BEGINNER, premium = false),
        Lesson("hidden_pair", Technique.HIDDEN_PAIR, LessonGroup.INTERMEDIATE, premium = true),
        Lesson("naked_triple", Technique.NAKED_TRIPLE, LessonGroup.INTERMEDIATE, premium = true),
        Lesson("hidden_triple", Technique.HIDDEN_TRIPLE, LessonGroup.INTERMEDIATE, premium = true),
        Lesson("x_wing", Technique.X_WING, LessonGroup.EXPERT, premium = true),
        Lesson("y_wing", Technique.Y_WING, LessonGroup.EXPERT, premium = true),
        Lesson("swordfish", Technique.SWORDFISH, LessonGroup.EXPERT, premium = true),
    )

    fun byId(id: String) = all.firstOrNull { it.id == id }
    fun next(id: String): Lesson? = all.getOrNull(all.indexOfFirst { it.id == id } + 1)
}
