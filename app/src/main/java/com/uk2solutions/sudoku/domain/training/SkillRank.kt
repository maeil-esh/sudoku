package com.uk2solutions.sudoku.domain.training

/** 실력 단계. 레슨을 마스터(별 2개 이상)할수록 올라간다. */
enum class Rank(val minMastered: Int) {
    ROOKIE(0), APPRENTICE(2), SOLVER(6), EXPERT(9), GRANDMASTER(12),
}

/**
 * [mastered]/[total]: 마스터한 레슨 수. [next]: 다음 단계(없으면 null). [toNext]: 다음 단계까지 남은 레슨 수.
 * [nextNeedsPremium]: 다음 단계에 오르려면 무료 레슨만으로는 부족한지(프리미엄 레슨이 필요한지).
 */
data class RankInfo(val rank: Rank, val mastered: Int, val total: Int, val next: Rank?, val toNext: Int, val nextNeedsPremium: Boolean) {
    /** 현재 단계 안에서의 진행률(0..1). */
    val progress: Float get() = if (next == null) 1f else (mastered - rank.minMastered).toFloat() / (next.minMastered - rank.minMastered)
}

object SkillRanks {
    const val MASTERED_SCORE = 2

    fun of(scores: Map<String, Int>): RankInfo {
        val total = Lessons.all.size
        val mastered = Lessons.all.count { (scores[it.id] ?: -1) >= MASTERED_SCORE }
        val rank = Rank.entries.last { mastered >= it.minMastered }
        val next = Rank.entries.getOrNull(rank.ordinal + 1)
        val freeCount = Lessons.all.count { !it.premium }
        return RankInfo(rank, mastered, total, next, if (next == null) 0 else next.minMastered - mastered, next != null && next.minMastered > freeCount)
    }
}
