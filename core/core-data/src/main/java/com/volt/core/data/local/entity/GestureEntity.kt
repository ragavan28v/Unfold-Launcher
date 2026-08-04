package com.volt.core.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.volt.core.domain.model.ActionType
import com.volt.core.domain.model.GestureBinding
import com.volt.core.domain.model.GestureType

@Entity(tableName = "gesture_bindings")
data class GestureEntity(
    @PrimaryKey val gestureType: String,
    val actionType: String,
    val targetPackage: String? = null,
    val targetIntentUri: String? = null,
    val targetScreenRoute: String? = null,
    val targetShortcutId: String? = null,
    val isUserModified: Boolean = false
) {
    fun toDomain(): GestureBinding {
        return GestureBinding(
            gestureType = GestureType.valueOf(gestureType),
            actionType = ActionType.valueOf(actionType),
            targetPackage = targetPackage,
            targetIntentUri = targetIntentUri,
            targetScreenRoute = targetScreenRoute,
            targetShortcutId = targetShortcutId,
            isUserModified = isUserModified
        )
    }

    companion object {
        fun fromDomain(binding: GestureBinding): GestureEntity {
            return GestureEntity(
                gestureType = binding.gestureType.name,
                actionType = binding.actionType.name,
                targetPackage = binding.targetPackage,
                targetIntentUri = binding.targetIntentUri,
                targetScreenRoute = binding.targetScreenRoute,
                targetShortcutId = binding.targetShortcutId,
                isUserModified = binding.isUserModified
            )
        }
    }
}
