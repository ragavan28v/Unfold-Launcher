package com.volt.feature.gestures

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.navigation.NavController
import com.volt.core.domain.model.ActionType
import com.volt.core.domain.model.GestureType
import com.volt.core.domain.usecase.ResolveGestureActionUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GestureActionResolver @Inject constructor(
    private val resolveAction: ResolveGestureActionUseCase,
    @ApplicationContext private val context: Context
) {
    suspend fun execute(gestureType: GestureType, navController: NavController) {
        val binding = resolveAction(gestureType) ?: return
        when (binding.actionType) {
            ActionType.LAUNCH_APP -> {
                val packageName = binding.targetPackage ?: return
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                intent?.let { context.startActivity(it) }
            }
            ActionType.OPEN_INTENT -> {
                val uriStr = binding.targetIntentUri ?: return
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Ignored if package not found
                }
            }
            ActionType.OPEN_SCREEN -> {
                binding.targetScreenRoute?.let { route ->
                    navController.navigate(route)
                }
            }
            ActionType.SYSTEM_TOGGLE -> {
                // System toggles
            }
            ActionType.SHORTCUT -> {
                // Launch shortcuts
            }
        }
    }
}
