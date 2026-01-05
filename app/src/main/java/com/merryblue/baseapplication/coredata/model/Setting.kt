package com.merryblue.baseapplication.coredata.model

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import org.app.core.R
import org.app.core.base.utils.StringResId

data class Setting(
    val code: Code,
    @DrawableRes val resIcon: Int = 0,
    @StringRes val resTitle: Int = 0,
    @StringRes val resDescription: Int = 0,
    var value: String = "",
    @DrawableRes var rightIcon: Int = 0,
    var hasRightLayout: Boolean = false,
    var enabled: Boolean = true
) {
    enum class Code {
        NONE,
        LANGUAGE,
        RATE,
        FEEDBACK,
        PRIVACY,
        Share,
        ;
    }

    fun getTitle(context: Context) : String {
        return if (code == Code.LANGUAGE) {
            context.getString(StringResId.language)
        } else {
            context.getString(resTitle)
        }
    }

    fun hasValue() = value.isNotEmpty()
    
    companion object {
        fun newInstance(code: Code = Code.NONE) = when (code) {
            Code.LANGUAGE -> Setting(
                code = code,
                resIcon = R.drawable.ic_menu_language,
                resTitle = StringResId.language,
                value = "English"
            )
            Code.RATE -> Setting(
                code = code,
                resIcon = com.merryblue.baseapplication.R.drawable.ic_rate,
                resTitle =  StringResId.rate,
            )
            
            Code.FEEDBACK -> Setting(
                code = code,
                resIcon = R.drawable.ic_menu_feedback,
                resTitle = StringResId.feedback,
            )
            
            Code.PRIVACY -> Setting(
                code = code,
                resIcon = R.drawable.ic_menu_privacy,
                resTitle = StringResId.privacy,
            )
            Code.Share -> Setting(
                code = code,
                resIcon = com.merryblue.baseapplication.R.drawable.ic_share,
                resTitle = com.merryblue.baseapplication.R.string.txt_share_friend,
            )
            else -> Setting(code)
        }
    }
}
