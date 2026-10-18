package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

private data class ConfettiParticle(
    val initialX: Float,
    val initialY: Float,
    val angle: Double,
    val speed: Float,
    val color: Color,
    val size: Float
)

@Composable
fun CelebrationOverlay(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (message == null) return

    val animationProgress = remember { Animatable(0f) }
    val particles = remember {
        val colors = listOf(
            Color(0xFF10B981), // Emerald
            Color(0xFFF59E0B), // Amber
            Color(0xFF4F46E5), // Indigo
            Color(0xFF0EA5E9), // Cyan
            Color(0xFFEF4444), // Rose
            Color(0xFF8B5CF6)  // Purple
        )
        List(40) {
            ConfettiParticle(
                initialX = 0.5f,
                initialY = 0.4f,
                angle = Random.nextDouble(0.0, Math.PI * 2),
                speed = Random.nextFloat() * 380f + 120f,
                color = colors[Random.nextInt(colors.size)],
                size = Random.nextFloat() * 10f + 6f
            )
        }
    }

    LaunchedEffect(message) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2000, easing = FastOutSlowInEasing)
        )
        delay(600L)
        onDismiss()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onDismiss() }
    ) {
        // Confetti Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val progress = animationProgress.value
            val centerX = size.width * 0.5f
            val centerY = size.height * 0.35f

            particles.forEach { p ->
                val distance = p.speed * progress
                val gravity = 300f * progress * progress
                val x = centerX + (cos(p.angle) * distance).toFloat()
                val y = centerY + (sin(p.angle) * distance).toFloat() + gravity

                val alpha = (1f - progress).coerceIn(0f, 1f)
                drawCircle(
                    color = p.color.copy(alpha = alpha),
                    radius = p.size * (1f - progress * 0.3f),
                    center = Offset(x, y)
                )
            }
        }

        // Celebration Banner
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 8.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp, start = 24.dp, end = 24.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp)
            )
        }
    }
}
