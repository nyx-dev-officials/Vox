@file:OptIn(ExperimentalTextApi::class)

package com.vox.music

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Comment
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import com.vox.music.constants.IsFirstRunKey
import com.vox.music.designsystem.MotionTokens
import com.vox.music.designsystem.VoxDesignTokens
import com.vox.music.ui.theme.VoxTheme
import com.vox.music.ui.theme.VoxType
import com.vox.music.ui.utils.safeOpenUri
import com.vox.music.utils.dataStore
import com.vox.music.utils.get
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import androidx.datastore.preferences.core.edit

data class OnboardingPageInfo(
    val content: @Composable (onUpdateScrollState: (Boolean) -> Unit) -> Unit
)

class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        val isFirstRun = dataStore.get(IsFirstRunKey, true)
        val forceShow = intent.getBooleanExtra("FORCE_SHOW", false)

        if (!isFirstRun && !forceShow) {
            finishOnboarding()
            return
        }

        enableEdgeToEdge()
        setContent {
            VoxTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    WelcomePagerScreen(onFinished = {
                        lifecycleScope.launch {
                            dataStore.edit { it[IsFirstRunKey] = false }
                            if (forceShow) {
                                finish()
                            } else {
                                finishOnboarding()
                            }
                        }
                    })
                }
            }
        }
    }

    private fun finishOnboarding() {
        startActivity(Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_PLAY_INTRO, true))
        finish()
        // The intro picks up from the black splash; a window slide would break the handoff.
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}

/**
 * Onboarding, set as four pages of a magazine.
 *
 * The whole flow is type and rules: an eyebrow, a display headline with the
 * signal word in vermilion, a hairline-divided list, and one filled bar at the
 * bottom. There is no card stack, no pastel app-icon grid, and no glass except
 * the one row surface that needs to sit over content.
 */
