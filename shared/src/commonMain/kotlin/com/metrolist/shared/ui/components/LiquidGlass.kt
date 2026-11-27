package com.metrolist.shared.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastFirstOrNull
import androidx.compose.ui.util.lerp
import com.metrolist.shared.settings.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * State holder for liquid-glass interactive press animation and radial glow.
 */
class LiquidGlassInteraction(
    private val animationScope: CoroutineScope,
) {
    private val pressSpec = spring<Float>(dampingRatio = 0.5f, stiffness = 300f, visibilityThreshold = 0.001f)
    private val pressAnimation = Animatable(0f, 0.001f)

    /** 0f at rest, animating up to 1f during touch. */
    val pressProgress: Float get() = pressAnimation.value

    /** Touch coordinate relative to the surface bounds. */
    var touchPosition by mutableStateOf(Offset.Zero)
        private set

    suspend fun detectPress(pointer: PointerInputScope) =
        with(pointer) {
            inspectLiquidGlassGestures(
                onDragStart = { down ->
                    touchPosition = down.position
                    animationScope.launch { pressAnimation.animateTo(1f, pressSpec) }
                },
                onDragEnd = {
                    animationScope.launch { pressAnimation.animateTo(0f, pressSpec) }
                },
                onDragCancel = {
                    animationScope.launch { pressAnimation.animateTo(0f, pressSpec) }
                },
            ) { change, _ ->
                touchPosition = change.position
            }
        }
}

@Composable
fun rememberLiquidGlassInteraction(): LiquidGlassInteraction {
    val scope = rememberCoroutineScope()
    return remember(scope) { LiquidGlassInteraction(scope) }
}

/**
 * Observe-only pointer recognizer that allows wrapped clicks to function uninterrupted
 * while tracking press state and touch position for the liquid glow effect.
 */
private suspend fun PointerInputScope.inspectLiquidGlassGestures(
    onDragStart: (down: PointerInputChange) -> Unit = {},
    onDragEnd: (change: PointerInputChange) -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        val down = awaitFirstDown(requireUnconsumed = false)
        onDragStart(down)
        onDrag(down, Offset.Zero)
        val upEvent =
            drag(
                pointerId = down.id,
                onDrag = { onDrag(it, it.positionChange()) },
            )
        if (upEvent == null) {
            onDragCancel()
        } else {
            onDragEnd(upEvent)
        }
    }
}

private suspend inline fun AwaitPointerEventScope.drag(
    pointerId: PointerId,
    onDrag: (PointerInputChange) -> Unit,
): PointerInputChange? {
    val isPointerUp = currentEvent.changes.fastFirstOrNull { it.id == pointerId }?.pressed != true
    if (isPointerUp) {
        return null
    }
    var pointer = pointerId
    while (true) {
        val change = awaitDragOrUp(pointer) ?: return null
        if (change.changedToUpIgnoreConsumed()) {
            return change
        }
        onDrag(change)
        pointer = change.id
    }
}

private suspend inline fun AwaitPointerEventScope.awaitDragOrUp(pointerId: PointerId): PointerInputChange? {
    var pointer = pointerId
    while (true) {
        val event = awaitPointerEvent()
        val dragEvent = event.changes.fastFirstOrNull { it.id == pointer } ?: return null
        if (dragEvent.changedToUpIgnoreConsumed()) {
            val otherDown = event.changes.fastFirstOrNull { it.pressed }
            if (otherDown == null) {
                return dragEvent
            } else {
                pointer = otherDown.id
            }
        } else {
            val hasDragged = dragEvent.previousPosition != dragEvent.position
            if (hasDragged) {
                return dragEvent
            }
        }
    }
}

/**
 * Applies the Liquid Glass effect to any surface:
 * - When enabled: applies translucent frosted obsidian background, subtle specular violet border
 *   (Color(0x338B5CF6)), and interactive touch response (subtle scale and radial press glow).
 * - When disabled: cleanly falls back to solid/frosted obsidian surface (Color(0xFF0E0B1F) or
 *   surfaceContainerHighest).
 */
@Composable
fun Modifier.liquidGlass(
    shape: Shape,
    interactive: Boolean = true,
    pressedScale: Float = 1.04f,
    specularBorderColor: Color = Color(0x338B5CF6),
    borderWidth: Dp = 1.dp,
    surfaceColor: Color = Color(0xCC0E0B1F),
    fallbackColor: Color = Color(0xFF0E0B1F),
): Modifier {
    val enabled = Settings.shared.enableLiquidGlass
    if (!enabled) {
        return this
            .clip(shape)
            .background(fallbackColor)
    }

    val interaction = if (interactive) rememberLiquidGlassInteraction() else null

    return this
        .graphicsLayer {
            if (interaction != null && interaction.pressProgress > 0f) {
                val scale = lerp(1f, pressedScale, interaction.pressProgress)
                scaleX = scale
                scaleY = scale
            }
        }.clip(shape)
        .background(surfaceColor)
        .border(
            width = borderWidth,
            brush =
                Brush.verticalGradient(
                    colors =
                        listOf(
                            specularBorderColor.copy(alpha = (specularBorderColor.alpha * 1.4f).coerceAtMost(1f)),
                            specularBorderColor,
                            specularBorderColor.copy(alpha = (specularBorderColor.alpha * 0.6f)),
                        ),
                ),
            shape = shape,
        ).drawWithContent {
            drawContent()
            val press = interaction?.pressProgress ?: 0f
            if (press > 0f) {
                val center =
                    if (interaction?.touchPosition != Offset.Zero) {
                        interaction?.touchPosition ?: Offset(size.width / 2f, size.height / 2f)
                    } else {
                        Offset(size.width / 2f, size.height / 2f)
                    }
                drawRect(
                    brush =
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    Color(0x4D8B5CF6).copy(alpha = 0.35f * press),
                                    Color(0x1A06B6D4).copy(alpha = 0.15f * press),
                                    Color.Transparent,
                                ),
                            center = center,
                            radius = size.minDimension.coerceAtLeast(size.maxDimension * 0.5f) * 1.4f,
                        ),
                    blendMode = BlendMode.Plus,
                )
            }
        }.then(
            if (interaction != null) {
                Modifier.pointerInput(interaction) {
                    interaction.detectPress(this)
                }
            } else {
                Modifier
            },
        )
}

/**
 * Convenience container wrapping arbitrary content with [Modifier.liquidGlass].
 */
@Composable
fun LiquidGlassContainer(
    shape: Shape,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    pressedScale: Float = 1.04f,
    specularBorderColor: Color = Color(0x338B5CF6),
    borderWidth: Dp = 1.dp,
    surfaceColor: Color = Color(0xCC0E0B1F),
    fallbackColor: Color = Color(0xFF0E0B1F),
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier =
            modifier.liquidGlass(
                shape = shape,
                interactive = interactive,
                pressedScale = pressedScale,
                specularBorderColor = specularBorderColor,
                borderWidth = borderWidth,
                surfaceColor = surfaceColor,
                fallbackColor = fallbackColor,
            ),
        contentAlignment = contentAlignment,
        content = content,
    )
}
