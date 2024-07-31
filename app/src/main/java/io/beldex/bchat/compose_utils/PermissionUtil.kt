package io.beldex.bchat.compose_utils

import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.core.content.ContextCompat

fun checkAndRequestPermissions(
    context: Context,
    permissions: Array<String>,
    launcher: ManagedActivityResultLauncher<Array<String>, Map<String, Boolean>>,
    onGranted: () -> Unit
) {
    if (
        permissions.all {
            ContextCompat.checkSelfPermission(
                context,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }
    ) {
        onGranted()
    } else {
        // Request permissions
        launcher.launch(permissions)
    }
}

@Composable
fun PermissionRequestLauncher(
    context: Context,
    permissions: Array<String>,
    onGranted: () -> Unit,
    onDeny: () -> Unit
) {
    val launcher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.all { it.value }) {
            onGranted()
        } else {
            onDeny()
        }
    }
    checkAndRequestPermissions(
        context = context,
        permissions = permissions,
        launcher = launcher,
        onGranted = onGranted
    )
}