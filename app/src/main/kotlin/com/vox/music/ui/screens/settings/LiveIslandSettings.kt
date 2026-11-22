/**
 * Vox Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.vox.music.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.vox.music.R
import com.vox.music.constants.IslandYOffsetKey
import com.vox.music.constants.LiveIslandPillKey
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
fun LiveIslandSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val (enabled, onEnabled) = rememberPreference(LiveIslandPillKey, defaultValue = false)
    val (yOffset, onYOffset) = rememberPreference(IslandYOffsetKey, defaultValue = 0)

    SettingsLazyPage(
        navController = navController,
        title = stringResource(R.string.live_island),
        pageColor = AppTheme.colorScheme.background,
        horizontalPadding = 16.dp,
    ) {
        item(key = "island_content") {
            Column {
                Text(
                    text = stringResource(R.string.live_island_desc),
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 20.dp),
                )

                IosSettingsGroup(
                    items = listOf(
                        IosSettingsItem(
                            icon = painterResource(R.drawable.graphic_eq),
                            title = { Text(stringResource(R.string.live_island)) },
                            description = { Text(stringResource(R.string.live_island_desc)) },
                            trailingContent = { Switch(checked = enabled, onCheckedChange = onEnabled) },
                            onClick = { onEnabled(!enabled) },
                        ),
                        IosSettingsItem(
                            title = { Text(stringResource(R.string.live_island_offset)) },
                            description = {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(
                                        "${yOffset.roundToInt()} dp · " +
                                            stringResource(R.string.live_island_offset_desc),
                                    )
                                    Slider(
                                        value = yOffset.toFloat(),
                                        onValueChange = { onYOffset(it.roundToInt()) },
                                        valueRange = -40f..40f,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            },
                        ),
                    ),
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.live_island_permission),
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )

                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }
}