@Composable
fun WelcomePagerScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    var canInstallPackages by remember {
        mutableStateOf(
            if (BuildConfig.FLAVOR.contains("gms") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)
            } else true
        )
    }

    var isLastPageScrolledToEnd by remember { mutableStateOf(true) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    hasNotificationPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    canInstallPackages = runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasNotificationPermission = isGranted }
    )

    val installParamsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            canInstallPackages = runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)
        }
    }

    val requestNotifications = {
        if (hasNotificationPermission) {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val pages = listOf(
        OnboardingPageInfo(
            content = { _ ->
                Column(modifier = Modifier.fillMaxSize()) {
                    OnboardingHeadline(
                        eyebrow = "MADE BY NYX",
                        lead = stringResource(com.vox.music.R.string.welcome_to),
                        signal = stringResource(com.vox.music.R.string.app_name).uppercase(),
                        body = stringResource(com.vox.music.R.string.welcome_preparing_subtitle),
                    )

                    Spacer(Modifier.weight(1f))

                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        RotatingShapeContainer(modifier = Modifier.size(232.dp))
                    }

                    Spacer(Modifier.weight(1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        MetaLabel(
                            if (BuildConfig.FLAVOR.contains("gms", ignoreCase = true)) "GMS EDITION" else "FOSS EDITION"
                        )
                        MetaLabel("v${BuildConfig.VERSION_NAME}")
                    }
                }
            }
        ),
        OnboardingPageInfo(
            content = { _ ->
                Column(modifier = Modifier.fillMaxSize()) {
                    OnboardingHeadline(
                        eyebrow = "BEFORE WE START",
                        lead = stringResource(com.vox.music.R.string.perm_required),
                        signal = stringResource(com.vox.music.R.string.perm_permissions),
                        body = stringResource(com.vox.music.R.string.perm_intro_text),
                    )

                    Spacer(Modifier.height(28.dp))

                    Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                        VoxRow(
                            icon = rememberVectorPainter(Icons.Rounded.Notifications),
                            title = stringResource(com.vox.music.R.string.perm_notif_title),
                            description = stringResource(com.vox.music.R.string.perm_notif_desc),
                            onClick = requestNotifications,
                            trailing = {
                                Switch(
                                    checked = hasNotificationPermission,
                                    onCheckedChange = { requestNotifications() },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = VoxDesignTokens.Colors.OnAccentSignal,
                                        checkedTrackColor = VoxDesignTokens.Colors.AccentSignal,
                                        uncheckedThumbColor = VoxDesignTokens.Colors.SecondaryLabel,
                                        uncheckedTrackColor = Color.Transparent,
                                    ),
                                )
                            },
                        )

                        if (BuildConfig.FLAVOR.contains("gms", ignoreCase = true)) {
                            VoxRow(
                                icon = rememberVectorPainter(Icons.Rounded.SystemUpdate),
                                title = stringResource(com.vox.music.R.string.perm_install_title),
                                description = stringResource(com.vox.music.R.string.perm_install_desc),
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        installParamsLauncher.launch(
                                            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                                                .setData(Uri.parse("package:${context.packageName}"))
                                        )
                                    }
                                },
                                trailing = { MetaLabel(if (canInstallPackages) "GRANTED" else "REQUIRED") },
                            )
                        }
                    }
                }
            }
        ),
        OnboardingPageInfo(
            content = { _ ->
                Column(modifier = Modifier.fillMaxSize()) {
                    OnboardingHeadline(
                        eyebrow = "OPEN SOURCE",
                        lead = "Join our",
                        signal = "Community",
                        body = "Vox depends on the people who use it. Star the repository, or come and argue about playback engines with us.",
                    )

                    Spacer(Modifier.height(28.dp))

                    VoxRow(
                        icon = rememberVectorPainter(Icons.Rounded.Star),
                        title = "Star on GitHub",
                        description = "Help us reach more people by starring the repository.",
                        onClick = {
                            uriHandler.safeOpenUri(context, "https://github.com/nyx-dev-officials/Vox")
                        },
                        trailing = { MetaLabel("GITHUB") },
                    )
                    VoxRow(
                        icon = rememberVectorPainter(Icons.Rounded.Comment),
                        title = "Join Discord",
                        description = "Builds, updates and the people who make them.",
                        onClick = {
                            uriHandler.safeOpenUri(context, "https://discord.gg/Ejeb4cmzfd")
                        },
                        trailing = { MetaLabel("DISCORD") },
                    )
                }
            }
        ),
        OnboardingPageInfo(
            content = { onUpdateScroll ->
                Column(modifier = Modifier.fillMaxSize()) {
                    OnboardingHeadline(
                        eyebrow = "THE TOUR",
                        lead = stringResource(com.vox.music.R.string.feat_discover),
                        signal = stringResource(com.vox.music.R.string.feat_features),
                        body = stringResource(com.vox.music.R.string.feat_intro),
                    )

                    Spacer(Modifier.height(20.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        val scrollState = rememberScrollState()
                        val isAtBottom by remember {
                            derivedStateOf {
                                val max = scrollState.maxValue
                                max == 0 || scrollState.value >= (max - 24)
                            }
                        }
                        LaunchedEffect(isAtBottom) { onUpdateScroll(isAtBottom) }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                        ) {
                            VoxRow(
                                icon = rememberVectorPainter(Icons.Rounded.Lyrics),
                                title = stringResource(com.vox.music.R.string.feat_lyrics_title),
                                description = stringResource(com.vox.music.R.string.feat_lyrics_desc),
                                onClick = null,
                            )
                            VoxRow(
                                icon = rememberVectorPainter(Icons.Rounded.CloudDownload),
                                title = stringResource(com.vox.music.R.string.feat_download_title),
                                description = stringResource(com.vox.music.R.string.feat_download_desc),
                                onClick = null,
                            )
                            VoxRow(
                                icon = rememberVectorPainter(Icons.Rounded.HighQuality),
                                title = stringResource(com.vox.music.R.string.feat_quality_title),
                                description = stringResource(com.vox.music.R.string.feat_quality_desc),
                                onClick = null,
                            )
                            VoxRow(
                                icon = rememberVectorPainter(Icons.Rounded.BlurOn),
                                title = stringResource(com.vox.music.R.string.feat_glass_title),
                                description = stringResource(com.vox.music.R.string.feat_glass_desc),
                                onClick = null,
                            )
                            VoxRow(
                                icon = rememberVectorPainter(Icons.Rounded.Comment),
                                title = stringResource(com.vox.music.R.string.feat_discord_title),
                                description = stringResource(com.vox.music.R.string.feat_discord_desc),
                                onClick = null,
                            )
                            if (BuildConfig.FLAVOR.contains("gms", ignoreCase = true)) {
                                VoxRow(
                                    icon = rememberVectorPainter(Icons.Rounded.SystemUpdate),
                                    title = stringResource(com.vox.music.R.string.feat_update_title),
                                    description = stringResource(com.vox.music.R.string.feat_update_desc),
                                    onClick = null,
                                )
                            }
                            VoxRow(
                                icon = rememberVectorPainter(Icons.Rounded.Gavel),
                                title = stringResource(com.vox.music.R.string.feat_license_title),
                                description = stringResource(com.vox.music.R.string.feat_license_desc),
                                onClick = null,
                            )
                            VoxRow(
                                icon = rememberVectorPainter(Icons.Rounded.Terminal),
                                title = stringResource(com.vox.music.R.string.feat_github_title),
                                description = stringResource(com.vox.music.R.string.feat_github_desc),
                                onClick = null,
                            )
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                }
            }
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val isFirstPage = pagerState.currentPage == 0
    val isLastPage = pagerState.currentPage == pages.size - 1
    val pageSpec = spring<Float>(dampingRatio = 0.9f, stiffness = 320f)

    BackHandler(enabled = !isFirstPage) {
        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1, animationSpec = pageSpec) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 24.dp)
            .widthIn(max = 700.dp)
    ) {
        OnboardingRail(
            step = pagerState.currentPage + 1,
            total = pages.size,
            modifier = Modifier.padding(top = 20.dp),
        )

        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            beyondViewportPageCount = pages.size - 1,
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) { index ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 28.dp)
                    .graphicsLayer {
                        val offset = pagerState.getOffsetDistanceInPages(index)
                        val distance = offset.absoluteValue.coerceIn(0f, 1f)
                        // Crossfade plus a short horizontal cut. No shrink: scaling a
                        // page of type is what made the old transition read as a card.
                        alpha = 1f - distance
                        translationX = offset * size.width * 0.18f
                    }
            ) {
                pages[index].content { }
            }
        }

        StepBar(
            selected = pagerState.currentPage,
            total = pages.size,
            modifier = Modifier.padding(top = 16.dp),
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 24.dp)
                .height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val backAlpha by animateFloatAsState(
                targetValue = if (isFirstPage) 0f else 1f,
                animationSpec = MotionTokens.settle(),
                label = "backAlpha",
            )
            if (backAlpha > 0.01f) {
                GhostButton(
                    text = stringResource(com.vox.music.R.string.back_button_desc),
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1, animationSpec = pageSpec)
                        }
                    },
                    modifier = Modifier.graphicsLayer { alpha = backAlpha },
                )
            }

            val enabled = !isLastPage || isLastPageScrolledToEnd
            val enabledAlpha by animateFloatAsState(
                targetValue = if (enabled) 1f else 0.35f,
                animationSpec = MotionTokens.settle(),
                label = "nextAlpha",
            )
            SignalButton(
                text = if (isLastPage) {
                    stringResource(com.vox.music.R.string.get_started)
                } else {
                    stringResource(com.vox.music.R.string.next)
                },
                onClick = {
                    if (isLastPage) {
                        if (isLastPageScrolledToEnd) onFinished()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1, animationSpec = pageSpec)
                        }
                    }
                },
                modifier = Modifier.weight(1f).graphicsLayer { alpha = enabledAlpha },
            )
        }
    }
}

