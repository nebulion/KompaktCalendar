package com.kompakt.calendar

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.kompakt.calendar.ui.mmd.PagedList
import com.kompakt.calendar.ui.mmd.EinkColors
import com.kompakt.calendar.ui.mmd.EinkRowTokens
import com.kompakt.calendar.ui.mmd.EinkType
import com.kompakt.calendar.ui.mmd.HeaderAction
import com.kompakt.calendar.ui.mmd.RowDivider
import com.kompakt.calendar.ui.mmd.ScreenHeader
import com.kompakt.calendar.ui.mmd.SettingsRow
import com.mudita.mmd.components.text.TextMMD

/**
 * One system permission or power setting that reminders depend on.
 * [granted] is null when Android cannot report the state (DuraSpeed).
 */
data class PermissionItem(
    val title: String,
    val granted: Boolean?,
    val status: String,
    val open: (Context) -> Unit,
)

/** Reads the current state of every permission the app needs. */
fun permissionItems(context: Context, hasCalendarPermission: Boolean): List<PermissionItem> {
    val items = mutableListOf<PermissionItem>()

    items += PermissionItem(
        title = "Calendar access",
        granted = hasCalendarPermission,
        status = if (hasCalendarPermission) "Granted" else "Not granted. Tap to fix.",
        open = { it.startActivity(appDetailsIntent(it.packageName)) },
    )

    val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    } else {
        context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()
    }
    items += PermissionItem(
        title = "Notifications",
        granted = notificationsGranted,
        status = if (notificationsGranted) "Granted" else "Not granted. Tap to fix.",
        open = {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, it.packageName)
            } else {
                appDetailsIntent(it.packageName)
            }
            it.startActivity(intent)
        },
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val exact = context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        items += PermissionItem(
            title = "Exact alarms",
            granted = exact,
            status = if (exact) "Granted" else "Not granted. Tap to fix.",
            open = {
                it.startActivity(
                    Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        .setData(Uri.fromParts("package", it.packageName, null))
                )
            },
        )
    }

    val ignoringBattery = context.getSystemService(PowerManager::class.java)
        .isIgnoringBatteryOptimizations(context.packageName)
    items += PermissionItem(
        title = "Battery optimization",
        granted = ignoringBattery,
        status = if (ignoringBattery) "Off (recommended)" else "On. Reminders may be late.",
        open = {
            it.startActivity(
                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(Uri.parse("package:${it.packageName}"))
            )
        },
    )

    if (isDuraSpeedInstalled(context)) {
        items += PermissionItem(
            title = "DuraSpeed",
            granted = null,
            status = "Make sure KompaktCalendar is on in DuraSpeed.",
            open = ::openDuraSpeed,
        )
    }

    val overlay = Settings.canDrawOverlays(context)
    items += PermissionItem(
        title = "Display over other apps",
        granted = overlay,
        status = if (overlay) "Allowed" else "Needed for full-screen alerts",
        open = {
            it.startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${it.packageName}"))
            )
        },
    )

    return items
}

/** The one-line summary that Settings shows for the permissions page. */
fun permissionSummary(items: List<PermissionItem>): String {
    val missing = items.count { it.granted == false }
    return when (missing) {
        0 -> "All set"
        1 -> "1 needs attention"
        else -> "$missing need attention"
    }
}

/** Increments each time the screen resumes, so permission states read again after a trip to system Settings. */
@Composable
fun rememberResumeCount(onResume: () -> Unit = {}): Int {
    var count by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val latestOnResume by rememberUpdatedState(onResume)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                latestOnResume()
                count++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return count
}

@Composable
fun PermissionsScreen(
    navController: NavController,
    viewModel: CalendarViewModel,
) {
    val context = LocalContext.current
    val hasPermission by viewModel.hasPermission.collectAsState()
    val resumeCount = rememberResumeCount { viewModel.refreshPermission() }
    val items = remember(resumeCount, hasPermission) { permissionItems(context, hasPermission) }

    Scaffold(
        topBar = {
            ScreenHeader(
                title = "Permissions",
                navigationIcon = {
                    HeaderAction(Icons.AutoMirrored.Filled.ArrowBack, "Back", { navController.popBackStack() })
                },
            )
        },
    ) { paddingValues ->
        PagedList(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
                item {
                    TextMMD(
                        text = "Reminders need all of these. Tap a row to open its system setting.",
                        fontSize = EinkType.Body,
                        color = EinkColors.Ink,
                        modifier = Modifier.padding(
                            horizontal = EinkRowTokens.Inset,
                            vertical = 12.dp,
                        ),
                    )
                }
                itemsIndexed(items, key = { _, item -> item.title }) { index, item ->
                    if (index > 0) RowDivider()
                    SettingsRow(
                        title = item.title,
                        subtitle = item.status,
                        onClick = { item.open(context) },
                    ) {
                        Icon(
                            imageVector = when (item.granted) {
                                true -> Icons.Default.CheckCircle
                                false -> Icons.Default.Error
                                null -> Icons.Default.Info
                            },
                            contentDescription = null,
                            tint = EinkColors.Ink,
                            modifier = Modifier.size(EinkRowTokens.StatusGlyph),
                        )
                    }
                }
                item { Spacer(modifier = Modifier.height(EinkRowTokens.SectionTop)) }
        }
    }
}

private fun appDetailsIntent(packageName: String) =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).setData(Uri.fromParts("package", packageName, null))

private const val DURASPEED_PACKAGE = "com.mediatek.duraspeed"

private fun isDuraSpeedInstalled(context: Context): Boolean =
    try {
        context.packageManager.getPackageInfo(DURASPEED_PACKAGE, 0)
        true
    } catch (e: Exception) {
        false
    }

private fun openDuraSpeed(context: Context) {
    try {
        val intent = context.packageManager.getLaunchIntentForPackage(DURASPEED_PACKAGE)
        if (intent != null) {
            context.startActivity(intent)
        } else {
            // Fallback to explicit component
            context.startActivity(Intent().apply {
                component = ComponentName(DURASPEED_PACKAGE, "com.mediatek.duraspeed.DuraSpeedMainActivity")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        }
    } catch (e: Exception) {
        // If all direct attempts fail, open App Info which often has a link to DuraSpeed
        try {
            context.startActivity(appDetailsIntent(DURASPEED_PACKAGE))
        } catch (e2: Exception) {
            // Final fallback: open general settings
            context.startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }
}
