/**
 * Vox Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Storage breakdown: what Vox is actually holding on disk, in four buckets that
 * can be acted on independently. Sizes are measured off the main thread when the
 * page opens; nothing here is cached, because a stale storage number is worse
 * than a slow one.
 */

package com.vox.music.ui.screens.settings

import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.vox.music.ui.component.IosSettingsGroup
import com.vox.music.ui.component.IosSettingsItem
import com.vox.music.ui.ios.compat.AppTheme
import com.vox.music.ui.ios.compat.Text
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class StorageBucket(val label: String, val bytes: Long, val clearable: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageBreakdownScreen(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    var buckets by remember { mutableStateOf<List<StorageBucket>>(emptyList()) }
    var working by remember { mutableStateOf(false) }

    suspend fun measure(): List<StorageBucket> = withContext(Dispatchers.IO) {
        val cache = context.cacheDir
        val files = context.filesDir
        val external = context.getExternalFilesDir(null)
        @Suppress("DEPRECATION")
        val pictures = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "Vox",
        )
        listOf(
            StorageBucket("Audio & image cache", sizeOf(cache), clearable = true),
            StorageBucket("Downloads & offline data", sizeOf(files), clearable = false),
            StorageBucket("App storage", sizeOf(external), clearable = false),
            StorageBucket("Saved share cards", sizeOf(pictures), clearable = false),
        )
    }

    LaunchedEffect(Unit) { buckets = measure() }

    val total = buckets.sumOf { it.bytes }.coerceAtLeast(1L)
    val accent = androidx.compose.ui.graphics.Color(0xFFE7C8A0)

    SettingsLazyPage(
        navController = navController,
        title = "Storage",
        pageColor = AppTheme.colorScheme.background,
        horizontalPadding = 16.dp,
    ) {
        item(key = "storage") {
            Column {
                Text(
                    text = "Everything Vox keeps on this device, measured just now.",
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 18.dp),
                )

                if (buckets.isNotEmpty()) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(12.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(AppTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
                    ) {
                        buckets.forEachIndexed { index, bucket ->
                            val weight = (bucket.bytes.toFloat() / total).coerceAtLeast(0.01f)
                            androidx.compose.foundation.layout.Box(
                                Modifier
                                    .weight(weight)
                                    .fillMaxWidth()
                                    .background(
                                        if (index == 0) accent
                                        else accent.copy(alpha = (0.55f - index * 0.12f).coerceAtLeast(0.15f)),
                                    ),
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = formatBytes(total) + " total",
                        style = AppTheme.typography.bodySmall,
                        color = AppTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }

                Spacer(Modifier.height(14.dp))
                IosSettingsGroup(
                    items = buckets.map { bucket ->
                        IosSettingsItem(
                            title = { Text(bucket.label) },
                            description = { Text(formatBytes(bucket.bytes)) },
                        )
                    } + listOf(
                        IosSettingsItem(
                            title = { Text(if (working) "Clearing…" else "Clear audio & image cache") },
                            description = { Text("Keeps downloads, playlists and lyrics.") },
                            enabled = !working,
                            onClick = {
                                working = true
                            },
                            trailingContent = {
                                if (working) {
                                    Text("…")
                                } else {
                                    Text(
                                        "CLEAR",
                                        color = accent,
                                        modifier = Modifier.clickable {
                                            working = true
                                        },
                                    )
                                }
                            },
                        ),
                    ),
                )

                LaunchedEffect(working) {
                    if (!working) return@LaunchedEffect
                    withContext(Dispatchers.IO) {
                        context.cacheDir.listFiles()?.forEach { child ->
                            runCatching { child.deleteRecursively() }
                        }
                    }
                    buckets = measure()
                    working = false
                }

                Spacer(Modifier.height(36.dp))
            }
        }
    }
}

private fun sizeOf(dir: File?): Long {
    if (dir == null || !dir.exists()) return 0L
    return runCatching {
        dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }.getOrDefault(0L)
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1L shl 30 -> "%.2f GB".format(bytes / (1L shl 30).toDouble())
    bytes >= 1L shl 20 -> "%.1f MB".format(bytes / (1L shl 20).toDouble())
    bytes >= 1L shl 10 -> "%.0f KB".format(bytes / 1024.0)
    else -> "$bytes B"
}
