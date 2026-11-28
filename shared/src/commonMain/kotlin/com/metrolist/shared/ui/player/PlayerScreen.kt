/*
 * Metrolist Project (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.metrolist.shared.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.graphics.vector.ImageVector
import com.metrolist.shared.generated.resources.action_download
import com.metrolist.shared.generated.resources.save
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.shared.cast.CastState
import com.metrolist.shared.generated.resources.Res
import com.metrolist.shared.generated.resources.action_like
import com.metrolist.shared.generated.resources.add_to_playlist
import com.metrolist.shared.generated.resources.album_art
import com.metrolist.shared.generated.resources.artists
import com.metrolist.shared.generated.resources.audio_output
import com.metrolist.shared.generated.resources.collapse
import com.metrolist.shared.generated.resources.details
import com.metrolist.shared.generated.resources.edit_lyrics
import com.metrolist.shared.generated.resources.line_synced
import com.metrolist.shared.generated.resources.lyrics
import com.metrolist.shared.generated.resources.lyrics_not_found
import com.metrolist.shared.generated.resources.lyrics_provider_lrclib
import com.metrolist.shared.generated.resources.lyrics_provider_lrclib_format
import com.metrolist.shared.generated.resources.menu_seek_backward
import com.metrolist.shared.generated.resources.menu_seek_forward
import com.metrolist.shared.generated.resources.more_options
import com.metrolist.shared.generated.resources.next
import com.metrolist.shared.generated.resources.now_playing
import com.metrolist.shared.generated.resources.now_playing_upper
import com.metrolist.shared.generated.resources.pause
import com.metrolist.shared.generated.resources.play
import com.metrolist.shared.generated.resources.playback_reconnecting
import com.metrolist.shared.generated.resources.previous
import com.metrolist.shared.generated.resources.queue
import com.metrolist.shared.generated.resources.repeat
import com.metrolist.shared.generated.resources.seek_feedback_description
import com.metrolist.shared.generated.resources.seek_feedback_seconds
import com.metrolist.shared.generated.resources.share
import com.metrolist.shared.generated.resources.show
import com.metrolist.shared.generated.resources.shuffle
import com.metrolist.shared.generated.resources.unknown
import com.metrolist.shared.generated.resources.unlike
import com.metrolist.shared.playback.LocalPlayerSession
import com.metrolist.shared.playback.SleepTimerState
import com.metrolist.shared.settings.Settings
import com.metrolist.shared.ui.LocalIsInPipMode
import com.metrolist.shared.ui.LocalIsTv
import com.metrolist.shared.ui.components.BottomSheet
import com.metrolist.shared.ui.components.BottomSheetState
import com.metrolist.shared.ui.components.COLLAPSED_ANCHOR
import com.metrolist.shared.ui.components.CrossfadeArtwork
import com.metrolist.shared.ui.components.LoadingScreen
import com.metrolist.shared.ui.components.PopupVisibility
import com.metrolist.shared.ui.components.SleepTimerDialog
import com.metrolist.shared.ui.components.TopRoundedSurfaceShape
import com.metrolist.shared.ui.components.adjacentArtworkUrls
import com.metrolist.shared.ui.components.focusOutline
import com.metrolist.shared.ui.components.images.AsyncImage
import com.metrolist.shared.ui.components.liquidGlass
import com.metrolist.shared.ui.components.lyrics.LyricsOffsetEditor
import com.metrolist.shared.ui.components.rememberBottomSheetState
import com.metrolist.shared.ui.components.sheetBackContent
import com.metrolist.shared.ui.haptics.HapticEvent
import com.metrolist.shared.ui.haptics.rememberMetrolistHaptics
import com.metrolist.shared.ui.icons.MetrolistIcons
import com.metrolist.shared.ui.navigation.NavigationBackHandler
import com.metrolist.shared.ui.navigation.blockInput
import com.metrolist.shared.ui.utils.supportsNativeBlur
import com.metrolist.shared.viewmodels.PlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * Mathematical smoothstep S-curve scrim gradient that eliminates color banding.
 */
fun smoothScrimBrush(
    from: Color,
    to: Color,
    startFraction: Float = 0f,
    endFraction: Float = 1f,
    startY: Float = 0f,
    endY: Float = Float.POSITIVE_INFINITY,
    steps: Int = 24,
): Brush =
    Brush.verticalGradient(
        colorStops =
            Array(steps + 1) { i ->
                val t = i / steps.toFloat()
                val position = startFraction + (endFraction - startFraction) * t
                position to lerp(from, to, t * t * (3f - 2f * t))
            },
        startY = startY,
        endY = endY,
    )

