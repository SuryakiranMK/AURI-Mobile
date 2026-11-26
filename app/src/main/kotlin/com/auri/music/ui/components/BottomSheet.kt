package com.auri.music.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

enum class SheetAnchor { DISMISSED, COLLAPSED, EXPANDED }

/**
 * Metrolist-style 3-anchor morphing BottomSheet supporting smooth drag between
 * MiniPlayer (COLLAPSED) and full-screen NowPlaying (EXPANDED).
 */
@Stable
class AuriBottomSheetState(
    private val scope: CoroutineScope,
    initialAnchor: SheetAnchor = SheetAnchor.COLLAPSED
) {
    val progressAnim = Animatable(if (initialAnchor == SheetAnchor.EXPANDED) 1f else 0f)

    val progress: Float
        get() = progressAnim.value.coerceIn(0f, 1f)

    val isExpanded: Boolean
        get() = progress > 0.5f

    fun expand() {
        scope.launch {
            progressAnim.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
            )
        }
    }

    fun collapse() {
        scope.launch {
            progressAnim.animateTo(
                0f,
                spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
            )
        }
    }

    fun snapByDelta(deltaFraction: Float) {
        scope.launch {
            progressAnim.snapTo((progressAnim.value + deltaFraction).coerceIn(0f, 1f))
        }
    }

    fun settle(velocityY: Float) {
        scope.launch {
            val target = when {
                velocityY < -600f -> 1f
                velocityY > 600f -> 0f
                progressAnim.value >= 0.35f -> 1f
                else -> 0f
            }
            progressAnim.animateTo(
                target,
                spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
            )
        }
    }
}

@Composable
fun rememberAuriBottomSheetState(
    initialAnchor: SheetAnchor = SheetAnchor.COLLAPSED
): AuriBottomSheetState {
    val scope = rememberCoroutineScope()
    return remember(scope) { AuriBottomSheetState(scope, initialAnchor) }
}

@Composable
fun MorphingPlayerSheet(
    visible: Boolean,
    state: AuriBottomSheetState,
    collapsedHeight: Dp,
    bottomNavHeight: Dp,
    maxHeight: Dp,
    collapsedContent: @Composable BoxScope.() -> Unit,
    expandedContent: @Composable BoxScope.() -> Unit,
    modifier: Modifier = Modifier
) {
    if (!visible) return

    BackHandler(enabled = state.isExpanded) {
        state.collapse()
    }

    val density = LocalDensity.current
    val maxPx = with(density) { maxHeight.toPx().coerceAtLeast(1f) }
    val collapsedPx = with(density) { collapsedHeight.toPx() }
    val navPx = with(density) { bottomNavHeight.toPx() }
    val travelPx = (maxPx - collapsedPx - navPx).coerceAtLeast(1f)

    val p = state.progress
    val currentHeightPx = collapsedPx + (maxPx - collapsedPx) * p
    val currentBottomOffsetPx = navPx * (1f - p)
    val cornerRadius = (18f * (1f - p)).dp
    var lastVelocity by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(with(density) { currentHeightPx.toDp() })
                .offset { IntOffset(0, -currentBottomOffsetPx.roundToInt()) }
                .clip(RoundedCornerShape(cornerRadius))
                .pointerInput(travelPx) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { _, dragAmount ->
                            lastVelocity = dragAmount * 15f
                            state.snapByDelta(-dragAmount / travelPx)
                        },
                        onDragEnd = {
                            state.settle(lastVelocity)
                            lastVelocity = 0f
                        },
                        onDragCancel = {
                            state.settle(0f)
                            lastVelocity = 0f
                        }
                    )
                }
        ) {
            if (p < 0.99f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(collapsedHeight)
                        .alpha((1f - p * 2f).coerceIn(0f, 1f)),
                    content = collapsedContent
                )
            }
            if (p > 0.01f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(((p - 0.1f) / 0.9f).coerceIn(0f, 1f)),
                    content = expandedContent
                )
            }
        }
    }
}
