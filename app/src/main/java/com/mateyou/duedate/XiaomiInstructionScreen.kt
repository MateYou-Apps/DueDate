package com.mateyou.duedate

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun XiaomiInstructionScreen(onComplete: () -> Unit) {
    val context = LocalContext.current
    
    PermissionRequirementScreen(
        title = "Xiaomi Optimization",
        description = "On Xiaomi devices, enabling 'Autostart' is required for DueDate to reliably detect bills from incoming SMS. You may also set Battery Saver to 'No Restrictions' for better performance.",
        svgRes = R.raw.xiaomi_optimization,
        buttonText = "Open Settings",
        onButtonClick = {
            openXiaomiSettings(context)
            onComplete()
        }
    )
}

private fun openXiaomiSettings(context: Context) {
    try {
        // Try opening Autostart settings directly
        val intent = Intent()
        intent.component = ComponentName(
            "com.miui.securitycenter",
            "com.miui.permcenter.autostart.AutoStartManagementActivity"
        )
        context.startActivity(intent)
    } catch (e: Exception) {
        try {
            // Fallback to App Info screen
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
            }
            context.startActivity(intent)
        } catch (e2: Exception) {
            // Ultimate fallback to main settings
            context.startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }
}

fun isXiaomiDevice(): Boolean {
    val manufacturer = android.os.Build.MANUFACTURER
    return manufacturer.equals("Xiaomi", ignoreCase = true) || 
           manufacturer.equals("Redmi", ignoreCase = true) || 
           manufacturer.equals("POCO", ignoreCase = true)
}
