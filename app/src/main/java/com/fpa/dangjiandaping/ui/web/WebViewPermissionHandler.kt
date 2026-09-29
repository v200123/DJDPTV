package com.fpa.dangjiandaping.ui.web

import android.Manifest
import android.content.pm.PackageManager
import android.webkit.PermissionRequest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/** Connects WebView camera/microphone requests to Android runtime permission dialogs. */
@Composable
internal fun rememberWebViewPermissionHandler(): (PermissionRequest) -> Unit {
    val context = LocalContext.current
    var pendingRequest by remember { mutableStateOf<PermissionRequest?>(null) }
    var requestedWebResources by remember { mutableStateOf<Array<String>>(emptyArray()) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val request = pendingRequest
        if (request != null) {
            val requiredPermissions = requestedWebResources.mapNotNull(::androidPermissionFor)
            val allRuntimePermissionsGranted = requiredPermissions.all { permission ->
                results[permission] == true || ContextCompat.checkSelfPermission(
                    context,
                    permission,
                ) == PackageManager.PERMISSION_GRANTED
            }
            if (allRuntimePermissionsGranted && requestedWebResources.isNotEmpty()) {
                request.grant(requestedWebResources)
            } else {
                request.deny()
            }
        }
        pendingRequest = null
        requestedWebResources = emptyArray()
    }

    DisposableEffect(Unit) {
        onDispose {
            pendingRequest?.deny()
            pendingRequest = null
        }
    }

    return remember(context, launcher) {
        { request: PermissionRequest ->
            val resources = request.resources?.toList().orEmpty()
            val supportedResources = resources.all { resource ->
                androidPermissionFor(resource) != null
            }
            val androidPermissions = resources.mapNotNull(::androidPermissionFor).distinct()
            if (resources.isEmpty() || !supportedResources) {
                request.deny()
            } else {
                val missing = androidPermissions.filter { permission ->
                    ContextCompat.checkSelfPermission(context, permission) !=
                        PackageManager.PERMISSION_GRANTED
                }
                if (missing.isEmpty()) {
                    request.grant(resources.toTypedArray())
                } else {
                    pendingRequest?.deny()
                    pendingRequest = request
                    requestedWebResources = resources.toTypedArray()
                    launcher.launch(missing.toTypedArray())
                }
            }
        }
    }
}

private fun androidPermissionFor(resource: String): String? = when (resource) {
    PermissionRequest.RESOURCE_VIDEO_CAPTURE -> Manifest.permission.CAMERA
    PermissionRequest.RESOURCE_AUDIO_CAPTURE -> Manifest.permission.RECORD_AUDIO
    else -> null
}