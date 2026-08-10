package com.unfold.core.domain.model

data class GestureBinding(
    val gestureType: GestureType,
    val actionType: ActionType,
    val targetPackage: String? = null,
    val targetIntentUri: String? = null,
    val targetScreenRoute: String? = null,
    val targetShortcutId: String? = null,
    val isUserModified: Boolean = false
)