/** Eyebrow, display headline with a signal second line, and a body paragraph. */
@Composable
private fun OnboardingHeadline(
    eyebrow: String,
    lead: String,
    signal: String,
    body: String,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = eyebrow,
            style = VoxType.Eyebrow,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = lead,
            style = VoxType.Masthead.copy(fontSize = 40.sp, lineHeight = 43.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = signal,
            style = VoxType.Masthead.copy(fontSize = 40.sp, lineHeight = 45.sp),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Step counter, set as a printed folio: "01 / 04" over a progress rail. */
@Composable
private fun OnboardingRail(step: Int, total: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = "VOX", style = VoxType.Eyebrow, color = MaterialTheme.colorScheme.onSurface)
        Text(
            text = "${step.toString().padStart(2, '0')} / ${total.toString().padStart(2, '0')}",
            style = VoxType.Eyebrow,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Four hairlines; the current one fills with signal. No dots, no capsules. */
@Composable
private fun StepBar(selected: Int, total: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        repeat(total) { index ->
            val color by animateColorAsState(
                targetValue = if (index == selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)
                },
                animationSpec = tween(240),
                label = "step$index",
            )
            Box(
                Modifier
                    .weight(if (index == selected) 2.2f else 1f)
                    .height(2.dp)
                    .background(color)
            )
        }
    }
}

/**
 * A list row on a hairline grid. The glyph is monochrome inside a hairline
 * square — no pastel tile — so the eye lands on the title, not the decoration.
 */
@Composable
fun VoxRow(
    icon: Painter,
    title: String,
    description: String,
    onClick: (() -> Unit)?,
    trailing: @Composable (() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val wash by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = MotionTokens.snap(),
        label = "rowWash",
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f * wash)
                )
                .clickable(
                    enabled = onClick != null,
                    interactionSource = interaction,
                    indication = null,
                    onClick = { onClick?.invoke() },
                )
                .padding(vertical = 16.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    modifier = Modifier.size(19.dp),
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            trailing?.let {
                Spacer(Modifier.width(12.dp))
                it()
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .height(VoxDesignTokens.Metrics.Hairline)
                .background(VoxDesignTokens.Colors.Hairline)
        )
    }
}

