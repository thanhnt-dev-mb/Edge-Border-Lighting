package com.merryblue.baseapplication.ui.text.model

data class TextEffect(
    var id: Int,
    var content: Int,
    var icon: Int,
    var type: TextScrollerEffect
)

enum class TextScrollerEffect {
    SCROLLER,
    BLINK
}
