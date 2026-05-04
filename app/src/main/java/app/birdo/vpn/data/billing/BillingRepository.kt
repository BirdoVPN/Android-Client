package app.birdo.vpn.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Google Play Billing wrapper. Hilt-singleton — owns a single BillingClient
 * instance for the app lifetime.
 *
 * **Product IDs** (configured in Play Console → Monetize → Subscriptions):
 *   - `operative_monthly`, `operative_yearly` — Operative tier
 *   - `sovereign_monthly`, `sovereign_yearly` — Sovereign tier
 *
 * Flow:
 *   1. Connect → onBillingSetupFinished → queryProducts()
 *   2. UI calls launchPurchase(activity, productDetails, offerToken)
 *   3. PurchasesUpdatedListener fires → acknowledgePurchase + post token to backend
 *   4. Backend validates with Google Play Developer API and provisions plan
 */
@Singleton
class BillingRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : PurchasesUpdatedListener {

    companion object {
        const val PRODUCT_OPERATIVE_MONTHLY = "operative_monthly"
        const val PRODUCT_OPERATIVE_YEARLY = "operative_yearly"
        const val PRODUCT_SOVEREIGN_MONTHLY = "sovereign_monthly"
        const val PRODUCT_SOVEREIGN_YEARLY = "sovereign_yearly"

        val SUBSCRIPTION_PRODUCT_IDS = listOf(
            PRODUCT_OPERATIVE_MONTHLY,
            PRODUCT_OPERATIVE_YEARLY,
            PRODUCT_SOVEREIGN_MONTHLY,
            PRODUCT_SOVEREIGN_YEARLY,
        )
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            com.android.billingclient.api.PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build()
        )
        .build()

    private val _connectionState = MutableStateFlow(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _products = MutableStateFlow<List<ProductDetails>>(emptyList())
    val products: StateFlow<List<ProductDetails>> = _products.asStateFlow()

    private val _purchaseEvents = MutableSharedFlow<PurchaseEvent>(extraBufferCapacity = 4)
    val purchaseEvents: SharedFlow<PurchaseEvent> = _purchaseEvents.asSharedFlow()

    enum class ConnectionState { Disconnected, Connecting, Connected, Unavailable }

    sealed class PurchaseEvent {
        data class Success(
            val productId: String,
            val purchaseToken: String,
            val orderId: String?,
            val packageName: String,
        ) : PurchaseEvent()
        data class Error(val message: String, val responseCode: Int) : PurchaseEvent()
        data object UserCanceled : PurchaseEvent()
    }

    /** Connect to Google Play Billing service. Safe to call multiple times. */
    fun connect() {
        if (_connectionState.value == ConnectionState.Connected ||
            _connectionState.value == ConnectionState.Connecting) return

        _connectionState.value = ConnectionState.Connecting
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _connectionState.value = ConnectionState.Connected
                    scope.launch {
                        queryProducts()
                        // Re-acknowledge any pending purchases that were missed.
                        queryActivePurchases()
                    }
                } else {
                    _connectionState.value = ConnectionState.Unavailable
                }
            }

            override fun onBillingServiceDisconnected() {
                _connectionState.value = ConnectionState.Disconnected
            }
        })
    }

    /** Query subscription product details from Play Console. */
    suspend fun queryProducts(): ProductDetailsResult? {
        if (!client.isReady) return null
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                SUBSCRIPTION_PRODUCT_IDS.map { id ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(id)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                }
            )
            .build()
        val result = client.queryProductDetails(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _products.value = result.productDetailsList.orEmpty()
        }
        return result
    }

    /**
     * Launch the Play purchase flow. [offerToken] is required for subscriptions —
     * it identifies the specific base plan + offer combo from [ProductDetails].
     */
    fun launchPurchase(
        activity: Activity,
        productDetails: ProductDetails,
        offerToken: String,
    ): BillingResult {
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(productDetails)
                        .setOfferToken(offerToken)
                        .build()
                )
            )
            .build()
        return client.launchBillingFlow(activity, flowParams)
    }

    /** Acknowledge a purchase locally (must be called within 3 days or it auto-refunds). */
    suspend fun acknowledgeLocally(purchaseToken: String): BillingResult {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchaseToken)
            .build()
        return client.acknowledgePurchase(params)
    }

    /** Query for any active subscriptions the user already owns. */
    private suspend fun queryActivePurchases() {
        if (!client.isReady) return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val result = client.queryPurchasesAsync(params)
        if (result.billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            for (purchase in result.purchasesList) {
                if (purchase.purchaseState == com.android.billingclient.api.Purchase.PurchaseState.PURCHASED) {
                    val productId = purchase.products.firstOrNull() ?: continue
                    _purchaseEvents.emit(
                        PurchaseEvent.Success(
                            productId = productId,
                            purchaseToken = purchase.purchaseToken,
                            orderId = purchase.orderId,
                            packageName = purchase.packageName,
                        )
                    )
                }
            }
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<com.android.billingclient.api.Purchase>?,
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases.isNullOrEmpty()) return
                scope.launch {
                    for (purchase in purchases) {
                        if (purchase.purchaseState != com.android.billingclient.api.Purchase.PurchaseState.PURCHASED) continue
                        val productId = purchase.products.firstOrNull() ?: continue
                        _purchaseEvents.emit(
                            PurchaseEvent.Success(
                                productId = productId,
                                purchaseToken = purchase.purchaseToken,
                                orderId = purchase.orderId,
                                packageName = purchase.packageName,
                            )
                        )
                    }
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                scope.launch { _purchaseEvents.emit(PurchaseEvent.UserCanceled) }
            }
            else -> {
                scope.launch {
                    _purchaseEvents.emit(
                        PurchaseEvent.Error(
                            billingResult.debugMessage.ifBlank { "Purchase failed" },
                            billingResult.responseCode,
                        )
                    )
                }
            }
        }
    }
}
