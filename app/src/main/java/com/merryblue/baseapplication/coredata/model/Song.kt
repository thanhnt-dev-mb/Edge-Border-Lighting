package com.merryblue.baseapplication.coredata.model

data class Song(
    val id: Long,
    val title: String,
    val path: String,
    val duration: Long,
    val albumId: Long,
    val albumName: String,
    val artistId: Long,
    val artistName: String,
    var albumUriString: String = "",
    val isLoading: Boolean = false
) {
    companion object {
        fun loading(): Song = Song(
            id = -1,
            title = "",
            path = "",
            duration = 0L,
            albumId = -1,
            albumName = "",
            artistId = -1,
            artistName = "",
            albumUriString = "",
            isLoading = true
        )
    }
}
