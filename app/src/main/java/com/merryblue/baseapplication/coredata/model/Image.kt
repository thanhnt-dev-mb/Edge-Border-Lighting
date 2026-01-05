package com.merryblue.baseapplication.coredata.model

data class Image(
    var id: Long,
    var name: String,
    var path: String?,
    var date: Long
) {
    var isSelected = false
}
