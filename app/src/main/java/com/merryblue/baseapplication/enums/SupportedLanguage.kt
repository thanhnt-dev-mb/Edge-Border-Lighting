package com.merryblue.baseapplication.enums

import android.content.Context
import org.app.core.R
import com.merryblue.baseapplication.coredata.model.LanguageModel
import org.app.core.R.string.*
import java.util.Locale

enum class SupportedLanguage(val value: String) {
    AFRIKAANS("af"),
    ALBANIAN("sq"),
    AMHARIC("am"),
    ARABIC("ar"),
    ARMENIAN("hy"),
    AZERBAIJAN("az"),
    BANGLA("bn"),
    BASQUE("eu"),
    BELARUSIAN("be"),
    BULGARIAN("bg"),
    BURMESE("my"),
    CATALAN("ca"),
    CHINESE_SIMPLE("zh"),
    CHINESE_TRADITION("zh"),
    CROATIAN("hr"),
    CZECH("cs"),
    DANISH("da"),
    DUTCH("nl"),
    ENGLISH("en"),
    ESTONIAN("et"),
    FILIPINO("fil"),
    FINNISH("fi"),
    FRENCH("fr"),
    GALICIAN("gl"),
    GEORGIAN("ka"),
    GERMAN("de"),
    GREEK("el"),
    GUJARATI("gu"),
    HEBREW("he"),
    HINDI("hi"),
    HUNGARIAN("hu"),
    ICELANDIC("is"),
    INDONESIAN("in"),
    IRISH("ga"),
    ITALIAN("it"),
    JAPANESE("ja"),
    KANNADA("kn"),
    KAZAKH("kk"),
    KHMER("km"),
    KOREAN("ko"),
    KYRGYZ("ky"),
    LAO("lo"),
    LATVIAN("lv"),
    LITHUANIAN("lt"),
    LUXEMBOURGISH("lb"),
    MACEDONIAN("mk"),
    MALAY("ms"),
    MALAYALAM("ml"),
    MAORI("mi"),
    MARATHI("mr"),
    MONGOLIAN("mn"),
    NEPALI("ne"),
    NORWEGIAN("no"),
    PASHTO("ps"),
    PERSIAN("fa"),
    POLISH("pl"),
    PORTUGUESE("pt"),
    PORTUGUESE_BR("pt-br"),
    PUNJABI("pa"),
    ROMANIAN("ro"),
    RUSSIA("ru"),
    SERBIAN("sr"),
    SINHALA("si"),
    SLOVAK("sk"),
    SLOVENIAN("sl"),
    SPANISH("es"),
    SWAHILI("sw"),
    SWEDISH("sv"),
    TAMIL("ta"),
    TELUGU("te"),
    THAI("th"),
    TURKISH("tr"),
    TURKMEN("tk"),
    UKRAINIAN("uk"),
    URDU("ur"),
    UZBEK("uz"),
    VIETNAMESE("vi"),
    ZULU("zu"),
    ;

