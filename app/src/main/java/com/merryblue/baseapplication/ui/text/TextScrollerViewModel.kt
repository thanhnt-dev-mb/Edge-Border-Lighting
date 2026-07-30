package com.merryblue.baseapplication.ui.text

import android.app.Application
import com.merryblue.baseapplication.R
import com.merryblue.baseapplication.coredata.AppRepository
import com.merryblue.baseapplication.ui.text.model.Background
import com.merryblue.baseapplication.ui.text.model.Text
import com.merryblue.baseapplication.ui.text.model.TextEffect
import com.merryblue.baseapplication.ui.text.model.TextScrollerEffect
import com.merryblue.baseapplication.ui.text.model.Themes
import dagger.hilt.android.lifecycle.HiltViewModel
import org.app.core.base.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class TextScrollerViewModel @Inject constructor(
    private val application: Application,
) : BaseViewModel(application) {

    private fun assetFont(fileName: String): String = "font/$fileName"

    fun getDataThemes(): List<Themes> {
        return listOf(
            Themes(id = 1, content = R.string.txt_light, contentColor = R.color.colorGray09, background = R.color.colorWhite, font = assetFont("inter_black.ttf")),
            Themes(id = 2, content = R.string.txt_dark, contentColor = R.color.colorWhite, background = org.app.core.R.color.colorBlack, font = assetFont("inter_black.ttf")),
            Themes(id = 3, content = R.string.txt_bakbak_one, contentColor = R.color.color_ADFC0D, background = org.app.core.R.color.colorBlack, font = assetFont("bakbak_one_regular.ttf")),
            Themes(id = 4, content = R.string.txt_baloo, contentColor = org.app.core.R.color.colorBlack, background = R.color.color_A9FF00, font = assetFont("baloo_regular.ttf")),
            Themes(id = 5, content = R.string.txt_cuprum, contentColor = org.app.core.R.color.colorBlack, background = R.color.color_FF1200, font = assetFont("cuprum_regular.ttf")),
            Themes(id = 6, content = R.string.txt_galdeano, contentColor = R.color.color_FF1200, background = org.app.core.R.color.colorBlack, font = assetFont("galdeano_regular.ttf")),
            Themes(id = 7, content = R.string.txt_impact, contentColor = org.app.core.R.color.colorBlack, background = R.color.color_FFF500, font = assetFont("impact.ttf")),
            Themes(id = 8, content = R.string.txt_mali, contentColor = org.app.core.R.color.colorBlack, background = R.color.color_FD7603, font = assetFont("mali_bold.ttf")),
            Themes(id = 9, content = R.string.txt_molle, contentColor = org.app.core.R.color.colorBlack, background = R.color.color_008CFF, font = assetFont("molle_italic.ttf")),
            Themes(id = 10, content = R.string.txt_optima, contentColor = R.color.color_008CFF, background = org.app.core.R.color.colorBlack, font = assetFont("optima.ttf")),
            Themes(id = 11, content = R.string.txt_ramabhadra, contentColor = org.app.core.R.color.colorWhite, background = R.color.color_3233FF, font = assetFont("ramabhadra_regular.ttf")),
            Themes(id = 12, content = R.string.txt_roboto, contentColor = org.app.core.R.color.colorBlack, background = R.color.color_DD16FB, font = assetFont("roboto_black.ttf")),
            Themes(id = 13, content = R.string.txt_rowdies, contentColor = org.app.core.R.color.colorBlack, background = R.color.color_F8E1C2, font = assetFont("rowdies_bold.ttf")),
            Themes(id = 14, content = R.string.txt_sarala, contentColor = org.app.core.R.color.colorBlack, background = R.color.color_CD6B86, font = assetFont("sarala_bold.ttf")),
            Themes(id = 15, content = R.string.txt_secular_one, contentColor = org.app.core.R.color.colorWhite, background = R.color.color_4313C4, font = assetFont("secular_one_regular.ttf")),
            Themes(id = 16, content = R.string.txt_shlop, contentColor = org.app.core.R.color.colorWhite, background = R.color.color_7D02E7, font = assetFont("shlop.otf")),
            Themes(id = 17, content = R.string.txt_shrikhand, contentColor = org.app.core.R.color.colorBlack, background = R.color.color_93EB22, font = assetFont("shrikhand_regular.ttf")),
            Themes(id = 18, content = R.string.txt_sigmar, contentColor = org.app.core.R.color.colorWhite, background = R.color.color_06276C, font = assetFont("sigmar_regular.ttf")),
            Themes(id = 19, content = R.string.txt_bungee, contentColor = org.app.core.R.color.colorWhite, background = R.color.color_0086F8, font = assetFont("bungee_regular.ttf")),
            Themes(id = 20, content = R.string.txt_anton, contentColor = org.app.core.R.color.colorWhite, background = R.color.color_1A5674, font = assetFont("anton_regular.ttf")),
            Themes(id = 21, content = R.string.txt_spicy_rice, contentColor = org.app.core.R.color.colorWhite, background = R.color.color_FE0202, font = assetFont("spicy_rice_regular.ttf")),
            Themes(id = 22, content = R.string.txt_sura, contentColor = org.app.core.R.color.colorWhite, background = R.color.color_38999F, font = assetFont("sura_bold.ttf")),
            Themes(id = 23, content = R.string.txt_tac_one, contentColor = org.app.core.R.color.colorWhite, background = R.color.color_F1428F, font = assetFont("tac_one_regular.ttf")),
            Themes(id = 24, content = R.string.txt_tiny_5, contentColor = org.app.core.R.color.colorWhite, background = R.color.color_FC7E05, font = assetFont("tiny5_regular.ttf")),
            Themes(id = 25, content = R.string.txt_tilt_warp, contentColor = org.app.core.R.color.colorBlack, background = R.color.color_FDDC03, font = assetFont("tilt_warp_regular.ttf")),
            Themes(id = 26, content = R.string.txt_titan_one, contentColor = org.app.core.R.color.colorWhite, background = R.color.color_7D02E7, font = assetFont("titan_one_regular.ttf")),
            Themes(id = 27, content = R.string.txt_tinos, contentColor = org.app.core.R.color.colorWhite, background = R.color.color_020305, font = assetFont("tinos_bold.ttf")),
        )
    }

    fun getDataText(): List<Text> {
        return listOf(
            Text(id = 1, content = R.string.txt_roboto, font = assetFont("roboto_black.ttf")),
            Text(id = 2, content = R.string.txt_inter, font = assetFont("inter_black.ttf")),
            Text(id = 3, content = R.string.txt_barlow, font = assetFont("barlow_black.ttf")),
            Text(id = 4, content = R.string.txt_bbh_bogle, font = assetFont("bbh_bogle_regular.ttf")),
            Text(id = 5, content = R.string.txt_do_hyeon, font = assetFont("do_hyeon_regular.ttf")),
            Text(id = 6, content = R.string.txt_helvetica, font = assetFont("helvetica.ttf")),
            Text(id = 7, content = R.string.txt_lato, font = assetFont("lato_black.ttf")),
            Text(id = 8, content = R.string.txt_micro_5, font = assetFont("micro_5_regular.ttf")),
            Text(id = 9, content = R.string.txt_nosifer, font = assetFont("nosifer_regular.ttf")),
            Text(id = 10, content = R.string.txt_quartz_ms, font = assetFont("quartz_ms_regular.ttf")),
            Text(id = 11, content = R.string.txt_anta, font = assetFont("anta_regular.ttf")),
            Text(id = 12, content = R.string.txt_sf_pro_display, font = assetFont("sf_pro_display_bold.otf")),
            Text(id = 13, content = R.string.txt_rubik, font = assetFont("rubik_bold.ttf")),
            Text(id = 14, content = R.string.txt_sancreek, font = assetFont("sancreek_regular.ttf")),
            Text(id = 15, content = R.string.txt_tahoma, font = assetFont("tahoma.ttf")),
            Text(id = 16, content = R.string.txt_stylish, font = assetFont("stylish_regular.ttf")),
            Text(id = 17, content = R.string.txt_sigmar, font = assetFont("sigmar_regular.ttf")),
            Text(id = 18, content = R.string.txt_staatliches, font = assetFont("staatliches_regular.ttf")),
            Text(id = 19, content = R.string.txt_zen_dots, font = assetFont("zen_dots_regular.ttf")),
            Text(id = 20, content = R.string.txt_avenir, font = assetFont("avenir_black.otf")),
            Text(id = 21, content = R.string.txt_ubuntu, font = assetFont("ubuntu_bold.ttf")),
            Text(id = 22, content = R.string.txt_tilt_warp, font = assetFont("tilt_warp_regular.ttf")),
            Text(id = 23, content = R.string.txt_raanana, font = assetFont("raanana.ttf")),
            Text(id = 24, content = R.string.txt_sono, font = assetFont("sono_regular.ttf")),
        )
    }

    fun getDataBackground(): List<Background> {
        return listOf(
            Background(id = 1, color = R.color.colorWhite),
            Background(id = 2, color = R.color.colorGray09),
            Background(id = 3, color = R.color.color_2AE30A),
            Background(id = 4, color = R.color.color_016BF5),
            Background(id = 5, color = R.color.color_8AECB3),
            Background(id = 6, color = R.color.color_B48CED),
            Background(id = 7, color = R.color.color_7B9CE9),
            Background(id = 8, color = R.color.color_52BC7A),
            Background(id = 9, color = R.color.color_F66E78),
            Background(id = 10, color = R.color.color_B473C3),
            Background(id = 11, color = R.color.color_FA00FF),
            Background(id = 12, color = R.color.color_FAA94E),
            Background(id = 13, color = R.color.color_81EBF9),
            Background(id = 14, color = R.color.color_F2E45D),
            Background(id = 15, color = R.color.color_54A3A1),
            Background(id = 16, color = R.color.color_F5515A),
            Background(id = 17, color = R.color.color_EFD800),
            Background(id = 18, color = R.color.color_61B584),
            Background(id = 19, color = R.color.color_B0550F),
            Background(id = 20, color = R.color.color_0DAFFA),
            Background(id = 21, color = R.color.color_454219),
            Background(id = 22, color = R.color.color_FCFA96),
            Background(id = 23, color = R.color.color_F2F2F2),
            Background(id = 24, color = R.color.color_7B71C8),
            Background(id = 25, color = R.color.color_ED0108),
            Background(id = 26, color = R.color.color_22467E),
            Background(id = 27, color = R.color.color_778E22),
            Background(id = 28, color = R.color.color_DE5EB3),
            Background(id = 29, color = R.color.color_61D0E3),
            Background(id = 30, color = R.color.color_DB7A19),
            Background(id = 31, color = R.color.color_5B21B6),
            Background(id = 32, color = R.color.color_44BC26),
            Background(id = 33, color = R.color.color_EC4899),
            Background(id = 34, color = R.color.color_B1A263),
            Background(id = 35, color = R.color.color_F6A800),
        )
    }

    fun getDataEffect(): List<TextEffect> {
        return listOf(
            TextEffect(
                id = 1,
                content = R.string.txt_scroller,
                icon = R.drawable.ic_effect_scroller,
                type = TextScrollerEffect.SCROLLER
            ),
            TextEffect(
                id = 2,
                content = R.string.txt_blink,
                icon = R.drawable.ic_effect_blink,
                type = TextScrollerEffect.BLINK
            )
        )
    }
}
