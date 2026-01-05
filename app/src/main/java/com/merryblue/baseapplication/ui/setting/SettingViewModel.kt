package com.merryblue.baseapplication.ui.setting

import android.app.Application
import android.content.Context
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.AppRepository
import com.merryblue.baseapplication.coredata.model.Setting
import com.merryblue.baseapplication.coredata.model.SubscriptionModel
import com.merryblue.baseapplication.enums.SupportedLanguage
import com.merryblue.baseapplication.ui.iap.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import org.app.core.base.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val application: Application,
    private val appRepository: AppRepository,
    private val billingRepository: BillingRepository
) : BaseViewModel(application) {

    val connectionState = appRepository.networkState

    fun isPremium() = billingRepository.isPurchased()

    fun getSettingItems(context: Context) : List<Setting> {
        return listOf(
            Setting.newInstance(Setting.Code.LANGUAGE).apply {
                value = appRepository.getUserLanguage()?.value?.let { code ->
                    SupportedLanguage.from(code).toModel(context).language
                } ?: SupportedLanguage.ENGLISH.toModel(context).language
            },
            Setting.newInstance(Setting.Code.Share),
            Setting.newInstance(Setting.Code.RATE),
            Setting.newInstance(Setting.Code.FEEDBACK),
            Setting.newInstance(Setting.Code.PRIVACY),
        )
    }

    fun getPremiumTitle(context: Context) : String {
        val purchased = billingRepository.purchasedProducts()
        return purchased?.let {
            if (purchased.period == SubscriptionModel.BillingPeriod.P1Y)
                context.getString(R.string.txt_yearly_premium)
            else
                context.getString(R.string.txt_monthly_premium)
        } ?: context.getString(R.string.txt_upgrade_premium)
    }
}