@Composable
internal fun Modifier.artworkSwipe(
    enabled: Boolean,
    sensitivity: Float,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
): Modifier {
    if (!enabled) return this
    val currentOnPrevious by rememberUpdatedState(onPrevious)
    val currentOnNext by rememberUpdatedState(onNext)
    var totalDrag by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    val offset =
        animateFloatAsState(
            targetValue = totalDrag,
            animationSpec =
                if (dragging) {
                    snap()
                } else {
                    spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium,
                    )
                },
            label = "artworkSwipe",
        )
    return graphicsLayer { translationX = offset.value }
        .pointerInput(sensitivity) {
            detectHorizontalDragGestures(
                onDragStart = { dragging = true },
                onHorizontalDrag = { change, dragAmount ->
                    change.consume()
                    totalDrag += dragAmount
                },
                onDragEnd = {
                    val threshold = artworkSwipeThresholdPx(size.width.toFloat(), sensitivity)
                    when {
                        totalDrag >= threshold -> currentOnPrevious()
                        totalDrag <= -threshold -> currentOnNext()
                    }
                    dragging = false
                    totalDrag = 0f
                },
                onDragCancel = {
                    dragging = false
                    totalDrag = 0f
                },
            )
        }
}

@Composable
internal fun Modifier.artworkSeekGestures(
    seekEnabled: Boolean,
    stepMs: Long,
    onTap: (() -> Unit)?,
    tapLabel: String?,
    progressive: Boolean = false,
    onSeek: (Long) -> Unit,
): Modifier {
    if (!seekEnabled && onTap != null) {
        return focusOutline(RoundedCornerShape(16.dp)).clickable(
            interactionSource = null,
            indication = null,
            onClickLabel = tapLabel,
            onClick = onTap,
        )
    }
    if (!seekEnabled) return this
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnSeek by rememberUpdatedState(onSeek)
    val currentProgressive by rememberUpdatedState(progressive)
    var skipMultiplier by remember { mutableStateOf(1) }
    var lastTapMark by remember { mutableStateOf<TimeMark?>(null) }
    return this
        .focusOutline(RoundedCornerShape(16.dp))
        .semantics {
            if (onTap != null) {
                role = Role.Button
                onClick(label = tapLabel) {
                    onTap()
                    true
                }
            }
        }.onKeyEvent { event ->
            if (onTap != null && (event.key == Key.Enter || event.key == Key.DirectionCenter || event.key == Key.Spacebar)) {
                if (event.type == KeyEventType.KeyUp) onTap()
                true
            } else {
                false
            }
        }.focusable(enabled = onTap != null)
        .pointerInput(stepMs) {
            detectTapGestures(
                onTap = { currentOnTap?.invoke() },
                onDoubleTap = { offset ->
                    val elapsedMs = lastTapMark?.elapsedNow()?.inWholeMilliseconds ?: Long.MAX_VALUE
                    skipMultiplier =
                        if (currentProgressive) nextSeekMultiplier(skipMultiplier, elapsedMs) else 1
                    lastTapMark = TimeSource.Monotonic.markNow()
                    val amount = stepMs * skipMultiplier
                    currentOnSeek(if (offset.x < size.width / 2f) -amount else amount)
                },
            )
        }
}

internal fun nextSeekMultiplier(
    previousMultiplier: Int,
    elapsedMs: Long,
    thresholdMs: Long = 1_000L,
): Int = if (elapsedMs in 0 until thresholdMs) previousMultiplier + 1 else 1

internal data class SeekFeedback(
    val deltaMs: Long,
    val nonce: Int,
)

@Composable
internal fun SeekFeedbackOverlay(
    feedback: SeekFeedback?,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val deltaMs = feedback?.deltaMs ?: 0L
    val forward = deltaMs >= 0L
    val label = stringResource(Res.string.seek_feedback_seconds, (abs(deltaMs) / 1000L).toInt())
    val directionLabel = stringResource(if (forward) Res.string.menu_seek_forward else Res.string.menu_seek_backward)
    val seekDescription = stringResource(Res.string.seek_feedback_description, directionLabel, label)
    val density = LocalDensity.current
    val nudgeDistance = with(density) { 12.dp.toPx() }
    val supportsBlur = supportsNativeBlur()
    val labelShadow =
        remember(density) {
            Shadow(
                color = Color.Black.copy(alpha = 0.55f),
                offset = Offset(0f, with(density) { 1.dp.toPx() }),
                blurRadius = with(density) { 6.dp.toPx() },
            )
        }

    val arrowNudge = remember { Animatable(0f) }
    LaunchedEffect(feedback?.nonce) {
        if (feedback == null) return@LaunchedEffect
        arrowNudge.snapTo(if (forward) nudgeDistance else -nudgeDistance)
        arrowNudge.animateTo(0f, animationSpec = spatialSpec)
    }

    AnimatedVisibility(
        visible = visible,
        enter = scaleIn(initialScale = 0.7f, animationSpec = spatialSpec) + fadeIn(animationSpec = effectsSpec),
        exit = scaleOut(targetScale = 0.9f, animationSpec = spatialSpec) + fadeOut(animationSpec = effectsSpec),
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier =
                Modifier
                    .size(96.dp)
                    .semantics(mergeDescendants = true) {
                        contentDescription = seekDescription
                        liveRegion = LiveRegionMode.Polite
                    },
        ) {
            Box(modifier = Modifier.size(32.dp)) {
                if (supportsBlur) {
                    Icon(
                        imageVector = MetrolistIcons.Outlined.FastForward,
                        contentDescription = null,
                        tint = Color.Black.copy(alpha = 0.45f),
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .offset(y = 1.dp)
                                .blur(6.dp, BlurredEdgeTreatment.Unbounded)
                                .graphicsLayer {
                                    if (!forward) scaleX = -1f
                                    translationX = arrowNudge.value
                                },
                    )
                }
                Icon(
                    imageVector = MetrolistIcons.Outlined.FastForward,
                    contentDescription = null,
                    tint = Color.White,
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                if (!forward) scaleX = -1f
                                translationX = arrowNudge.value
                            },
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, shadow = labelShadow),
            )
        }
    }
}

