package com.auri.music.ui.components.sliders

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.auri.music.ui.theme.AuriVioletBright
import com.auri.music.ui.theme.AuriVioletElectric
import kotlin.math.PI
import kotlin.math.sin

/**
 * Metrolist-inspired Expressive wavy progress scrubber with SimpMusic RTL protection.
 * Flattens smoothly when paused or actively scrubbing.
 */
@Composable
fun SquigglyProgress(
    progress: Float,
    isPlaying: Boolean,
    onSeekFraction: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = AuriVioletElectric,
    thumbColor: Color = AuriVioletBright,
    inactiveColor: Color = Color(0x338B5CF6)
) {
    var isDragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(progress) }
    val displayFraction = if (isDragging) dragFraction else progress.coerceIn(0f, 1f)

    val amplitudeFactor by animateFloatAsState(
        targetValue = if (isPlaying && !isDragging) 1f else 0f,
        animationSpec = tween(300),
        label = "wave_amp"
    )

    val transition = rememberInfiniteTransition(label = "wave_phase")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Canvas(
            modifier = modifier
                .fillMaxWidth()
                .height(34.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val frac = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        onSeekFraction(frac)
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            dragFraction = (offset.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            dragFraction = (change.position.x / size.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                        },
                        onDragEnd = {
                            isDragging = false
                            onSeekFraction(dragFraction)
                        },
                        onDragCancel = {
                            isDragging = false
                        }
                    )
                }
        ) {
            val midY = size.height / 2f
            val strokePx = 3.5.dp.toPx()
            val thumbRadius = if (isDragging) 9.dp.toPx() else 7.dp.toPx()
            val activeEndX = size.width * displayFraction

            // Inactive track
            drawLine(
                color = inactiveColor,
                start = Offset(activeEndX, midY),
                end = Offset(size.width, midY),
                strokeWidth = strokePx,
                cap = StrokeCap.Round
            )

            // Active wavy track
            if (activeEndX > 0f) {
                val amp = 4.dp.toPx() * amplitudeFactor
                if (amp < 0.2f) {
                    drawLine(
                        color = activeColor,
                        start = Offset(0f, midY),
                        end = Offset(activeEndX, midY),
                        strokeWidth = strokePx,
                        cap = StrokeCap.Round
                    )
                } else {
                    val path = Path()
                    val wavelength = 28.dp.toPx()
                    val step = 3.dp.toPx()
                    path.moveTo(0f, midY)
                    var x = 0f
                    while (x <= activeEndX) {
                        val y = midY + amp * sin((x / wavelength) * 2f * PI.toFloat() - phase)
                        path.lineTo(x, y)
                        x += step
                    }
                    path.lineTo(activeEndX, midY)
                    drawPath(
                        path = path,
                        color = activeColor,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                }
            }

            // Thumb
            drawCircle(
                color = thumbColor,
                radius = thumbRadius,
                center = Offset(activeEndX.coerceIn(thumbRadius, size.width - thumbRadius), midY)
            )
        }
    }
}
