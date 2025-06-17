package com.ardakazanci.stepper

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.shadow.DropShadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ardakazanci.stepper.ui.theme.BackgroundLight
import com.ardakazanci.stepper.ui.theme.PurpleIcon
import com.ardakazanci.stepper.ui.theme.PurplePrimary
import com.ardakazanci.stepper.ui.theme.StepperTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StepperTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DraggableStepper()
                }
            }
        }
    }
}

@Composable
fun DraggableStepper() {
    val dragLimit = 150f
    val scope = rememberCoroutineScope()

    var rawOffsetX by remember { mutableFloatStateOf(0f) }
    val offsetX = remember { Animatable(0f) }
    var count by remember { mutableIntStateOf(0) }
    val leftIconAlpha = remember { Animatable(1f) }
    val rightIconAlpha = remember { Animatable(1f) }

    val direction: Direction by remember {
        derivedStateOf {
            when {
                rawOffsetX >= dragLimit -> Direction.Increase
                rawOffsetX <= -dragLimit -> Direction.Decrease
                else -> Direction.None
            }
        }
    }

    val currentDirection by rememberUpdatedState(newValue = direction)

    LaunchedEffect(currentDirection) {
        if (currentDirection != Direction.None) {
            var delayTime = 500L
            while (isActive) {
                count += when (currentDirection) {
                    Direction.Increase -> 1
                    Direction.Decrease -> -1
                    else -> 0
                }
                delay(delayTime)
                delayTime = (delayTime * 0.9f).toLong().coerceAtLeast(30L)
            }
        }
    }

    val scaleX by remember {
        derivedStateOf {
            1f + (offsetX.value / dragLimit) * 0.05f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FadeIcon(
                imageVector = Icons.Default.KeyboardArrowLeft,
                alpha = leftIconAlpha.value
            )

            Spacer(modifier = Modifier.width(24.dp))

            StepperBox(
                count = count,
                offsetX = offsetX.value,
                scaleX = scaleX,
                onDrag = { delta ->
                    val newOffset = (rawOffsetX + delta.x).coerceIn(-dragLimit, dragLimit)
                    rawOffsetX = newOffset

                    scope.launch {
                        offsetX.snapTo(newOffset)
                        if (newOffset > 0) {
                            rightIconAlpha.animateTo(
                                targetValue = 1f - (newOffset / dragLimit).coerceIn(0f, 1f),
                                animationSpec = tween(100)
                            )
                        }
                        if (newOffset < 0) {
                            leftIconAlpha.animateTo(
                                targetValue = 1f - (-newOffset / dragLimit).coerceIn(0f, 1f),
                                animationSpec = tween(100)
                            )
                        }
                        if (newOffset.absoluteValue < dragLimit / 2) {
                            rightIconAlpha.animateTo(1f, tween(150))
                            leftIconAlpha.animateTo(1f, tween(150))
                        }
                    }
                },
                onDragEnd = {
                    rawOffsetX = 0f
                    scope.launch {
                        offsetX.animateTo(
                            0f,
                            spring(
                                dampingRatio = Spring.DampingRatioHighBouncy,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                        leftIconAlpha.animateTo(1f, tween(300))
                        rightIconAlpha.animateTo(1f, tween(300))
                    }
                },
                animatable = offsetX
            )

            Spacer(modifier = Modifier.width(24.dp))

            FadeIcon(
                imageVector = Icons.Default.KeyboardArrowRight,
                alpha = rightIconAlpha.value
            )
        }
    }
}

@Composable
private fun StepperBox(
    count: Int,
    offsetX: Float,
    scaleX: Float,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    animatable: Animatable<Float, AnimationVector1D>
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "pressScale"
    )

    Box(
        modifier = Modifier
            .offset { IntOffset(offsetX.roundToInt(), 0) }
            .graphicsLayer(
                scaleX = scaleX * pressScale,
                scaleY = pressScale
            )
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { interactionSource.tryEmit(PressInteraction.Press(it)) },
                    onDragEnd = {
                        interactionSource.tryEmit(
                            PressInteraction.Release(
                                PressInteraction.Press(
                                    Offset.Zero
                                )
                            )
                        )
                        onDragEnd()
                    },
                    onDragCancel = {
                        interactionSource.tryEmit(
                            PressInteraction.Cancel(
                                PressInteraction.Press(
                                    Offset.Zero
                                )
                            )
                        )
                        onDragEnd()
                    },
                    onDrag = { _, dragAmount -> onDrag(dragAmount) }
                )
            }
            .size(120.dp)
            .dropShadow(
                shape = RoundedCornerShape(32.dp),
                dropShadow = DropShadow(15.dp, PurplePrimary, 0.dp, alpha = 0.5f),
                offset = DpOffset(10.dp, 10.dp)
            )
            .clip(RoundedCornerShape(32.dp))
            .background(PurplePrimary),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = count,
            transitionSpec = {
                slideInHorizontally(
                    animationSpec = tween(250),
                    initialOffsetX = { it / 2 }
                ) + fadeIn(tween(250)) togetherWith
                        slideOutHorizontally(
                            animationSpec = tween(250),
                            targetOffsetX = { -it / 2 }
                        ) + fadeOut(tween(250))
            },
            label = "StepperSlide"
        ) { targetCount ->
            Text(
                text = targetCount.toString(),
                fontSize = 40.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}


@Composable
private fun FadeIcon(
    imageVector: ImageVector,
    alpha: Float
) {
    Icon(
        imageVector = imageVector,
        contentDescription = null,
        tint = PurpleIcon.copy(alpha = alpha),
        modifier = Modifier.size(40.dp)
    )
}

sealed class Direction {
    object None : Direction()
    object Increase : Direction()
    object Decrease : Direction()
}

// For test - helper
fun determineDirection(offsetX: Float, limit: Float): Direction =
    when {
        offsetX >= limit -> Direction.Increase
        offsetX <= -limit -> Direction.Decrease
        else -> Direction.None
    }

fun calculateScale(offsetX: Float, limit: Float): Float =
    1f + (offsetX / limit) * 0.05f

fun calculateGlowAlpha(count: Int, direction: Direction): Float =
    if (direction != Direction.None) 0.3f + (count % 10) * 0.05f else 0f




