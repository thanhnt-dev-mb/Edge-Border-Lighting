package com.merryblue.baseapplication.enums

import android.content.Context
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.model.IntroModel

enum class IntroPage {
    PAGE_0,
    PAGE_1,
    PAGE_2,
    PAGE_3,
    PAGE_ADS,
    ;

    fun toModel(context: Context) : IntroModel {
        return when(this) {
            PAGE_0 -> IntroModel(
                0,
                context.getString(R.string.txt_intro1_title),
                context.getString(R.string.txt_intro1_subtitle),
                R.drawable.image_intro1,
                0
            )
            PAGE_1 -> IntroModel(
                1,
                context.getString(R.string.txt_intro2_title),
                context.getString(R.string.txt_intro2_subtitle),
                R.drawable.image_intro2,
                1
            )
            PAGE_ADS -> IntroModel(
                2,
                "",
                "",
                0,
                0
            )
            PAGE_2 -> IntroModel(
                3,
                context.getString(R.string.txt_intro3_title),
                context.getString(R.string.txt_intro3_subtitle),
                R.drawable.image_intro3,
                2
            )
            PAGE_3 -> IntroModel(
                4,
                context.getString(R.string.txt_intro3_title),
                context.getString(R.string.txt_intro3_subtitle),
                R.drawable.image_intro3,
                3
            )
        }
        }

    companion object {
        fun allPage(hideAds: Boolean = false): List<IntroPage> {
            return if (hideAds) {
                listOf(PAGE_0, PAGE_1, PAGE_2, PAGE_3)
            } else {
                listOf(PAGE_0, PAGE_1, PAGE_ADS, PAGE_2, PAGE_3)
            }
        }

        fun pageDataBy(page: Int,  context: Context, hideAds: Boolean): IntroModel {
            return allPage(hideAds).getOrNull(page)
                ?.toModel(context) ?:
            IntroModel(
                0,
                context.getString(R.string.txt_intro1_title),
                context.getString(R.string.txt_intro1_subtitle),
                R.drawable.image_intro1,
                0
            )
        }
    }
}
