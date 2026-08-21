package com.unfold.feature.gestures

import android.content.Context
import android.content.ComponentName
import android.content.Intent
import android.content.pm.LauncherApps
import android.net.Uri
import android.os.Process
import androidx.navigation.NavController
import com.unfold.core.domain.model.ActionType
import com.unfold.core.domain.model.GestureType
import com.unfold.core.domain.navigation.UnfoldRoute
import com.unfold.core.domain.usecase.ResolveGestureActionUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GestureActionResolver @Inject constructor(
    private val resolveAction: ResolveGestureActionUseCase,
    @ApplicationContext private val context: Context
) {
    suspend fun execute(gestureType: GestureType, navController: NavController) {
        if (gestureType == GestureType.EDGE_SWIPE) {
            returnToHome(navController)
            return
        }

        val binding = resolveAction(gestureType) ?: return

        val horizontalGestures = setOf(
            GestureType.SWIPE_LEFT_1F,
            GestureType.SWIPE_RIGHT_1F,
            GestureType.SWIPE_LEFT_2F,
            GestureType.SWIPE_RIGHT_2F
        )

        if (!binding.isUserModified && gestureType in horizontalGestures) {
            return
        }

        when (binding.actionType) {
            ActionType.LAUNCH_APP -> {
                val packageName = binding.targetPackage ?: return
                val intent = runCatching {
                    val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                        setPackage(packageName)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    val resolveInfo = context.packageManager.queryIntentActivities(launcherIntent, 0)
                        .firstOrNull()
                    resolveInfo?.activityInfo?.let { activityInfo ->
                        Intent(Intent.ACTION_MAIN).apply {
                            addCategory(Intent.CATEGORY_LAUNCHER)
                            component = ComponentName(activityInfo.packageName, activityInfo.name)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    } ?: context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }.getOrNull()
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
                val packageName = binding.targetPackage ?: return
                val shortcutId = binding.targetShortcutId
                if (shortcutId.isNullOrBlank()) {
                    val fallback = context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    fallback?.let { context.startActivity(it) }
                } else {
                    try {
                        val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return
                        launcherApps.startShortcut(
                            packageName,
                            shortcutId,
                            null,
                            null,
                            Process.myUserHandle()
                        )
                    } catch (_: Exception) {
                        val fallback = context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        fallback?.let { context.startActivity(it) }
                    }
                }
            }
        }
    }

    private fun returnToHome(navController: NavController) {
        if (!navController.popBackStack(UnfoldRoute.Home.route, inclusive = false)) {
            navController.navigate(UnfoldRoute.Home.route) {
                launchSingleTop = true
                restoreState = true
            }
        }
    }
}

