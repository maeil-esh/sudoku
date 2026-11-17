package com.uk2solutions.sudoku.domain.training

/** 연습 문제 채점(순수 함수). */
object QuizGrader {
    /** 숫자 고르기: 고른 숫자가 정답과 같아야 한다. */
    fun digit(ex: Example, chosen: Int) = chosen == ex.quizDigit

    /** 칸 고르기(숨은 단일): 고른 칸이 정답 칸이어야 한다. */
    fun cell(ex: Example, tapped: Int) = tapped in ex.expectedCells

    /** 지울 칸 고르기: 고른 칸 집합이 정답 집합과 정확히 같아야 한다(하나라도 빠지거나 더 고르면 오답). */
    fun eliminations(ex: Example, picked: Set<Int>) = picked.isNotEmpty() && picked == ex.expectedCells
}
