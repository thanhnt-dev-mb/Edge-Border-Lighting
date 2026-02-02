package com.merryblue.baseapplication.helpers


const val KEY_RECEIVE_DATA = "data_theme"

// screen
const val TYPE_PRESET = "type_preset"
const val TYPE_THEME = "type_theme"
const val KEY_IS_GALLERY = "key_is_gallery"
const val KEY_IS_CUSTOM = "key_is_custom"

// type
const val KEY_ALL = "all"
const val EDGE_MOST = "edge/most"
const val EDGE_REWARD_DAY = "edge/rewardday"
const val EDGE_TRENDING = "edge/trending"
const val EDGE_FIM = "edge/fim"
const val RIPPLE_MAGICAL_BORDERS = "ripple/magical_borders"
const val RIPPLE_PREMIUM = "ripple/premium"
const val RIPPLE_NATURE_SPAZ = "ripple/nature/spaz"
const val RIPPLE_NATURE_INDS = "ripple/nature/lnds"
const val RIPPLE_NATURE_D = "ripple/nature/b"
const val RIPPLE_NATURE_LIVE = "ripple/nature/live"
const val RIPPLE_ABSTRACT_CQ = "ripple/abstract/cq"
const val RIPPLE_ABSTRACT_ABSCT = "ripple/abstract/absct"
const val RIPPLE_TOP_PICS = "ripple/top_pics"
const val RIPPLE_RIPPLE = "ripple/ripple"

object ServiceState {

    // EdgeLightingService
    const val ACTION_EDGE_OVERLAY_CHANGED = "com.merryblue.baseapplication.ACTION_EDGE_OVERLAY_CHANGED"
    const val ACTION_EDGE_OVERLAY_STOP = "com.merryblue.baseapplication.ACTION_EDGE_OVERLAY_STOP"
    const val ACTION_EDGE_OVERLAY_RESTART = "com.merryblue.baseapplication.ACTION_EDGE_OVERLAY_RESTART"

    // EdgeLightingWallpaperService
    const val ACTION_EDGE_WALLPAPER_STATE_CHANGED = "com.merryblue.baseapplication.ACTION_EDGE_WALLPAPER_STATE_CHANGED"
    const val ACTION_EDGE_WALLPAPER_STATE_STOP = "com.merryblue.baseapplication.ACTION_EDGE_WALLPAPER_STATE_STOP"

    // VideoWallpaperService
    const val ACTION_VIDEO_WALLPAPER_STATE_CHANGED = "com.merryblue.baseapplication.wallpaper.ACTION_VIDEO_WALLPAPER_STATE_CHANGED"

    // RippleWallpaperService
    const val ACTION_RIPPLE_BG_CHANGED = "com.merryblue.baseapplication.ACTION_RIPPLE_BG_CHANGED"
}

object WallpaperType {
    const val TYPE_EDGE = "edge"
    const val TYPE_VIDEO = "video"
    const val TYPE_STATIC = "static"
    const val TYPE_RIPPLE = "ripple"
}

object BackgroundType {
    const val BACKGROUND_COLOR = 0
    const val BACKGROUND_RES = 1
    const val BACKGROUND_URL = 2
    const val BACKGROUND_URI = 3
}

object EdgeStyle {
    const val EDGE_LINEAR = 0
    const val EDGE_PATTERN = 1
    const val EDGE_NONE = 2
}

object PreviewType {
    const val EDGE_WALLPAPER_SCREEN = "EDGE_WALLPAPER_SCREEN"
    const val STATIC_WALLPAPER_SCREEN = "STATIC_WALLPAPER_SCREEN"
    const val RIPPLE_WALLPAPER_SCREEN = "RIPPLE_WALLPAPER_SCREEN"
}