package com.merryblue.baseapplication.coredata.model

import android.content.Context
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.ui.iap.BillingRepository.Companion.MONTHLY_SUBSCRIPTION_ID


data class SubscriptionModel(
    val id: String,
    val name: String,
    val formatPrice: String,
    val currencyCode: String,
    val priceAmount: Long,
    val period: BillingPeriod,
    val hasTrial: Boolean = true,
    val discount: Int = 40,
    var state: Int = Purchase.PurchaseState.UNSPECIFIED_STATE,
) {

    enum class BillingPeriod {
        NONE, P1W, P1M, P3M, P6M, P1Y
        ;

        fun title(context: Context) = when(this) {
            P1M -> context.getString(R.string.txt_premium_monthly_package)
            P1Y -> context.getString(R.string.txt_premium_yearly_package)
            else -> ""
        }

        companion object {
            fun safeValueOf(value: String) = try {
                valueOf(value)
            } catch (e: Exception) {
                null
            }
        }
    }

    companion object {
        fun fromProductDetails(productDetails: ProductDetails, context: Context): SubscriptionModel? {
            val pricingPhase = productDetails.subscriptionOfferDetails?.get(0)?.pricingPhases?.pricingPhaseList?.firstOrNull { it.priceAmountMicros > 0 } ?: return null
            val formatPrice = pricingPhase.formattedPrice
            val currencyCode = pricingPhase.priceCurrencyCode
            val priceAmount = pricingPhase.priceAmountMicros
            val period = BillingPeriod.safeValueOf(pricingPhase.billingPeriod) ?: return null
            val id = productDetails.productId
            val offers = productDetails.subscriptionOfferDetails
            val hasTrial = offers != null && offers.size > 1

            return SubscriptionModel(
                id,
                period.title(context),
                formatPrice,
                currencyCode,
                priceAmount,
                period,
                hasTrial,
            )
        }
    }
}