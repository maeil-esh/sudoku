package com.uk2solutions.sudoku.billing

import android.app.Activity
import com.uk2solutions.sudoku.domain.model.Difficulty
import com.uk2solutions.sudoku.domain.premium.Entitlement
import com.uk2solutions.sudoku.domain.premium.PREMIUM_PRODUCT_ID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class FakeBillingGateway(var result: QueryResult = QueryResult.Success(emptyList())) : BillingGateway {
    val updates = MutableSharedFlow<PurchaseUpdate>(extraBufferCapacity = 4)
    override val purchaseUpdates: Flow<PurchaseUpdate> = updates
    val acknowledged = mutableListOf<String>()
    override suspend fun queryPurchases() = result
    override suspend fun queryPrice(productId: String): String? = "₩4,900"
    override suspend fun acknowledge(token: String): Boolean { acknowledged += token; return true }
    override suspend fun launchPurchase(activity: Activity, productId: String) = true
}

class FakePremiumStore(initial: Boolean = false) : PremiumStore {
    val state = MutableStateFlow(initial)
    var verifiedAt = 0L
    override val premiumCached: Flow<Boolean> = state
    override suspend fun save(premium: Boolean, verifiedAt: Long) { state.value = premium; this.verifiedAt = verifiedAt }
}

class PremiumManagerTest {
    private fun purchase(state: PurchaseState = PurchaseState.PURCHASED, ack: Boolean = false, id: String = PREMIUM_PRODUCT_ID, token: String = "t1") =
        OwnedPurchase(listOf(id), state, token, ack)

    private fun manager(g: FakeBillingGateway, s: FakePremiumStore) = PremiumManager(g, s) { 1000L }

    @Test fun notPurchased() = runBlocking {
        val s = FakePremiumStore(); val m = manager(FakeBillingGateway(), s)
        assertTrue(m.refresh()); assertFalse(s.state.value)
    }

    @Test fun purchasedGrantsAndAcknowledges() = runBlocking {
        val g = FakeBillingGateway(QueryResult.Success(listOf(purchase()))); val s = FakePremiumStore()
        manager(g, s).refresh()
        assertTrue(s.state.value); assertEquals(listOf("t1"), g.acknowledged); assertEquals(1000L, s.verifiedAt)
    }

    @Test fun alreadyAcknowledgedIsNotAcknowledgedAgain() = runBlocking {
        val g = FakeBillingGateway(QueryResult.Success(listOf(purchase(ack = true)))); val s = FakePremiumStore()
        manager(g, s).refresh()
        assertTrue(s.state.value); assertTrue(g.acknowledged.isEmpty())
    }

    @Test fun pendingDoesNotGrant() = runBlocking {
        val g = FakeBillingGateway(QueryResult.Success(listOf(purchase(PurchaseState.PENDING)))); val s = FakePremiumStore()
        val m = manager(g, s); m.refresh()
        assertFalse(s.state.value); assertTrue(m.pending.value); assertTrue(g.acknowledged.isEmpty())
    }

    @Test fun pendingThenPurchasedViaUpdate() = runBlocking {
        val g = FakeBillingGateway(QueryResult.Success(listOf(purchase(PurchaseState.PENDING)))); val s = FakePremiumStore()
        val m = manager(g, s); m.refresh()
        g.result = QueryResult.Success(listOf(purchase()))
        m.handleUpdate(PurchaseUpdate(ok = true, canceled = false, purchases = listOf(purchase())))
        assertTrue(s.state.value); assertFalse(m.pending.value); assertEquals(listOf("t1"), g.acknowledged)
    }

    @Test fun offlineKeepsCachedPremium() = runBlocking {
        val g = FakeBillingGateway(QueryResult.Failure); val s = FakePremiumStore(initial = true)
        val m = manager(g, s)
        assertFalse(m.refresh()); assertTrue(m.isPremium.first()); assertEquals(0L, s.verifiedAt)
    }

    @Test fun refundRemovesPremiumOnlyWhenQuerySucceeds() = runBlocking {
        val g = FakeBillingGateway(QueryResult.Success(listOf(purchase()))); val s = FakePremiumStore()
        val m = manager(g, s); m.refresh(); assertTrue(s.state.value)
        g.result = QueryResult.Failure; m.refresh(); assertTrue(s.state.value)           // 조회 실패: 유지
        g.result = QueryResult.Success(emptyList()); m.refresh(); assertFalse(s.state.value) // 환불: 해제
    }

    @Test fun otherProductsAreIgnored() = runBlocking {
        val g = FakeBillingGateway(QueryResult.Success(listOf(purchase(id = "something_else")))); val s = FakePremiumStore()
        manager(g, s).refresh()
        assertFalse(s.state.value); assertTrue(g.acknowledged.isEmpty())
    }

    @Test fun canceledUpdateChangesNothing() = runBlocking {
        val g = FakeBillingGateway(); val s = FakePremiumStore(initial = true)
        manager(g, s).handleUpdate(PurchaseUpdate(ok = false, canceled = true, purchases = emptyList()))
        assertTrue(s.state.value)
    }

    @Test fun entitlementControlsLocks() {
        val free = Entitlement(false); val pro = Entitlement(true)
        assertTrue(free.canPlay(Difficulty.HARD)); assertTrue("전문가는 누구나", free.canPlay(Difficulty.EXPERT)); assertFalse(free.canPlay(Difficulty.DEVIL))
        assertTrue(pro.canPlay(Difficulty.DEVIL))
        assertEquals(3, free.hintsPerDay); assertNull(pro.hintsPerDay)
        assertFalse(free.detailedStats); assertTrue(pro.detailedStats)
        assertTrue(free.canUsePalette(0)); assertFalse(free.canUsePalette(2)); assertTrue(pro.canUsePalette(3))
    }
}
