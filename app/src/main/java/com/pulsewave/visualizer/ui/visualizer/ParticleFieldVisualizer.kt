package com.pulsewave.visualizer.ui.visualizer

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import com.pulsewave.visualizer.ui.settings.VisualizerSettings
import kotlin.math.roundToInt
import kotlin.random.Random

private class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    val maxLife: Float,
    val hueSeed: Float,
)

@Composable
fun ParticleFieldVisualizer(
    spectrum: FloatArray,
    settings: VisualizerSettings,
    modifier: Modifier = Modifier,
) {
    val particles = remember { mutableListOf<Particle>() }
    val latestSpectrum = rememberUpdatedState(spectrum)
    val latestSettings = rememberUpdatedState(settings)
    val random = remember { Random(System.nanoTime()) }
    var tick by remember { mutableStateOf(0L) }

    LaunchedEffect(Unit) {
        var lastNanos = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                val dt = ((now - lastNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
                lastNanos = now

                val spec = latestSpectrum.value
                val set = latestSettings.value
                val bassRange = spec.take((spec.size * 0.15f).roundToInt().coerceAtLeast(1))
                val trebleRange = spec.takeLast((spec.size * 0.25f).roundToInt().coerceAtLeast(1))
                val bass = bassRange.average().toFloat()
                val treble = trebleRange.average().toFloat()

                val maxParticles = (150 * set.density).roundToInt().coerceIn(30, 500)
                val spawnCount = (bass * 6).roundToInt()
                repeat(spawnCount) {
                    if (particles.size < maxParticles) {
                        val angle = random.nextFloat() * (2 * Math.PI).toFloat()
                        val speed = 0.15f + treble * 0.6f + random.nextFloat() * 0.15f
                        particles += Particle(
                            x = 0.5f,
                            y = 0.5f,
                            vx = kotlin.math.cos(angle) * speed,
                            vy = kotlin.math.sin(angle) * speed,
                            life = 0f,
                            maxLife = 1f + random.nextFloat() * 1.2f,
                            hueSeed = random.nextFloat(),
                        )
                    }
                }

                val iterator = particles.iterator()
                while (iterator.hasNext()) {
                    val p = iterator.next()
                    p.life += dt
                    if (p.life >= p.maxLife) {
                        iterator.remove()
                        continue
                    }
                    p.x += p.vx * dt
                    p.y += p.vy * dt
                }
                tick++
            }
        }
    }

    Canvas(modifier = modifier) {
        // Reading tick here (unused otherwise) ties this draw phase to the
        // physics loop above so Compose redraws every animation frame.
        tick
        for (p in particles) {
            val lifeRatio = (1f - p.life / p.maxLife).coerceIn(0f, 1f)
            val color = themeAccent(settings.colorTheme, phase = p.hueSeed).copy(alpha = lifeRatio)
            val radius = 3f + 9f * lifeRatio
            drawCircle(
                color = color,
                radius = radius,
                center = Offset(p.x * size.width, p.y * size.height),
            )
        }
    }
}
