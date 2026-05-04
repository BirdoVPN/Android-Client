package app.birdo.vpn.ui.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.birdo.vpn.data.billing.BillingRepository
import app.birdo.vpn.data.repository.ApiResult
import app.birdo.vpn.data.repository.BirdoRepository
import com.android.billingclient.api.ProductDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BillingUiState(
    val isReady: Boolean = false,
    val isUnavailable: Boolean = false,
    val products: List<ProductDetails> = emptyList(),
    val isPurchasing: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
)

@HiltViewModel
class BillingViewModel @Inject constructor(
    private val billing: BillingRepository,
    private val repository: BirdoRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BillingUiState())
    val uiState: StateFlow<BillingUiState> = _uiState.asStateFlow()

    init {
        billing.connect()
        observeConnection()
        observeProducts()
        observePurchases()
    }

    private fun observeConnection() {
        viewModelScope.launch {
            billing.connectionState.collect { state ->
                _uiState.value = _uiState.value.copy(
                    isReady = state == BillingRepository.ConnectionState.Connected,
                    isUnavailable = state == BillingRepository.ConnectionState.Unavailable,
                )
            }
        }
    }

    private fun observeProducts() {
        viewModelScope.launch {
            billing.products.collect { products ->
                _uiState.value = _uiState.value.copy(products = products)
            }
        }
    }

    private fun observePurchases() {
        viewModelScope.launch {
            billing.purchaseEvents.collect { event ->
                when (event) {
                    is BillingRepository.PurchaseEvent.Success -> handlePurchaseSuccess(event)
                    is BillingRepository.PurchaseEvent.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isPurchasing = false,
                            message = event.message,
                            isError = true,
                        )
                    }
                    BillingRepository.PurchaseEvent.UserCanceled -> {
                        _uiState.value = _uiState.value.copy(isPurchasing = false, message = null)
                    }
                }
            }
        }
    }

    private suspend fun handlePurchaseSuccess(event: BillingRepository.PurchaseEvent.Success) {
        // Send token to backend for receipt validation + plan provisioning.
        when (
            val result = repository.acknowledgeGooglePlayPurchase(
                productId = event.productId,
                purchaseToken = event.purchaseToken,
                packageName = event.packageName,
                orderId = event.orderId,
            )
        ) {
            is ApiResult.Success -> {
                if (result.data.ok) {
                    // Acknowledge to Google so Play stops nagging.
                    billing.acknowledgeLocally(event.purchaseToken)
                    _uiState.value = _uiState.value.copy(
                        isPurchasing = false,
                        message = "Purchase complete — ${result.data.plan} active",
                        isError = false,
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isPurchasing = false,
                        message = result.data.error ?: "Server rejected the purchase",
                        isError = true,
                    )
                }
            }
            is ApiResult.Error -> {
                _uiState.value = _uiState.value.copy(
                    isPurchasing = false,
                    message = "Network error during validation. Restart app to retry.",
                    isError = true,
                )
            }
        }
    }

    /**
     * Launch the Play purchase flow for [productId]. The first available
     * subscription offer (base plan) is used.
     */
    fun purchase(activity: Activity, productId: String) {
        val product = _uiState.value.products.firstOrNull { it.productId == productId }
        if (product == null) {
            _uiState.value = _uiState.value.copy(message = "Product not available", isError = true)
            return
        }
        val offerToken = product.subscriptionOfferDetails
            ?.firstOrNull()
            ?.offerToken
        if (offerToken == null) {
            _uiState.value = _uiState.value.copy(message = "No subscription offer found", isError = true)
            return
        }
        _uiState.value = _uiState.value.copy(isPurchasing = true, message = null, isError = false)
        billing.launchPurchase(activity, product, offerToken)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null, isError = false)
    }
}