/**
 * Authentic SimpMusic-styled Now Playing screen:
 * - Dynamic dominant ambient backdrop smoothly blending into OLED Black #05030E.
 * - Top bar: Down chevron, NOW PLAYING uppercase header + Playlist/Queue title, 3-dot overflow menu.
 * - Centered 16.dp squircle artwork card with soft ambient glow.
 * - Single-line synchronized lyric preview with smooth 300ms crossfade.
 * - Metadata row: Bold white track title, secondary artist, Favorite heart (filled red when liked).
 * - Thin Material 3 seekbar slider with elapsed and duration timestamps.
 * - Transport controls: Shuffle, Previous, solid 64.dp white circle Play/Pause, Next, Repeat.
 * - Quick actions: Info, Audio output/headphones, Lyrics toggle, Add to playlist (+), Queue.
 * - Scrolled content below fold: Tinted dominant-colored Lyrics Card with LRCLIB attribution, Artists Card.
 */
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    bottomSheetState: BottomSheetState,
    onClose: () -> Unit = {},
    onNavigateToAlbum: (String) -> Unit = {},
    onNavigateToArtist: (String) -> Unit = {},
    onNavigateToListenTogether: () -> Unit = {},
    onNavigateToIntegrations: () -> Unit = {},
    modifier: Modifier = Modifier,
    requestPlayPauseFocus: Boolean = false,
    onPlayPauseFocusRequested: () -> Unit = {},
    onLyricsVisibleChange: (Boolean) -> Unit = {},
    isScreenVisible: Boolean = true,
) {
    val isPlayerContentVisible by remember(bottomSheetState) {
        derivedStateOf { bottomSheetState.value > bottomSheetState.collapsedBound && !bottomSheetState.isCollapsed }
    }
    val playerContentState =
        resolvePlayerContentState(
            isContentVisible = isPlayerContentVisible,
            isSettledExpanded = bottomSheetState.isSettledExpanded,
            isScreenVisible = isScreenVisible,
            isInPipMode = LocalIsInPipMode.current,
        )
    val uiState by viewModel.uiState.collectWhilePlayerActive(isPlayerContentVisible && isScreenVisible)
    val windowSize = LocalWindowInfo.current.containerSize
    val isTv = LocalIsTv.current

    val currentSongs = remember(viewModel) { viewModel.uiState.map { it.currentSong }.distinctUntilChanged() }
    val artworkPrefetchUrls by remember(viewModel) {
        viewModel.uiState.map { it.adjacentArtworkUrls() }.distinctUntilChanged()
    }.collectAsState(viewModel.uiState.value.adjacentArtworkUrls())
    val playerThumbnailStyle = Settings.shared.playerThumbnailStyle
    val artworkRequestSize = playerArtworkRequestSize(windowSize, playerThumbnailStyle, false)
    val trackPresentation = rememberPlayerTrackPresentation(currentSongs, artworkRequestSize, artworkPrefetchUrls, isScreenVisible)
    val currentSong = trackPresentation.song
    val repeatMode = uiState.repeatMode
    val isShuffleOn = uiState.isShuffleOn
    val duration = if (currentSong?.id == uiState.currentSong?.id) uiState.duration else currentSong?.duration ?: 0L
    val isLiked = currentSong?.isLiked == true

    val displaysPlayingSong = currentSong?.id == uiState.currentSong?.id
    val toggleDisplayedLike = { if (displaysPlayingSong) viewModel.toggleLike() }

    SideEffect { onLyricsVisibleChange(uiState.isLyricsVisible) }

    val platform = LocalPlayerPlatform.current
    val playerConnection = LocalPlayerSession.current
    val isReconnecting by playerConnection?.isReconnecting?.collectWhilePlayerActive(isPlayerContentVisible && isScreenVisible)
        ?: remember { mutableStateOf(false) }
    val isPlaybackLoading by playerConnection?.isPlaybackLoading?.collectWhilePlayerActive(isPlayerContentVisible && isScreenVisible)
        ?: remember { mutableStateOf(false) }
    val wantsPlayback by viewModel.wantsPlaybackState.collectWhilePlayerActive(isPlayerContentVisible && isScreenVisible)
    val sleepTimerState by playerConnection?.sleepTimerState?.collectWhilePlayerActive(isPlayerContentVisible && isScreenVisible)
        ?: remember { mutableStateOf(SleepTimerState()) }
    val playbackResource by playerConnection?.currentPlaybackResource?.collectWhilePlayerActive(isPlayerContentVisible && isScreenVisible)
        ?: remember { mutableStateOf(null) }
    val isListenTogetherGuest = platform.isListenTogetherGuest()
    val castState by playerConnection?.castState?.collectWhilePlayerActive(isPlayerContentVisible && isScreenVisible)
        ?: remember { mutableStateOf(CastState()) }
    val isPlaying =
        uiState.isPlaying ||
            (wantsPlayback && !castState.isConnected && (isPlaybackLoading || playbackResource == null))

    var showDeviceConnectSheet by remember { mutableStateOf(false) }
    var showPlayerMenu by remember { mutableStateOf(false) }
    var showSongDetails by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showLyricsMenu by remember { mutableStateOf(false) }
    var showLyricsOffsetEditor by rememberSaveable(currentSong?.id) { mutableStateOf(false) }

    val shareLyricsAction = platform.lyricsShareAction(viewModel)

    val hapticManager = rememberMetrolistHaptics()
    var seekFeedback by remember { mutableStateOf<SeekFeedback?>(null) }
    var seekFeedbackVisible by remember { mutableStateOf(false) }
    val artworkSeekStepMs = Settings.shared.seekDurationSeconds * 1000L
    val onArtworkSeek: (Long) -> Unit = { deltaMs ->
        viewModel.seekBy(deltaMs)
        hapticManager.perform(HapticEvent.PRESS)
        seekFeedback = SeekFeedback(deltaMs = deltaMs, nonce = (seekFeedback?.nonce ?: 0) + 1)
    }
    LaunchedEffect(seekFeedback) {
        if (seekFeedback != null) {
            seekFeedbackVisible = true
            delay(650)
            seekFeedbackVisible = false
        }
    }

    val isPlayerBackHandlerEnabled by remember(bottomSheetState) {
        derivedStateOf { bottomSheetState.progress > 0.2f && !bottomSheetState.isDismissed }
    }

    val artworkSwipeEnabled =
        shouldEnableArtworkSwipe(
            enabled = Settings.shared.swipeThumbnail,
            isTv = isTv,
            touchExplorationEnabled = platform.isTouchExplorationEnabled(),
        )

    val playPauseFocusRequester = remember { FocusRequester() }
    LaunchedEffect(currentSong?.id) {
        currentSong?.let { song ->
            viewModel.loadLyrics(song)
        }
    }
    LaunchedEffect(bottomSheetState.isExpanded, requestPlayPauseFocus, currentSong?.id) {
        if (bottomSheetState.isExpanded && requestPlayPauseFocus) {
            runCatching { playPauseFocusRequester.requestFocus() }
            onPlayPauseFocusRequested()
        }
    }

    platform.PlayerWindowEffects(
        hideSystemBars = false,
        lockLandscape = false,
        lightStatusBars = false,
    )

    val dominantColor =
        remember(uiState.dominantColor, uiState.gradientColors) {
            uiState.dominantColor.takeIf { it != Color.Unspecified && it.alpha > 0.05f }
                ?: uiState.gradientColors.firstOrNull()
                ?: Color(0xFF8B5CF6)
        }
    val oledBlack = Color(0xFF05030E)

    // Current synchronized lyric preview
    val currentPosition by viewModel.currentPosition.collectWhilePlayerActive(isPlayerContentVisible && isScreenVisible)
    val currentLineIndex =
        remember(uiState.lyrics, currentPosition, uiState.lyricsOffsetMs) {
            val pos = currentPosition + uiState.lyricsOffsetMs
            var idx = -1
            for (i in uiState.lyrics.indices) {
                if (uiState.lyrics[i].time <= pos) {
                    idx = i
                } else {
                    break
                }
            }
            idx
        }
    val activeLineText =
        remember(currentLineIndex, uiState.lyrics) {
            if (currentLineIndex in uiState.lyrics.indices) {
                uiState.lyrics[currentLineIndex].text
            } else {
                ""
            }
        }

    val queueSession = rememberQueueSession(viewModel)
    val queueFocusRequester = remember { FocusRequester() }
    val queueFocusModifier = if (isTv) Modifier.focusRequester(queueFocusRequester).focusGroup() else Modifier

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val maxHeight = maxHeight
        var queueStage by rememberSaveable { mutableStateOf(QueueSheetStage.COLLAPSED) }
        val targetQueueHeight =
            when (queueStage) {
                QueueSheetStage.COLLAPSED -> 0.dp
                QueueSheetStage.HALF -> maxHeight * 0.54f
                QueueSheetStage.FULL -> maxHeight - 36.dp
            }
        val animatedQueueHeight by animateDpAsState(
            targetValue = targetQueueHeight,
            animationSpec = spring<androidx.compose.ui.unit.Dp>(stiffness = Spring.StiffnessMediumLow),
            label = "queueHeightAnim",
        )

        NavigationBackHandler(enabled = isPlayerBackHandlerEnabled) {
            when {
                showLyricsOffsetEditor -> showLyricsOffsetEditor = false
                queueStage == QueueSheetStage.FULL -> queueStage = QueueSheetStage.HALF
                queueStage == QueueSheetStage.HALF -> queueStage = QueueSheetStage.COLLAPSED
                uiState.isLyricsVisible -> viewModel.setLyricsVisible(false)
                else -> onClose()
            }
        }

        // SimpMusic OLED Background: Dominant color smoothly fading into #05030E
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(oledBlack)
                    .drawBehind {
                        val gradientHeight = size.height * 0.72f
                        drawRect(
                            brush =
                                smoothScrimBrush(
                                    from = dominantColor.copy(alpha = 0.65f),
                                    to = oledBlack,
                                    startY = 0f,
                                    endY = gradientHeight,
                                ),
                            size = Size(size.width, gradientHeight),
                        )
                    },
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
            ) {
                // TOP BAR
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            hapticManager.perform(HapticEvent.PRESS)
                            onClose()
                        },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = MetrolistIcons.Outlined.KeyboardArrowDown,
                            contentDescription = stringResource(Res.string.collapse),
                            tint = Color.White,
                            modifier = Modifier.size(28.dp),
                        )
                    }

                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = stringResource(Res.string.now_playing_upper),
                            style =
                                MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp,
                                ),
                            color = Color.White.copy(alpha = 0.65f),
                            maxLines = 1,
                        )
                        val queueOrPlaylistTitle =
                            uiState.queueTitle?.takeIf { it.isNotBlank() }
                                ?: currentSong?.album
                                ?: stringResource(Res.string.now_playing)
                        Text(
                            text = queueOrPlaylistTitle,
                            style =
                                MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }

                    IconButton(
                        onClick = {
                            hapticManager.perform(HapticEvent.PRESS)
                            showPlayerMenu = true
                        },
                        modifier = Modifier.size(40.dp),
                    ) {
                        Icon(
                            imageVector = MetrolistIcons.Outlined.MoreVert,
                            contentDescription = stringResource(Res.string.more_options),
                            tint = Color.White,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }

                // ARTWORK: Centered 16.dp squircle card with soft ambient shadow
                val artworkMaxHeight = (maxHeight - 340.dp).coerceIn(160.dp, this@BoxWithConstraints.maxWidth - 48.dp)
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(horizontal = 28.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = artworkMaxHeight)
                                .aspectRatio(1f)
                                .shadow(
                                    elevation = 16.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    spotColor = dominantColor.copy(alpha = 0.6f),
                                    ambientColor = Color.Black.copy(alpha = 0.5f),
                                ).clip(RoundedCornerShape(16.dp))
                                .artworkSwipe(
                                    enabled = artworkSwipeEnabled,
                                    sensitivity = Settings.shared.swipeSensitivity,
                                    onPrevious = viewModel::previous,
                                    onNext = viewModel::next,
                                ).artworkSeekGestures(
                                    seekEnabled = !isListenTogetherGuest,
                                    stepMs = artworkSeekStepMs,
                                    onTap = null,
                                    tapLabel = null,
                                    progressive = Settings.shared.seekExtraSeconds,
                                    onSeek = onArtworkSeek,
                                ),
                    ) {
                        CrossfadeArtwork(
                            currentUrl = currentSong?.displayThumbnail,
                            nextUrl = null,
                            progress = { 1f },
                            contentDescription = stringResource(Res.string.album_art),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            animate = LocalPlayerContentActive.current,
                        )
                        SeekFeedbackOverlay(
                            feedback = seekFeedback,
                            visible = seekFeedbackVisible,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }

                // SYNCED LYRIC PREVIEW: Directly below artwork, smooth 300ms crossfade
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Crossfade(
                        targetState = activeLineText,
                        animationSpec = tween(durationMillis = 300),
                        label = "activeLyricPreviewCrossfade",
                    ) { line ->
                        if (line.isNotBlank()) {
                            Text(
                                text = line,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                color = Color.White.copy(alpha = 0.92f),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                // METADATA ROW: Left-aligned title and artist
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 4.dp),
                ) {
                    Text(
                        text = currentSong?.title ?: stringResource(Res.string.unknown),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, fontSize = 22.sp),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable(enabled = currentSong?.albumId != null) {
                                    currentSong?.albumId?.let(onNavigateToAlbum)
                                },
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentSong?.artists?.joinToString { it.name } ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF94A3B8),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable(enabled = currentSong?.artists?.firstOrNull()?.id != null) {
                                    currentSong
                                        ?.artists
                                        ?.firstOrNull()
                                        ?.id
                                        ?.let(onNavigateToArtist)
                                },
                    )
                }

                // ACTION CHIPS ROW: [ Heart ] [ Lyrics ] [ Save ] [ Download ] [ Info ]
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 1. Heart (Like / Favorite)
                    PlayerActionChip(
                        icon = if (isLiked) MetrolistIcons.Rounded.Favorite else MetrolistIcons.Rounded.FavoriteBorder,
                        iconTint = if (isLiked) Color(0xFFEF4444) else Color.White,
                        label = null,
                        onClick = {
                            hapticManager.toggle(!isLiked)
                            toggleDisplayedLike()
                        },
                    )

                    // 2. Lyrics
                    PlayerActionChip(
                        icon = MetrolistIcons.Outlined.Lyrics,
                        iconTint = Color.White,
                        label = stringResource(Res.string.lyrics),
                        onClick = { viewModel.setLyricsVisible(true) },
                    )

                    // 3. Save to playlist
                    PlayerActionChip(
                        icon = MetrolistIcons.AutoMirrored.Outlined.PlaylistAdd,
                        iconTint = Color.White,
                        label = stringResource(Res.string.save),
                        onClick = { showPlayerMenu = true },
                    )

                    // 4. Download
                    PlayerActionChip(
                        icon = MetrolistIcons.Outlined.Download,
                        iconTint = Color.White,
                        label = stringResource(Res.string.action_download),
                        onClick = { showPlayerMenu = true },
                    )

                    // 5. Details (Info)
                    PlayerActionChip(
                        icon = MetrolistIcons.Outlined.Info,
                        iconTint = Color.White,
                        label = stringResource(Res.string.details),
                        onClick = { showSongDetails = true },
                    )
                }

                // SEEKBAR: Sleek progress slider with elapsed and duration
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                ) {
                    PlayerProgressSlider(
                        viewModel = viewModel,
                        duration = duration,
                        isPlaying = isPlaying,
                        textBackgroundColor = Color.White,
                        canSeek = !isListenTogetherGuest,
                        isCasting = castState.isConnected,
                        songId = currentSong?.id,
                        progressColor = Color.White,
                        isLive = uiState.isLive || playbackResource?.isLive == true,
                    )
                }

                // TRANSPORT CONTROLS
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = {
                            hapticManager.perform(HapticEvent.PRESS)
                            viewModel.toggleShuffle()
                        },
                        modifier = Modifier.size(44.dp),
                    ) {
                        Icon(
                            imageVector = MetrolistIcons.Rounded.Shuffle,
                            contentDescription = stringResource(Res.string.shuffle),
                            tint = if (isShuffleOn) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }

                    IconButton(
                        onClick = {
                            hapticManager.perform(HapticEvent.PRESS)
                            viewModel.previous()
                        },
                        modifier = Modifier.size(52.dp),
                    ) {
                        Icon(
                            imageVector = MetrolistIcons.Rounded.SkipPrevious,
                            contentDescription = stringResource(Res.string.previous),
                            tint = Color.White,
                            modifier = Modifier.size(36.dp),
                        )
                    }

                    Box(
                        modifier =
                            Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .focusRequester(playPauseFocusRequester)
                                .clickable {
                                    hapticManager.perform(HapticEvent.PRESS)
                                    viewModel.togglePlayPause()
                                },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isPlaybackLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = Color.Black,
                                strokeWidth = 3.dp,
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) MetrolistIcons.Rounded.Pause else MetrolistIcons.Rounded.PlayArrow,
                                contentDescription = stringResource(if (isPlaying) Res.string.pause else Res.string.play),
                                tint = Color.Black,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            hapticManager.perform(HapticEvent.PRESS)
                            viewModel.next()
                        },
                        modifier = Modifier.size(52.dp),
                    ) {
                        Icon(
                            imageVector = MetrolistIcons.Rounded.SkipNext,
                            contentDescription = stringResource(Res.string.next),
                            tint = Color.White,
                            modifier = Modifier.size(36.dp),
                        )
                    }

                    IconButton(
                        onClick = {
                            hapticManager.perform(HapticEvent.PRESS)
                            viewModel.toggleRepeat()
                        },
                        modifier = Modifier.size(44.dp),
                    ) {
                        val isRepeatActive = repeatMode != 0
                        Icon(
                            imageVector = if (repeatMode == 1) MetrolistIcons.Rounded.RepeatOne else MetrolistIcons.Rounded.Repeat,
                            contentDescription = stringResource(Res.string.repeat),
                            tint = if (isRepeatActive) MaterialTheme.colorScheme.primary else Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }

                // BOTTOM QUEUE DRAWER PEEK HANDLE (Matching Image 1)
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable { queueStage = QueueSheetStage.HALF }
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { _, dragAmount ->
                                    if (dragAmount < -15) {
                                        queueStage = QueueSheetStage.HALF
                                    }
                                }
                            }
                            .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier =
                                Modifier
                                    .width(36.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.45f)),
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(4.dp).clip(CircleShape).background(Color(0xFF60A5FA)))
                            Box(Modifier.size(4.dp).clip(CircleShape).background(Color(0xFFFBBF24)))
                        }
                    }
                }
            }

            // Fullscreen / Expanded Lyrics View
            AnimatedVisibility(
                visible = uiState.isLyricsVisible,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = oledBlack,
                ) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                                .navigationBarsPadding(),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = { viewModel.setLyricsVisible(false) }) {
                                Icon(
                                    imageVector = MetrolistIcons.Outlined.KeyboardArrowDown,
                                    contentDescription = stringResource(Res.string.collapse),
                                    tint = Color.White,
                                )
                            }
                            Text(
                                text = stringResource(Res.string.lyrics),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = { showLyricsOffsetEditor = !showLyricsOffsetEditor }) {
                                Icon(
                                    imageVector = MetrolistIcons.Outlined.Sync,
                                    contentDescription = stringResource(Res.string.edit_lyrics),
                                    tint = Color.White,
                                )
                            }
                            IconButton(onClick = {
                                val textToShare = uiState.lyrics.joinToString("\n") { it.text }
                                if (textToShare.isNotBlank()) {
                                    shareLyricsAction?.invoke(textToShare)
                                }
                            }) {
                                Icon(
                                    imageVector = MetrolistIcons.Outlined.Share,
                                    contentDescription = stringResource(Res.string.share),
                                    tint = Color.White,
                                )
                            }
                        }

                        if (showLyricsOffsetEditor) {
                            LyricsOffsetEditor(
                                offsetMs = uiState.lyricsOffsetMs,
                                onOffsetChange = { offset -> currentSong?.let { viewModel.setLyricsOffset(it.id, offset) } },
                                onDone = { showLyricsOffsetEditor = false },
                                contentColor = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }

                        LyricsViewWithPosition(
                            viewModel = viewModel,
                            lyrics = uiState.lyrics,
                            songwriters = uiState.lyricsSongwriters,
                            agents = uiState.lyricsAgents,
                            provider = uiState.rawLyricsProvider,
                            timingOffsetMs = uiState.lyricsOffsetMs,
                            onSeek = { targetMs, _ ->
                                if (duration > 0L) {
                                    viewModel.seekTo(targetMs.toFloat() / duration, duration, currentSong?.id)
                                }
                            },
                            isLoading = isPlaybackLoading && playerContentState.isActive,
                            textColor = Color.White,
                            isCasting = castState.isConnected,
                            isBuffering = castState.isBuffering,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        )
                    }
                }
            }

            // Queue Sheet (Matching Image 2 and Image 3)
            if (animatedQueueHeight.value > 10f) {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f * (animatedQueueHeight.value / maxHeight.value).coerceIn(0f, 1f)))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    if (queueStage == QueueSheetStage.FULL) {
                                        queueStage = QueueSheetStage.HALF
                                    } else {
                                        queueStage = QueueSheetStage.COLLAPSED
                                    }
                                },
                            ),
                )

                Surface(
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(animatedQueueHeight),
                    shape = TopRoundedSurfaceShape(24.dp),
                    color = Color(0xFF111111),
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Drag Handle
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                                    .pointerInput(queueStage) {
                                        detectVerticalDragGestures { _, dragAmount ->
                                            if (dragAmount < -15 && queueStage == QueueSheetStage.HALF) {
                                                queueStage = QueueSheetStage.FULL
                                            } else if (dragAmount > 15 && queueStage == QueueSheetStage.FULL) {
                                                queueStage = QueueSheetStage.HALF
                                            } else if (dragAmount > 20 && queueStage == QueueSheetStage.HALF) {
                                                queueStage = QueueSheetStage.COLLAPSED
                                            }
                                        }
                                    },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .width(36.dp)
                                        .height(4.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.45f)),
                            )
                        }

                        // Mini Transport Header (Only when FULL - matching Image 3!)
                        if (queueStage == QueueSheetStage.FULL) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AsyncImage(
                                    url = currentSong?.displayThumbnail,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)),
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentSong?.title ?: "",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = currentSong?.artists?.joinToString { it.name } ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                IconButton(onClick = viewModel::previous) {
                                    Icon(
                                        MetrolistIcons.Rounded.SkipPrevious,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                                IconButton(onClick = viewModel::togglePlayPause) {
                                    Icon(
                                        if (isPlaying) MetrolistIcons.Rounded.Pause else MetrolistIcons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                                IconButton(onClick = viewModel::next) {
                                    Icon(
                                        MetrolistIcons.Rounded.SkipNext,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                            }
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.10f),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }

                        // Header: "Playing from" + "+ Save" (matching Image 2 & 3!)
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(Res.string.now_playing),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF94A3B8),
                                )
                                val queueOrPlaylistTitle =
                                    uiState.queueTitle?.takeIf { it.isNotBlank() }
                                        ?: currentSong?.album
                                        ?: stringResource(Res.string.now_playing)
                                Text(
                                    text = queueOrPlaylistTitle,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Row(
                                modifier =
                                    Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(0x22FFFFFF))
                                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(50))
                                        .clickable { showPlayerMenu = true }
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = MetrolistIcons.AutoMirrored.Outlined.PlaylistAdd,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = stringResource(Res.string.save),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                )
                            }
                        }

                        // Queue Screen content
                        QueueScreen(
                            session = queueSession,
                            viewModel = viewModel,
                            onClose = {
                                if (queueStage == QueueSheetStage.FULL) {
                                    queueStage = QueueSheetStage.HALF
                                } else {
                                    queueStage = QueueSheetStage.COLLAPSED
                                }
                            },
                            onShowSleepTimer = { showSleepTimerDialog = true },
                            onNavigateToAlbum = { albumId ->
                                queueStage = QueueSheetStage.COLLAPSED
                                onNavigateToAlbum(albumId)
                            },
                            onNavigateToArtist = { artistId ->
                                queueStage = QueueSheetStage.COLLAPSED
                                onNavigateToArtist(artistId)
                            },
                            onNavigateToListenTogether = {
                                queueStage = QueueSheetStage.COLLAPSED
                                onNavigateToListenTogether()
                            },
                            modifier = Modifier.fillMaxWidth().weight(1f).then(queueFocusModifier),
                            backHandlerEnabled = isPlayerBackHandlerEnabled && queueStage != QueueSheetStage.COLLAPSED,
                            handleDismissalBack = false,
                        )
                    }
                }
            }

            if (isReconnecting) {
                Text(
                    text = stringResource(Res.string.playback_reconnecting),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(8.dp),
                )
            }

            if (playerContentState.isActive && shouldShowPlayerLoadingScreen(isPlaybackLoading && !isReconnecting, false)) {
                LoadingScreen()
            }

            if (showDeviceConnectSheet) {
                platform.DeviceConnectSheet(
                    onDismiss = { showDeviceConnectSheet = false },
                    onNavigateToIntegrations = {
                        showDeviceConnectSheet = false
                        onNavigateToIntegrations()
                    },
                )
            }

            PopupVisibility(visible = showPlayerMenu) {
                platform.PlayerMenu(
                    viewModel = viewModel,
                    onDismiss = { showPlayerMenu = false },
                    onNavigateToAlbum = onNavigateToAlbum,
                    onNavigateToArtist = onNavigateToArtist,
                    onNavigateToListenTogether = onNavigateToListenTogether,
                    onShowSongDetails = {
                        showPlayerMenu = false
                        showSongDetails = true
                    },
                    onShowSleepTimer = {
                        showPlayerMenu = false
                        showSleepTimerDialog = true
                    },
                    extraActions = emptyList(),
                )
            }

            PopupVisibility(visible = showSongDetails) {
                platform.SongDetailsSheet(viewModel = viewModel, onDismiss = { showSongDetails = false })
            }

            PopupVisibility(visible = showSleepTimerDialog) {
                SleepTimerDialog(
                    state = sleepTimerState,
                    onDismiss = { showSleepTimerDialog = false },
                    onStart = { durationMs, fadeOut, finishCurrentSong ->
                        playerConnection?.startSleepTimer(durationMs, fadeOut, finishCurrentSong)
                    },
                    onStopAfterCurrentSong = { fadeOut -> playerConnection?.stopAfterCurrentSong(fadeOut) },
                    onCancelTimer = { playerConnection?.cancelSleepTimer() },
                )
            }

            PopupVisibility(visible = showLyricsMenu) {
                platform.LyricsMenu(
                    viewModel = viewModel,
                    onDismiss = { showLyricsMenu = false },
                    onShowLyricsOffset = {
                        showLyricsMenu = false
                        showLyricsOffsetEditor = true
                    },
                )
            }
        }
    }
}

enum class QueueSheetStage {
    COLLAPSED,
    HALF,
    FULL,
}

@Composable
private fun PlayerActionChip(
    icon: ImageVector,
    iconTint: Color,
    label: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .clip(RoundedCornerShape(50))
                .background(Color(0x22FFFFFF))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(50))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconTint,
            modifier = Modifier.size(18.dp),
        )
        if (label != null) {
            Text(
                text = label,
                color = Color.White,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            )
        }
    }
}
