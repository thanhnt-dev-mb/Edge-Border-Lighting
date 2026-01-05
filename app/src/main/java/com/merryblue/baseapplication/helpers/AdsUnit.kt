package com.merryblue.baseapplication.helpers

import com.merryblue.baseapplication.BuildConfig
import org.app.core.ads.*

enum class AdsUnit {
    LanguageOption, // Medium
    CompleteGuide,  // Interstitial
    StartApp,       // Interstitial
    SwitchApp,      // AppOpen
    ;
    
    val id: String
        get() {
            val flavor = org.app.core.BuildConfig.FLAVOR
            return when (this) {
                LanguageOption -> {
                    if (BuildConfig.DEBUG) {
                        if (flavor == "admob") NativeAdvancedVideo else NativeAdvancedVideo
                    } else {
                        if (flavor == "admob") NativeAdvancedVideo else AdaptiveBanner
                    }
                }
                CompleteGuide -> {
                    if (BuildConfig.DEBUG) {
                        if (flavor == "admob") Interstitial else Interstitial
                    } else {
                        if (flavor == "admob") Interstitial else AdaptiveBanner
                    }
                }
                StartApp -> {
                    if (BuildConfig.DEBUG) {
                        if (flavor == "admob") Interstitial else Interstitial
                    } else {
                        if (flavor == "admob") Interstitial else AdaptiveBanner
                    }
                }
                SwitchApp -> {
                    if (BuildConfig.DEBUG) {
                        if (flavor == "admob") "ca-app-pub-6445739239297382/5008195428" else AppOpen
                    } else {
                        if (flavor == "admob") "ca-app-pub-6445739239297382/5008195428" else AppOpen
                    }
                }
            }
        }
    
    fun getEvent() : String {
        return when (this) {
            LanguageOption -> { "ClickLanguageOption" }
            CompleteGuide -> { "ClickCompleteGuide" }
            StartApp -> { "ClickStartApp" }
            SwitchApp -> { "ClickSwitchApp" }
        }
    }
}