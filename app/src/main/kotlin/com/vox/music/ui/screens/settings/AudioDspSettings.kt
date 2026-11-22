/**
 * Vox Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.vox.music.ui.screens.settings

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.vox.music.LocalPlayerConnection
import com.vox.music.R
import com.vox.music.constants.BitPerfectKey
import com.vox.music.constants.DspBalanceKey
import com.vox.music.constants.DspCrossfeedKey
import com.vox.music.constants.DspHighPassKey
import com.vox.music.constants.DspMidnightKey
import com.vox.music.constants.DspMonoKey
import com.vox.music.constants.DspPreampKey
import com.vox.music.ui.component.IosSettingsGroup
import com.vox.music.ui.component.IosSettingsItem
import com.vox.music.ui.ios.compat.AppTheme
import com.vox.music.ui.ios.compat.Slider
import com.vox.music.ui.ios.compat.Switch
import com.vox.music.ui.ios.compat.Text
import com.vox.music.utils.rememberPreference
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioDspSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val playerConnection = LocalPlayerConnection.current

    val (balance, onBalance) = rememberPreference(DspBalanceKey, defaultValue = 0f)
    val (crossfeed, onCrossfeed) = rememberPreference(DspCrossfeedKey, defaultValue = 0f)
    val (preamp, onPreamp) = rememberPreference(DspPreampKey, defaultValue = 0f)
    val (mono, onMono) = rememberPreference(DspMonoKey, defaultValue = false)
    val (highPass, onHighPass) = rememberPreference(DspHighPassKey, defaultValue = false)
    val (midnight, onMidnight) = rememberPreference(DspMidnightKey, defaultValue = false)
    val (bitPerfect, _) = rememberPreference(BitPerfectKey, defaultValue = false)

    val balancePercent = (abs(balance) * 100).roundToInt()
    val balanceLabel = when {
        balance <= -0.02f -> stringResource(R.string.dsp_balance_left, balancePercent)
        balance >= 0.02f -> stringResource(R.string.dsp_balance_right, balancePercent)
        else -> stringResource(R.string.dsp_balance_center)
    }

    SettingsLazyPage(
        navController = navController,
        title = stringResource(R.string.dsp_settings),
        pageColor = AppTheme.colorScheme.background,
        horizontalPadding = 16.dp,
    ) {
        item(key = "dsp_content") {
            Column {
                Text(
                    text = stringResource(
                        if (bitPerfect) R.string.dsp_bit_perfect_bypass else R.string.dsp_settings_desc,
                    ),
                    style = AppTheme.typography.bodyMedium,
                    color = AppTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp, bottom = 20.dp),
                )

                IosSettingsGroup(
                    title = stringResource(R.string.dsp_tone),
                    items = listOf(
                        IosSettingsItem(
                            title = { Text(stringResource(R.string.dsp_balance)) },
                            description = {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(balanceLabel)
                                    Slider(
                                        value = balance,
                                        onValueChange = onBalance,
                                        valueRange = -1f..1f,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            },
                        ),
                        IosSettingsItem(
                            title = { Text(stringResource(R.string.dsp_crossfeed)) },
                            description = {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(stringResource(R.string.dsp_crossfeed_desc))
                                    Slider(
                                        value = crossfeed,
                                        onValueChange = onCrossfeed,
                                        valueRange = 0f..1f,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            },
                        ),
                        IosSettingsItem(
                            title = { Text(stringResource(R.string.dsp_preamp)) },
                            description = {
                                Column(Modifier.fillMaxWidth()) {
                                    Text(
                                        "${preamp.roundToInt()} dB · " +
                                            stringResource(R.string.dsp_preamp_desc),
                                    )
                                    Slider(
                                        value = preamp,
                                        onValueChange = onPreamp,
                                        valueRange = -12f..12f,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                            },
                        ),
                    ),
                )

                Spacer(modifier = Modifier.height(20.dp))

                IosSettingsGroup(
                    items = listOf(
                        IosSettingsItem(
                            icon = painterResource(R.drawable.graphic_eq),
                            title = { Text(stringResource(R.string.dsp_mono)) },
                            description = { Text(stringResource(R.string.dsp_mono_desc)) },
                            trailingContent = { Switch(checked = mono, onCheckedChange = onMono) },
                            onClick = { onMono(!mono) },
                        ),
                        IosSettingsItem(
                            icon = painterResource(R.drawable.graphic_eq),
                            title = { Text(stringResource(R.string.dsp_highpass)) },
                            description = { Text(stringResource(R.string.dsp_highpass_desc)) },
                            trailingContent = { Switch(checked = highPass, onCheckedChange = onHighPass) },
                            onClick = { onHighPass(!highPass) },
                        ),
                        IosSettingsItem(
                            icon = painterResource(R.drawable.graphic_eq),
                            title = { Text(stringResource(R.string.dsp_midnight)) },
                            description = { Text(stringResource(R.string.dsp_midnight_desc)) },
                            trailingContent = { Switch(checked = midnight, onCheckedChange = onMidnight) },
                            onClick = { onMidnight(!midnight) },
                        ),
                    ),
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Press and hold to hear the unprocessed signal; release restores it.
                val service = playerConnection?.service
                IosSettingsGroup(
                    items = listOf(
                        IosSettingsItem(
                            title = { Text(stringResource(R.string.dsp_ab_hold)) },
                            description = { Text(stringResource(R.string.dsp_ab_hold_desc)) },
                            trailingContent = {
                                Text(
                                    text = stringResource(R.string.dsp_ab_badge),
                                    style = AppTheme.typography.bodyMedium,
                                    color = AppTheme.colorScheme.primary,
                                    modifier = Modifier.pointerInput(service) {
                                        detectTapGestures(
                                            onPress = {
                                                service?.setDspRawBypass(true)
                                                tryAwaitRelease()
                                                service?.setDspRawBypass(false)
                                            },
                                        )
                                    },
                                )
                            },
                        ),
                    ),
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.dsp_passthrough_note),
                    style = AppTheme.typography.bodySmall,
                    color = AppTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )

                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }
}
