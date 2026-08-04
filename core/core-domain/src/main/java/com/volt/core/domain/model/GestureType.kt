package com.volt.core.domain.model

enum class GestureType {
    SWIPE_LEFT_1F,
    SWIPE_RIGHT_1F,
    SWIPE_LEFT_2F,
    SWIPE_RIGHT_2F,
    SWIPE_UP_1F,
    SWIPE_UP_2F,
    PINCH,
    LONG_PRESS_HOLD,
    DOUBLE_TAP_BG,
    EDGE_SWIPE
}

enum class ActionType {
    LAUNCH_APP,
    OPEN_INTENT,
    OPEN_SCREEN,
    SYSTEM_TOGGLE,
    SHORTCUT
}
