package com.merryblue.baseapplication.ui.text

import android.content.Context
import android.graphics.Typeface

object TextScrollerFontCache {
    private val cache = LinkedHashMap<String, Typeface>()

    fun get(context: Context, path: String): Typeface {
        return cache.getOrPut(path) {
            runCatching {
                Typeface.createFromAsset(context.applicationContext.assets, path)
            }.getOrDefault(Typeface.DEFAULT_BOLD)
        }
    }
}
