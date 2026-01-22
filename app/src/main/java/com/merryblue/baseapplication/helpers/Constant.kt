package com.merryblue.baseapplication.helpers


const val KEY_RECEIVE_DATA = "data_theme"

// screen
const val TYPE_PRESET = "type_preset"
const val TYPE_THEME = "type_theme"

// type
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
    const val ACTION_EDGE_OVERLAY_CHANGED = "com.merryblue.baseapplication.ACTION_EDGE_OVERLAY_CHANGED"
    const val ACTION_EDGE_OVERLAY_STOP = "com.merryblue.baseapplication.ACTION_EDGE_OVERLAY_STOP"
    const val ACTION_EDGE_OVERLAY_RESTART = "com.merryblue.baseapplication.ACTION_EDGE_OVERLAY_RESTART"
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
    const val KEY_EDGE = "key_edge"
    const val KEY_VIDEO = "key_video"
    const val KEY_RIPPLE = "key_ripple"
}