package com.merryblue.baseapplication.coredata.model

data class Album(val id: Long, var name: String, var cover: String?) {
    var count: Int = 0
    var images = arrayListOf<Image>()
}
