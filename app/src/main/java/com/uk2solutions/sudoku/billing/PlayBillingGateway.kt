package com.uk2solutions.sudoku.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Google Play Billing Library 9.x 구현. 근거(2026-11 확인):
 * developer.android.com/google/play/billing/integrate — 일회성 상품은 enablePendingPurchases(enableOneTimeProducts),
 * PENDING은 PURCHASED가 될 때만 권한 부여, 3일 내 acknowledge, queryPurchasesAsync로 앱 밖 상환 감지.
 */
@Singleton
class PlayBillingGateway @Inject constructor(@ApplicationContext private val context: Context) : BillingGateway {
    private val updates = MutableSharedFlow<PurchaseUpdate>(extraBufferCapacity = 8)
    override val purchaseUpdates: Flow<PurchaseUpdate> = updates.asSharedFlow()

    private val listener = PurchasesUpdatedListener { result, purchases ->
        updates.tryEmit(
            PurchaseUpdate(
                ok = result.responseCode == BillingClient.BillingResponseCode.OK,
                canceled = result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED,
                purchases = purchases.orEmpty().map { it.toOwned() },
            ),
        )
    }

    private val client: BillingClient by lazy {
        BillingClient.newBuilder(context)
            .setListener(listener)
            .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
            .enableAutoServiceReconnection()
            .build()
    }

    private val connectLock = Mutex()
    private var details: ProductDetails? = null

    private suspend fun ready(): Boolean = connectLock.withLock {
        if (client.isReady) return true
        suspendCancellableCoroutine { cont ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (cont.isActive) cont.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                }
                override fun onBillingServiceDisconnected() {
                    if (cont.isActive) cont.resume(false)
                }
            })
        }
    }

    // billing-ktx는 더 새로운 Kotlin으로 빌드되어 현재 툴체인과 호환되지 않아, Java 코어 라이브러리의 콜백을 직접 코루틴으로 감싼다.
    override suspend fun queryPurchases(): QueryResult {
        if (!ready()) return QueryResult.Failure
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        return suspendCancellableCoroutine { cont ->
            client.queryPurchasesAsync(params) { result, purchases ->
                if (cont.isActive) cont.resume(
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) QueryResult.Success(purchases.map { it.toOwned() })
                    else QueryResult.Failure,
                )
            }
        }
    }

    override suspend fun queryPrice(productId: String): String? {
        if (!ready()) return null
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId).setProductType(BillingClient.ProductType.INAPP).build(),
                ),
            ).build()
        val d: ProductDetails? = suspendCancellableCoroutine { cont ->
            client.queryProductDetailsAsync(params) { result, queryResult ->
                if (cont.isActive) cont.resume(
                    if (result.responseCode == BillingClient.BillingResponseCode.OK) queryResult.productDetailsList.firstOrNull() else null,
                )
            }
        }
        details = d ?: return null
        @Suppress("DEPRECATION")
        return d.oneTimePurchaseOfferDetails?.formattedPrice
    }

    override suspend fun acknowledge(token: String): Boolean {
        if (!ready()) return false
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(token).build()
        return suspendCancellableCoroutine { cont ->
            client.acknowledgePurchase(params) { result ->
                if (cont.isActive) cont.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
            }
        }
    }

    override suspend fun launchPurchase(activity: Activity, productId: String): Boolean {
        if (!ready()) return false
        val d = details ?: run { queryPrice(productId); details } ?: return false
        val flow = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(d).build()))
            .build()
        return client.launchBillingFlow(activity, flow).responseCode == BillingClient.BillingResponseCode.OK
    }

    private fun Purchase.toOwned() = OwnedPurchase(
        productIds = products,
        state = when (purchaseState) {
            Purchase.PurchaseState.PURCHASED -> PurchaseState.PURCHASED
            Purchase.PurchaseState.PENDING -> PurchaseState.PENDING
            else -> PurchaseState.UNSPECIFIED
        },
        token = purchaseToken,
        acknowledged = isAcknowledged,
    )
}
