/**
 * Vox Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.vox.music.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.vox.music.R
import com.vox.music.constants.StudioBackdropBlurKey
import com.vox.music.constants.StudioCoverRadiusKey
import com.vox.music.constants.StudioDaylightScrimKey
import com.vox.music.constants.StudioGrainKey
import com.vox.music.constants.StudioGyroKey
import com.vox.music.constants.StudioHapticsKey
import com.vox.music.constants.StudioPlayerKey
import com.vox.music.constants.StudioRpmKey
import com.vox.music.constants.StudioSkinKey
import com.vox.music.constants.StudioTitleScaleKey
import com.vox.music.constants.StudioTitleWeightKey
import com.vox.music.ui.component.IosSettingsGroup
import com.vox.music.ui.component.IosSettingsItem
import com.vox.music.ui.ios.compat.AppTheme
import com.vox.music.ui.ios.compat.Slider
import com.vox.music.ui.ios.compat.Switch
import com.vox.music.ui.ios.compat.Text
import com.vox.music.utils.rememberPreference
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerStudioSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (enabled, onEnabled) = rememberPreference(StudioPlayerKey, defaultValue = false)
    val (skin, onSkin) = rememberPreference(StudioSkinKey, defaultValue = "COVER")
    val (radius, onRadius) = rememberPreference(StudioCoverRadiusKey, defaultValue = 20)
    val (rpm, onRpm) = rememberPreference(StudioRpmKey, defaultValue = 33)
    val (grain, onGrain) = rememberPreference(StudioGrainKey, defaultValue = 0.16f)
    val (blur, onBlur) = rememberPreference(StudioBackdropBlurKey, defaultValue = 36f)
    val (titleScale, onTitleScale) = rememberPreference(StudioTitleScaleKey, defaultValue = 1f)
    val (titleWeight, onTitleWeight) = rememberPreference(StudioTitleWeightKey, defaultValue = "BOLD")
    val (haptics, onHaptics) = rememberPreference(StudioHapticsKey, defaultValue = true)
    val (gyro, onGyro) = rememberPreference(StudioGyroKey, defaultValue = true)
    val (daylight, onDaylight) = rememberPreference(StudioDaylightScrimKey, defaultValue = true)

    SettingsLazyPage(
        navController = navController,
        title = "Player Studio",
        pageColor = AppTheme.colorScheme.background,
        horizontalPadding = 16.dp,
    ) {
        item(key = "studio") {
            Column {
                Text(
                    text = "A second Now Playing surface: vinyl, cassette, typographic and ambient skins with their own gestures and tone controls.",
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 20.dp),
                )

                IosSettingsGroup(
                    items = listOf(
                        IosSettingsItem(
                            icon = painterResource(R.drawable.graphic_eq),
                            title = { Text("Use Player Studio") },
                            description = { Text("Replaces the expanded player while it is on. Tap STUDIO in its corner to close it.") },
                            trailingContent = { Switch(checked = enabled, onCheckedChange = onEnabled) },
                            onClick = { onEnabled(!enabled) },
                        ),
                        IosSettingsItem(
                            title = { Text("Skin") },
                            description = {
                                Column(Modifier.fillMaxWidth()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        listOf("COVER", "VINYL", "CASSETTE", "TYPE").forEach { option ->
                                            ChoiceChip(option, skin == option) { onSkin(option) }
                                        }
                                    }
                                }
                            },
                        ),
                        IosSettingsItem(
                            title = { Text("Vinyl speed") },
                            description = {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    ChoiceChip("33⅓ RPM", rpm == 33) { onRpm(33) }
                                    ChoiceChip("45 RPM", rpm == 45) { onRpm(45) }
                                }
                            },
                        ),
                        IosSettingsItem(
                            title = { Text("Cover corner radius") },
                            description = {
                                Column(Modifier.fillMaxWidth()) {
                                    Text("$radius dp")
                                    Slider(
                                        value = radius.toFloat(),
                                        onValueChange = { onRadius(it.roundToInt()) },
                                        valueRange = 0f..32f,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            },
                        ),
                        IosSettingsItem(
                            title = { Text("Backdrop blur") },
                            description = {
                                Column(Modifier.fillMaxWidth()) {
                                    Text("${blur.roundToInt()} dp")
                                    Slider(
                                        value = blur,
                                        onValueChange = onBlur,
                                        valueRange = 0f..60f,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            },
                        ),
                        IosSettingsItem(
                            title = { Text("Film grain") },
                            description = {
                                Column(Modifier.fillMaxWidth()) {
                                    Text("${(grain * 100).roundToInt()}%")
                                    Slider(
                                        value = grain,
                                        onValueChange = onGrain,
                                        valueRange = 0f..1f,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            },
                        ),
                        IosSettingsItem(
                            title = { Text("Title size") },
                            description = {
                                Column(Modifier.fillMaxWidth()) {
                                    Text("${(titleScale * 100).roundToInt()}%")
                                    Slider(
                                        value = titleScale,
                                        onValueChange = onTitleScale,
                                        valueRange = 0.8f..1.4f,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            },
                        ),
                        IosSettingsItem(
                            title = { Text("Title weight") },
                            description = {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("LIGHT", "REGULAR", "BOLD").forEach { option ->
                                        ChoiceChip(option, titleWeight == option) { onTitleWeight(option) }
                                    }
                                }
                            },
                        ),
                        IosSettingsItem(
                            title = { Text("Scrubber haptics") },
                            description = { Text("A tick every 10% while dragging the timeline.") },
                            trailingContent = { Switch(checked = haptics, onCheckedChange = onHaptics) },
                            onClick = { onHaptics(!haptics) },
                        ),
                        IosSettingsItem(
                            title = { Text("Gyroscope parallax") },
                            description = { Text("Tilt the phone to shift the cover's depth and sheen.") },
                            trailingContent = { Switch(checked = gyro, onCheckedChange = onGyro) },
                            onClick = { onGyro(!gyro) },
                        ),
                        IosSettingsItem(
                            title = { Text("Daylight scrim boost") },
                            description = { Text("Thickens the dark veil outdoors so type keeps its contrast.") },
                            trailingContent = { Switch(checked = daylight, onCheckedChange = onDaylight) },
                            onClick = { onDaylight(!daylight) },
                        ),
                    ),
                )

                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }
}

@Composable
private fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val accent = androidx.compose.ui.graphics.Color(0xFFE7C8A0)
    val muted = AppTheme.colorScheme.onSurfaceVariant
    Text(
        text = label,
        color = if (selected) androidx.compose.ui.graphics.Color(0xFF0B0A09) else muted,
        style = AppTheme.typography.bodySmall,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) accent else muted.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    )
}
