// SPDX-License-Identifier: AGPL-3.0-or-later
// Copyright (C) 2026 Tobias Mueller and KabelWacht contributors

package com.github.muelli.kabelwacht.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

/**
 * Android 13+ hides all of our notifications — the automation service's and the
 * "always-on VPN failed" one — until POST_NOTIFICATIONS is granted at runtime.
 * Everything keeps working without it; the user just isn't told.
 */
class NotificationPermissionState internal constructor(
    private val grantedState: MutableState<Boolean>,
    private val onRequest: () -> Unit,
) {
    val granted: Boolean get() = grantedState.value

    /** Shows the system prompt, or the app's notification settings once Android stopped asking. */
    fun request() {
        if (!granted) onRequest()
    }
}

fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

@Composable
fun rememberNotificationPermissionState(): NotificationPermissionState {
    val context = LocalContext.current
    val granted = remember { mutableStateOf(hasNotificationPermission(context)) }
    // Once denied without a rationale (twice, or "don't ask again"), Android no
    // longer shows the prompt, so the only way left is the settings screen.
    val openSettingsNext = remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        granted.value = isGranted
        val activity = context.findActivity()
        openSettingsNext.value = !isGranted && activity != null &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.POST_NOTIFICATIONS,
            )
    }

    // Pick up a change made in system settings when the user comes back.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            granted.value = hasNotificationPermission(context)
        }
    }

    return remember {
        NotificationPermissionState(granted) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@NotificationPermissionState
            if (openSettingsNext.value) {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            } else {
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
