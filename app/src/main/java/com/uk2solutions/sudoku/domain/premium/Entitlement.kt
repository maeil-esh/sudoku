package com.uk2solutions.sudoku.domain.premium

import com.uk2solutions.sudoku.domain.game.HintPolicy
import com.uk2solutions.sudoku.domain.model.Difficulty

/** 프리미엄 상품 ID(SPEC §7.1). 일회성 인앱 상품(비소모성). */
const val PREMIUM_PRODUCT_ID = "premium_lifetime"

/**
 * 프리미엄 여부에 따른 권한. 잠금 난이도·힌트·상세 통계·추가 테마는 모두 여기서만 판정한다(SPEC §2, §9 M4).
 * 무료/프리미엄 경계를 바꾸려면 이 파일의 상수만 고친다.
 */
data class Entitlement(val premium: Boolean) {
    fun canPlay(d: Difficulty): Boolean = premium || d in FREE_DIFFICULTIES

    /** 하루 힌트 한도. null이면 무제한. */
    val hintsPerDay: Int? get() = if (premium) null else HintPolicy.FREE_PER_DAY

    val detailedStats: Boolean get() = premium

    /** 팔레트 번호(0=기본). 기본 테마는 모두에게, 나머지는 프리미엄에게만. */
    fun canUsePalette(palette: Int): Boolean = premium || palette == DEFAULT_PALETTE

    companion object {
        /** 전문가까지 무료: 막히면 힌트·트레이닝으로 배우도록 유도한다(악마와 고급 레슨은 프리미엄). */
        val FREE_DIFFICULTIES = setOf(Difficulty.EASY, Difficulty.MEDIUM, Difficulty.HARD, Difficulty.EXPERT)
        const val DEFAULT_PALETTE = 0
        const val PALETTE_COUNT = 4
    }
}
