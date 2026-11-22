/**
 * Vox Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Diagnostics: the small set of numbers worth knowing on a low-RAM phone —
 * process memory against the heap ceiling, system pressure, the size of the
 * caches Vox owns, and the build identity. Read-only; no player handle needed,
 * so it opens even when playback is stopped.
 */

package com.vox.music.ui.screens.settings

import android.app.ActivityManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.vox.music.BuildConfig
import com.vox.music.ui.component.IosSettingsGroup
import com.vox.music.ui.component.IosSettingsItem
import com.vox.music.ui.ios.compat.AppTheme
import com.vox.music.ui.ios.compat.Text
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    var rows by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }

    LaunchedEffect(Unit) {
        rows = withContext(Dispatchers.IO) {
            val runtime = Runtime.getRuntime()
            val used = runtime.totalMemory() - runtime.freeMemory()
            val manager = context.getSystemService(ActivityManager::class.java)
            val info = ActivityManager.MemoryInfo().also { manager?.getMemoryInfo(it) }
            val cache = runCatching {
                context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            }.getOrDefault(0L)
            val heapMb = runtime.maxMemory() / (1024 * 1024)
            listOf(
                "Version" to "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                "Process memory" to "${used / (1024 * 1024)} MB used of $heapMb MB heap",
                "System memory" to "${formatMb(info.availMem)} free of ${formatMb(info.totalMem)}",
                "Low-memory flag" to if (info.lowMemory) "set — system is under pressure" else "clear",
                "Cache" to "${cache / (1024 * 1024)} MB",
                "ABI" to android.os.Build.SUPPORTED_ABIS.firstOrNull().orEmpty(),
                "Android" to "${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})",
            )
        }
    }

    SettingsLazyPage(
        navController = navController,
        title = "Diagnostics",
        pageColor = AppTheme.colorScheme.background,
        horizontalPadding = 16.dp,
    ) {
        item(key = "diagnostics") {
            Column {
                Text(
                    text = "Live figures for tuning Vox on older hardware. Nothing here is uploaded.",
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 18.dp),
                )
                IosSettingsGroup(
                    items = rows.map { (label, value) ->
                        IosSettingsItem(
                            title = { Text(label) },
                            description = { Text(value) },
                        )
                    },
                )
                Spacer(Modifier.height(36.dp))
            }
        }
    }
}

private fun formatMb(bytes: Long): String =
    if (bytes >= 1024L * 1024L * 1024L) {
        "%.1f GB".format(bytes / (1024.0 * 1024.0 * 1024.0))
    } else {
        "${bytes / (1024 * 1024)} MB"
    }
