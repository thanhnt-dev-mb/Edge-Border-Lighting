package com.merryblue.baseapplication.ui.onboard.language

import android.app.Application
import android.content.Context
import com.merryblue.baseapplication.coredata.AppRepository
import com.merryblue.baseapplication.coredata.model.LanguageModel
import com.merryblue.baseapplication.enums.SupportedLanguage
import com.merryblue.baseapplication.ui.iap.BillingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import org.app.core.base.BaseViewModel
import org.app.core.base.utils.SingleLiveEvent
import java.util.*
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    application: Application,
    private val appRepository: AppRepository,
    private val billingRepository: BillingRepository,
) : BaseViewModel(application) {

    private var orderedList = mutableListOf<SupportedLanguage>()

    private var _selectedLanguage: LanguageModel? = null
    var selectedLanguage: LanguageModel?
        get() = _selectedLanguage
        set(value) {
            _selectedLanguage = value
        }

    val nextEnable by lazy { SingleLiveEvent<Boolean>() }

    val connectionState = appRepository.networkState

    fun isPremium() = billingRepository.isPurchased()

    val userLanguage: LanguageModel?
        get() = appRepository.getUserLanguage()

    val onboardedLanguage: Boolean
        get() = appRepository.onboardedLanguage

    fun isFirstTime() = appRepository.isFirstLaunch

    init {
        if (appRepository.isFirstLaunch) {
            nextEnable.postValue(false)
            appRepository.clearPreferences()
        }
    }

    fun getLanguage(context: Context, fromIntro: Boolean): List<LanguageModel> {
        val language = appRepository.getUserLanguage()
        val systemLanguage = Locale.getDefault().language
        var currentLanguage = "en"
        if (language != null) {
            currentLanguage = language.value
        } else if (systemLanguage.isNotBlank()) {
            currentLanguage = systemLanguage
        }

        val selectedModel = if (language != null) SupportedLanguage.from(currentLanguage) else null

        val results = mutableListOf<LanguageModel>()
        if (orderedList.isNotEmpty()) {
            selectedModel?.let {
                results.add(selectedModel.toModel(context, true))
                nextEnable.postValue(true)
            }
            orderedList.forEach { item ->
                if (item != selectedModel) {
                    results.add(item.toModel(context))
                }
            }
        } else {
            selectedModel?.let {
                orderedList.add(selectedModel)
                results.add(selectedModel.toModel(context, true))
                nextEnable.postValue(true)
            } ?: kotlin.run {
                val lang = SupportedLanguage.from(currentLanguage)
                orderedList.add(lang)
                results.add(lang.toModel(context, false))
            }

//            if (fromIntro) {
//                SupportedLanguage.displayList().forEach { item ->
//                    if (item != selectedModel) {
//                        orderedList.add(item)
//                        results.add(item.toModel(context))
//                    }
//                }
//            } else {
                nextEnable.postValue(true)
                SupportedLanguage.entries.forEach { item ->
                    if (!orderedList.contains(item)) {
                        val preList = SupportedLanguage.displayList()
                        if (preList.contains(item)) {
                            orderedList.add(1, item)
                            results.add(1, item.toModel(context))
                        } else {
                            orderedList.add(item)
                            results.add(item.toModel(context))
                        }
                    }
                }
//            }
        }

        return results
    }

    fun updateUserLanguage() {
        _selectedLanguage?.let {
            appRepository.setUserLanguage(it)
        }
    }
}
