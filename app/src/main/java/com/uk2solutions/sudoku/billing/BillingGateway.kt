package com.uk2solutions.sudoku.billing

import android.app.Activity
import kotlinx.coroutines.flow.Flow

enum class PurchaseState { PURCHASED, PENDING, UNSPECIFIED }

/** Play가 알려 준 보유 구매. 앱은 이 값만 믿고 프리미엄을 판정한다(코드·영수증 직접 검증 없음). */
data class OwnedPurchase(
    val productIds: List<String>,
    val state: PurchaseState,
    val token: String,
    val acknowledged: Boolean,
)

sealed interface QueryResult {
    data class Success(val purchases: List<OwnedPurchase>) : QueryResult
    /** 네트워크·Play 서비스 오류. 이때는 프리미엄을 해제하지 않는다(SPEC §7.2-5). */
    data object Failure : QueryResult
}

/** PurchasesUpdatedListener로 들어온 알림(앱 안 구매, 앱 밖 상환·보류 완료). */
data class PurchaseUpdate(val ok: Boolean, val canceled: Boolean, val purchases: List<OwnedPurchase>)

/** Play 결제를 숨기는 경계. 테스트에서는 Fake로 대체한다(SPEC §7.5). */
interface BillingGateway {
    val purchaseUpdates: Flow<PurchaseUpdate>
    suspend fun queryPurchases(): QueryResult
    /** Play 상품 가격 문자열(통화 포함). 가격을 하드코딩하지 않는다(SPEC §7.1). 조회 실패 시 null. */
    suspend fun queryPrice(productId: String): String?
    suspend fun acknowledge(token: String): Boolean
    /** 결제 화면을 띄운다. 화면이 떴으면 true(결과는 [purchaseUpdates]로 온다). */
    suspend fun launchPurchase(activity: Activity, productId: String): Boolean
}
