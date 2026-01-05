package com.merryblue.baseapplication.coredata.model

data class LanguageModel(
    var language: String,
    var isSelected: Boolean = false,
    var value: String,
    var flags: Int,
    var downloaded: Boolean = false
)
