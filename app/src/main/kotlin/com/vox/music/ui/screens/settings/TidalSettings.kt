/**
 * Vox Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.vox.music.ui.screens.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.vox.music.R
import com.vox.music.constants.EnableTidalStreamingKey
import com.vox.music.constants.TidalEngineMode
import com.vox.music.constants.TidalEngineModeKey
import com.vox.music.constants.TidalQualityCap
import com.vox.music.constants.TidalQualityCapKey
import com.vox.music.ui.component.IosSettingsGroup
import com.vox.music.ui.component.IosSettingsItem
import com.vox.music.ui.component.ModernSwitch
import com.vox.music.ui.component.SymbolIcon
import com.vox.music.ui.component.sf
import com.vox.music.ui.ios.compat.AppTheme
import com.vox.music.ui.ios.compat.RadioButton
import com.vox.music.ui.ios.compat.Text
import com.vox.music.utils.rememberEnumPreference
import com.vox.music.utils.rememberPreference
import com.vox.music.utils.tidal.TidalAuth
import androidx.compose.material3.TextButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TidalSettings(
    navController: NavController,
    scrollBehavior: TopAppBarScrollBehavior,
) {
    val context = LocalContext.current
    val (enabled, onEnabledChange) = rememberPreference(
        EnableTidalStreamingKey,
        defaultValue = false,
    )
    val (engineMode, onEngineModeChange) = rememberEnumPreference(
        TidalEngineModeKey,
        defaultValue = TidalEngineMode.HYBRID,
    )
    val (cap, onCapChange) = rememberEnumPreference(
        TidalQualityCapKey,
        defaultValue = TidalQualityCap.MAX,
    )
    var loggedIn by remember { mutableStateOf(TidalAuth.isLoggedIn(context)) }

    // The browser round-trip leaves and re-enters the app; refresh the session
    // state on resume instead of polling.
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                loggedIn = TidalAuth.isLoggedIn(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    SettingsPage(
        navController = navController,
        title = stringResource(R.string.tidal_settings),
        pageColor = AppTheme.colorScheme.background,
        horizontalPadding = 24.dp,
    ) {
        Text(
            text = stringResource(R.string.enable_tidal_streaming_desc),
            style = AppTheme.typography.bodyMedium,
            color = AppTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp, top = 16.dp),
        )

        val containerColor by animateColorAsState(
            targetValue = if (enabled) AppTheme.colorScheme.primaryContainer
            else AppTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            label = "tidalBanner",
        )
        val contentColor = if (enabled) AppTheme.colorScheme.onPrimaryContainer
        else AppTheme.colorScheme.onSurfaceVariant

        Card(
            onClick = { onEnabledChange(!enabled) },
            shape = RoundedCornerShape(50),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.enable_tidal_streaming),
                    style = AppTheme.typography.titleMedium,
                    color = contentColor,
                )
                ModernSwitch(checked = enabled, onCheckedChange = onEnabledChange)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        IosSettingsGroup(
            title = stringResource(R.string.tidal_engine),
            items = TidalEngineMode.entries.map { mode ->
                IosSettingsItem(
                    leadingContent = {
                        RadioButton(
                            selected = engineMode == mode,
                            onClick = null,
                            enabled = enabled,
                        )
                    },
                    title = { Text(mode.toLabel()) },
                    enabled = enabled,
                    onClick = { onEngineModeChange(mode) },
                )
            },
        )

        Spacer(modifier = Modifier.height(24.dp))

        IosSettingsGroup(
            title = stringResource(R.string.tidal_quality_cap),
            items = TidalQualityCap.entries.map { quality ->
                IosSettingsItem(
                    leadingContent = {
                        RadioButton(
                            selected = cap == quality,
                            onClick = null,
                            enabled = enabled,
                        )
                    },
                    title = { Text(quality.toLabel()) },
                    enabled = enabled,
                    onClick = { onCapChange(quality) },
                )
            },
        )

        Spacer(modifier = Modifier.height(24.dp))

        IosSettingsGroup(
            title = stringResource(R.string.tidal_settings),
            items = listOf(
                IosSettingsItem(
                    title = {
                        Text(
                            if (loggedIn) stringResource(R.string.tidal_session_active)
                            else stringResource(R.string.tidal_session_off),
                        )
                    },
                    trailingContent = {
                        when {
                            !TidalAuth.isConfigured -> Unit
                            loggedIn -> TextButton(onClick = {
                                TidalAuth.logout(context)
                                loggedIn = false
                            }) {
                                Text(stringResource(R.string.tidal_logout))
                            }
                            else -> TextButton(onClick = {
                                TidalAuth.beginLogin(context)?.let { intent ->
                                    runCatching { context.startActivity(intent) }
                                }
                            }) {
                                Text(stringResource(R.string.tidal_login))
                            }
                        }
                    },
                ),
            ),
        )

        if (!TidalAuth.isConfigured) {
            Text(
                text = stringResource(R.string.tidal_not_configured),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Row(
            modifier = Modifier.padding(top = 24.dp),
            verticalAlignment = Alignment.Top,
        ) {
            SymbolIcon(
                symbol = sf("info.circle"),
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp),
                tint = AppTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.tidal_beta_info),
                style = AppTheme.typography.bodyMedium,
                color = AppTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(modifier = Modifier.height(36.dp))
    }
}
