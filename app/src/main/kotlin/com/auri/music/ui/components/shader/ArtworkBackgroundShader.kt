package com.auri.music.ui.components.shader

import android.graphics.Bitmap
import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.auri.music.ui.theme.AuriObsidianBackground
import com.auri.music.ui.theme.AuriVioletContainer
import com.auri.music.ui.theme.AuriVioletElectric
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val AURI_AMBIENT_AGSL = """
uniform float2 iResolution;
uniform float iTime;
uniform half4 uColorPrimary;
uniform half4 uColorSecondary;
uniform half4 uColorBase;

half4 main(float2 fragCoord) {
    float2 uv = fragCoord / max(iResolution, float2(1.0));
    float2 c1 = float2(0.32 + 0.18 * sin(iTime * 0.35), 0.28 + 0.14 * cos(iTime * 0.28));
    float2 c2 = float2(0.70 + 0.16 * cos(iTime * 0.25), 0.56 + 0.18 * sin(iTime * 0.31));
    float d1 = smoothstep(0.95, 0.0, length(uv - c1));
    float d2 = smoothstep(0.90, 0.0, length(uv - c2));
    half4 col = uColorBase;
    col = mix(col, uColorPrimary, d1 * 0.48);
    col = mix(col, uColorSecondary, d2 * 0.34);
    float bottomFade = smoothstep(0.45, 0.95, uv.y);
    return mix(col, uColorBase, bottomFade * 0.85);
}
"""

/**
 * Metrolist-inspired 128px isolated artwork color extraction + ambient AGSL shader
 * (with software radial-gradient fallback below Android 13).
 */
@Composable
fun ArtworkBackgroundShader(
    thumbnailUrl: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var dominantColor by remember { mutableStateOf(AuriVioletElectric) }
    var secondaryColor by remember { mutableStateOf(AuriVioletContainer) }

    LaunchedEffect(thumbnailUrl) {
        if (thumbnailUrl.isBlank()) {
            dominantColor = AuriVioletElectric
            secondaryColor = AuriVioletContainer
            return@LaunchedEffect
        }
        val colors = withContext(Dispatchers.IO) {
            runCatching {
                val req = ImageRequest.Builder(context)
                    .data(thumbnailUrl)
                    .size(128, 128)
                    .allowHardware(false)
                    .build()
                val res = context.imageLoader.execute(req)
                if (res is SuccessResult) {
                    extractTopTwoColors(res.drawable.toBitmap(128, 128))
                } else null
            }.getOrNull()
        }
        if (colors != null) {
            dominantColor = colors.first
            secondaryColor = colors.second
        }
    }

    val transition = rememberInfiniteTransition(label = "shader_time")
    val timeSec by transition.animateFloat(
        initialValue = 0f,
        targetValue = 62.8318f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isPlaying) 45_000 else 120_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shader_phase"
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val shader = remember { RuntimeShader(AURI_AMBIENT_AGSL) }
        Canvas(modifier = modifier.fillMaxSize()) {
            shader.setFloatUniform("iResolution", size.width, size.height)
            shader.setFloatUniform("iTime", timeSec)
            shader.setColorUniform("uColorPrimary", android.graphics.Color.argb(
                255,
                (dominantColor.red * 255).toInt(),
                (dominantColor.green * 255).toInt(),
                (dominantColor.blue * 255).toInt()
            ))
            shader.setColorUniform("uColorSecondary", android.graphics.Color.argb(
                255,
                (secondaryColor.red * 255).toInt(),
                (secondaryColor.green * 255).toInt(),
                (secondaryColor.blue * 255).toInt()
            ))
            shader.setColorUniform("uColorBase", android.graphics.Color.argb(
                255,
                (AuriObsidianBackground.red * 255).toInt(),
                (AuriObsidianBackground.green * 255).toInt(),
                (AuriObsidianBackground.blue * 255).toInt()
            ))
            drawRect(brush = ShaderBrush(shader))
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            dominantColor.copy(alpha = 0.36f),
                            secondaryColor.copy(alpha = 0.25f),
                            AuriObsidianBackground
                        ),
                        center = Offset(540f, 450f),
                        radius = 1200f
                    )
                )
        )
    }
}

private fun extractTopTwoColors(bitmap: Bitmap): Pair<Color, Color> {
    var rSum = 0L
    var gSum = 0L
    var bSum = 0L
    var count = 0L
    val step = 8
    for (y in 0 until bitmap.height step step) {
        for (x in 0 until bitmap.width step step) {
            val px = bitmap.getPixel(x, y)
            val r = (px shr 16) and 0xFF
            val g = (px shr 8) and 0xFF
            val b = px and 0xFF
            if (r + g + b > 45) {
                rSum += r
                gSum += g
                bSum += b
                count++
            }
        }
    }
    if (count == 0L) return AuriVioletElectric to AuriVioletContainer
    val primary = Color(
        red = (rSum / count / 255f).coerceIn(0.15f, 0.9f),
        green = (gSum / count / 255f).coerceIn(0.10f, 0.85f),
        blue = (bSum / count / 255f).coerceIn(0.20f, 0.95f)
    )
    val secondary = Color(
        red = (primary.red * 0.6f).coerceIn(0.08f, 0.6f),
        green = (primary.green * 0.5f).coerceIn(0.05f, 0.5f),
        blue = (primary.blue * 0.8f).coerceIn(0.15f, 0.8f)
    )
    return primary to secondary
}
