package com.uk2solutions.sudoku.billing

import com.uk2solutions.sudoku.domain.premium.PREMIUM_PRODUCT_ID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 프리미엄 캐시 저장소(SPEC §7.2-5, §8: premiumCached, lastVerifiedAt). */
interface PremiumStore {
    val premiumCached: Flow<Boolean>
    suspend fun save(premium: Boolean, verifiedAt: Long)
}

/**
 * 프리미엄 판정 규칙(SPEC §7.2).
 * - PURCHASED + premium_lifetime 일 때만 부여. PENDING은 부여하지 않는다.
 * - 부여할 구매는 반드시 acknowledge(3일 내 미승인 시 자동 환불).
 * - 조회가 성공했는데 구매가 없을 때만 해제. 조회 실패 시에는 캐시를 유지한다.
 */
class PremiumManager(
    private val gateway: BillingGateway,
    private val store: PremiumStore,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val acknowledgedTokens = mutableSetOf<String>() // 같은 구매를 중복 승인하지 않기 위한 세션 메모
    private val _pending = MutableStateFlow(false)
    /** 결제가 보류 중이면 true(완료되면 프리미엄이 열린다). */
    val pending: StateFlow<Boolean> = _pending.asStateFlow()

    val isPremium: Flow<Boolean> get() = store.premiumCached

    /** 앱 안 구매·앱 밖 상환·보류 완료 알림을 받아 반영한다. */
    fun start(scope: CoroutineScope) {
        scope.launch { gateway.purchaseUpdates.collect { handleUpdate(it) } }
    }

    /** 앱 시작/포그라운드 복귀/구매 복원 때 호출. 조회에 성공했으면 true. */
    suspend fun refresh(): Boolean = when (val r = gateway.queryPurchases()) {
        is QueryResult.Success -> { apply(r.purchases, authoritative = true); true }
        QueryResult.Failure -> false
    }

    suspend fun priceText(): String? = gateway.queryPrice(PREMIUM_PRODUCT_ID)

    suspend fun launchPurchase(activity: android.app.Activity): Boolean = gateway.launchPurchase(activity, PREMIUM_PRODUCT_ID)

    suspend fun handleUpdate(u: PurchaseUpdate) {
        if (u.canceled || !u.ok) return
        apply(u.purchases, authoritative = false)
        refresh() // 업데이트에는 전체 보유 목록이 없으므로 한 번 더 확인
    }

    private suspend fun apply(purchases: List<OwnedPurchase>, authoritative: Boolean) {
        val mine = purchases.filter { PREMIUM_PRODUCT_ID in it.productIds }
        val owned = mine.filter { it.state == PurchaseState.PURCHASED }
        _pending.value = owned.isEmpty() && mine.any { it.state == PurchaseState.PENDING }
        for (p in owned) {
            if (p.acknowledged || p.token in acknowledgedTokens) continue
            if (gateway.acknowledge(p.token)) acknowledgedTokens += p.token // 실패하면 다음 조회 때 다시 시도
        }
        if (owned.isNotEmpty()) store.save(true, clock())
        else if (authoritative) store.save(false, clock())
    }
}