/** Small uppercase metadata, the only place tracking goes this wide. */
@Composable
private fun MetaLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = VoxType.Eyebrow,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
    )
}

@Composable
private fun SignalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = MotionTokens.snap(),
        label = "signalPress",
    )

    Box(
        modifier = modifier
            .height(56.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.primary)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            style = VoxType.Eyebrow.copy(letterSpacing = 1.6.sp),
            color = VoxDesignTokens.Colors.OnAccentSignal,
        )
    }
}

@Composable
private fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color.Transparent)
            .then(
                Modifier.clickable(onClick = onClick)
            )
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text.uppercase(),
            style = VoxType.Eyebrow.copy(letterSpacing = 1.6.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The Vox mark: a signal rosette rotating under the logo cut out of the canvas.
 * Slow enough to read as a printed seal rather than a loading spinner.
 */
@Composable
fun RotatingShapeContainer(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "rosette")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(32000),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rotation",
    )
    val ringColor = VoxDesignTokens.Colors.AccentSignal.copy(alpha = 0.22f)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation }) {
            drawCircle(color = ringColor, radius = size.minDimension * 0.5f, style = Stroke(1.dp.toPx()))
            drawCircle(color = ringColor, radius = size.minDimension * 0.34f, style = Stroke(1.dp.toPx()))
            // Twelve ticks, like a press registration mark.
            repeat(12) { index ->
                val angle = Math.toRadians((index * 30).toDouble())
                val inner = size.minDimension * 0.40f
                val outer = size.minDimension * 0.47f
                val cx = size.width / 2f
                val cy = size.height / 2f
                drawLine(
                    color = ringColor,
                    start = Offset(cx + inner * kotlin.math.cos(angle).toFloat(), cy + inner * kotlin.math.sin(angle).toFloat()),
                    end = Offset(cx + outer * kotlin.math.cos(angle).toFloat(), cy + outer * kotlin.math.sin(angle).toFloat()),
                    strokeWidth = 1.dp.toPx(),
                    cap = StrokeCap.Square,
                )
            }
        }

        Icon(
            painter = painterResource(com.vox.music.R.drawable.vox_logo),
            contentDescription = null,
            tint = VoxDesignTokens.Colors.AccentSignal,
            modifier = Modifier.size(112.dp),
        )
    }
}