    fun toModel(context: Context, isSelected: Boolean = false): LanguageModel {
        return when (this) {
            AFRIKAANS -> LanguageModel(context.getString(txt_language_afrikaans), isSelected, value, R.drawable.flag_afrikaans)
            ALBANIAN -> LanguageModel(context.getString(txt_language_albanian), isSelected, value, R.drawable.flag_albanian)
            AMHARIC -> LanguageModel(context.getString(txt_language_amharic), isSelected, value, R.drawable.flag_ethiopia)
            ARABIC -> LanguageModel(context.getString(txt_language_arabic), isSelected, value, R.drawable.flag_arabic)
            ARMENIAN -> LanguageModel(context.getString(txt_language_armenian), isSelected, value, R.drawable.flag_armenian)
            AZERBAIJAN -> LanguageModel(context.getString(txt_language_azerbaijan), isSelected, value, R.drawable.flag_azerbaijani)
            BANGLA -> LanguageModel(context.getString(txt_language_bangla), isSelected, value, R.drawable.flag_hindi)
            BASQUE -> LanguageModel(context.getString(txt_language_basque), isSelected, value, R.drawable.flag_basque)
            BELARUSIAN -> LanguageModel(context.getString(txt_language_belarusian), isSelected, value, R.drawable.flag_belarusian)
            BULGARIAN -> LanguageModel(context.getString(txt_language_bulgarian), isSelected, value, R.drawable.flag_bulgarian)
            BURMESE -> LanguageModel(context.getString(txt_language_burmese), isSelected, value, R.drawable.flag_malay)
            CATALAN -> LanguageModel(context.getString(txt_language_catalan), isSelected, value, R.drawable.flag_catalan)
            CHINESE_SIMPLE -> LanguageModel(context.getString(txt_language_chinese), isSelected, value, R.drawable.flag_chinese)
            CHINESE_TRADITION -> LanguageModel(context.getString(txt_language_chinese_tradition), isSelected, value, R.drawable.flag_chinese_tw)
            CROATIAN -> LanguageModel(context.getString(txt_language_croatian), isSelected, value, R.drawable.flag_croatian)
            CZECH -> LanguageModel(context.getString(txt_language_czech), isSelected, value, R.drawable.flag_czech)
            DANISH -> LanguageModel(context.getString(txt_language_danish), isSelected, value, R.drawable.flag_danish)
            DUTCH -> LanguageModel(context.getString(txt_language_dutch), isSelected, value, R.drawable.flag_dutch)
            ENGLISH -> LanguageModel(context.getString(txt_language_english), isSelected, value, R.drawable.flag_english)
            ESTONIAN -> LanguageModel(context.getString(txt_language_estonian), isSelected, value, R.drawable.flag_estonian)
            FILIPINO -> LanguageModel(context.getString(txt_language_filipino), isSelected, value, R.drawable.flag_filipino)
            FINNISH -> LanguageModel(context.getString(txt_language_finnish), isSelected, value, R.drawable.flag_finnish)
            FRENCH -> LanguageModel(context.getString(txt_language_french), isSelected, value, R.drawable.flag_french)
            GALICIAN -> LanguageModel(context.getString(txt_language_galicia), isSelected, value, R.drawable.flag_galician)
            GEORGIAN -> LanguageModel(context.getString(txt_language_georgian), isSelected, value, R.drawable.flag_georgian)
            GERMAN -> LanguageModel(context.getString(txt_language_german), isSelected, value, R.drawable.flag_german)
            GREEK -> LanguageModel(context.getString(txt_language_greek), isSelected, value, R.drawable.flag_greek)
            GUJARATI -> LanguageModel(context.getString(txt_language_gujarati), isSelected, value, R.drawable.flag_hindi)
            HEBREW -> LanguageModel(context.getString(txt_language_hebrew), isSelected, value, R.drawable.flag_hebrew)
            HINDI -> LanguageModel(context.getString(txt_language_hindi), isSelected, value, R.drawable.flag_hindi)
            HUNGARIAN -> LanguageModel(context.getString(txt_language_hungarian), isSelected, value, R.drawable.flag_hungarian)
            ICELANDIC -> LanguageModel(context.getString(txt_language_icelandic), isSelected, value, R.drawable.flag_icelandic)
            INDONESIAN -> LanguageModel(context.getString(txt_language_indonesian), isSelected, value, R.drawable.flag_indonesian)
            IRISH -> LanguageModel(context.getString(txt_language_irish), isSelected, value, R.drawable.flag_irish)
            ITALIAN -> LanguageModel(context.getString(txt_language_italian), isSelected, value, R.drawable.flag_italian)
            JAPANESE -> LanguageModel(context.getString(txt_language_japanese), isSelected, value, R.drawable.flag_japanese)
            KANNADA -> LanguageModel(context.getString(txt_language_kannada), isSelected, value, R.drawable.flag_hindi)
            KAZAKH -> LanguageModel(context.getString(txt_language_kazakh), isSelected, value, R.drawable.flag_kyrgyz)
            KHMER -> LanguageModel(context.getString(txt_language_khmer), isSelected, value, R.drawable.flag_cambodia)
            KOREAN -> LanguageModel(context.getString(txt_language_korean), isSelected, value, R.drawable.flag_korean)
            KYRGYZ -> LanguageModel(context.getString(txt_language_kyrgyz), isSelected, value, R.drawable.flag_kyrgyz)
            LAO -> LanguageModel(context.getString(txt_language_lao), isSelected, value, R.drawable.flag_lao)
            LATVIAN -> LanguageModel(context.getString(txt_language_latvian), isSelected, value, R.drawable.flag_latvian)
            LITHUANIAN -> LanguageModel(context.getString(txt_language_lithuanian), isSelected, value, R.drawable.flag_lithuanian)
            LUXEMBOURGISH -> LanguageModel(context.getString(txt_language_luxembourg), isSelected, value, R.drawable.flag_luxembourg)
            MACEDONIAN -> LanguageModel(context.getString(txt_language_macedonian), isSelected, value, R.drawable.flag_macedonian)
            MALAY -> LanguageModel(context.getString(txt_language_malay), isSelected, value, R.drawable.flag_malay)
            MALAYALAM -> LanguageModel(context.getString(txt_language_malayalam), isSelected, value, R.drawable.flag_hindi)
            MAORI -> LanguageModel(context.getString(txt_language_maori), isSelected, value, R.drawable.flag_english)
            MARATHI -> LanguageModel(context.getString(txt_language_marathi), isSelected, value, R.drawable.flag_hindi)
            MONGOLIAN -> LanguageModel(context.getString(txt_language_mongolian), isSelected, value, R.drawable.flag_mongolia)
            NEPALI -> LanguageModel(context.getString(txt_language_nepali), isSelected, value, R.drawable.flag_nepali)
            NORWEGIAN -> LanguageModel(context.getString(txt_language_norwegian), isSelected, value, R.drawable.flag_norwegian)
            PASHTO -> LanguageModel(context.getString(txt_language_pashto), isSelected, value, R.drawable.flag_afghanistan)
            PERSIAN -> LanguageModel(context.getString(txt_language_persian), isSelected, value, R.drawable.flag_persian)
            POLISH -> LanguageModel(context.getString(txt_language_polish), isSelected, value, R.drawable.flag_polish)
            PORTUGUESE -> LanguageModel(context.getString(txt_language_portuguese), isSelected, value, R.drawable.flag_portuguese)
            PORTUGUESE_BR -> LanguageModel(context.getString(txt_language_portuguese_br), isSelected, value, R.drawable.flag_brazil)
            PUNJABI -> LanguageModel(context.getString(txt_language_punjabi), isSelected, value, R.drawable.flag_hindi)
            ROMANIAN -> LanguageModel(context.getString(txt_language_romanian), isSelected, value, R.drawable.flag_romanian)
            RUSSIA -> LanguageModel(context.getString(txt_language_russian), isSelected, value, R.drawable.flag_russia)
            SERBIAN -> LanguageModel(context.getString(txt_language_serbian), isSelected, value, R.drawable.flag_serbian)
            SINHALA -> LanguageModel(context.getString(txt_language_sinhala), isSelected, value, R.drawable.flag_sinhala)
            SLOVAK -> LanguageModel(context.getString(txt_language_slovak), isSelected, value, R.drawable.flag_slovak)
            SLOVENIAN -> LanguageModel(context.getString(txt_language_slovenian), isSelected, value, R.drawable.flag_slovenian)
            SPANISH -> LanguageModel(context.getString(txt_language_spanish), isSelected, value, R.drawable.flag_spanish)
            SWAHILI -> LanguageModel(context.getString(txt_language_swahili), isSelected, value, R.drawable.flag_swahili)
            SWEDISH -> LanguageModel(context.getString(txt_language_swedish), isSelected, value, R.drawable.flag_swedish)
            TAMIL -> LanguageModel(context.getString(txt_language_tamil), isSelected, value, R.drawable.flag_sinhala)
            TELUGU -> LanguageModel(context.getString(txt_language_telugu), isSelected, value, R.drawable.flag_hindi)
            THAI -> LanguageModel(context.getString(txt_language_thai), isSelected, value, R.drawable.flag_thai)
            TURKISH -> LanguageModel(context.getString(txt_language_turkish), isSelected, value, R.drawable.flag_turkish)
            TURKMEN -> LanguageModel(context.getString(txt_language_turkmen), isSelected, value, R.drawable.flag_turkmen)
            UKRAINIAN -> LanguageModel(context.getString(txt_language_ukrainian), isSelected, value, R.drawable.flag_ukrainian)
            URDU -> LanguageModel(context.getString(txt_language_urdu), isSelected, value, R.drawable.flag_urdu)
            UZBEK -> LanguageModel(context.getString(txt_language_uzbek), isSelected, value, R.drawable.flag_uzbek)
            VIETNAMESE -> LanguageModel(context.getString(txt_language_vietnamese), isSelected, value, R.drawable.flag_vietnamese)
            ZULU -> LanguageModel(context.getString(txt_language_zulu), isSelected, value, R.drawable.flag_zulu)
        }
    }
    
//    fun getTextRecognition(): TextRecognizer {
//        return when(this) {
//            CHINESE -> TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
//            HINDI -> TextRecognition.getClient(DevanagariTextRecognizerOptions.Builder().build())
//            JAPANESE -> TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
//            KOREAN -> TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
//            else -> TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
//        }
//    }

    companion object {
        fun from(value: String): SupportedLanguage {
            val result = entries.find { it.value == value }
//            Locale.SIMPLIFIED_CHINESE
            return result ?: ENGLISH
        }

        fun displayList(): List<SupportedLanguage> {
            return listOf(ENGLISH, FRENCH, CHINESE_SIMPLE, JAPANESE)
        }
    }
}
