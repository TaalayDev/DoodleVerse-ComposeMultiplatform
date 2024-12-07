package io.github.taalaydev.doodleverse.ui.screens.bridge

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

enum class GameStage {
    SHOW_REFERENCE,
    DRAW,
    COMPARISON
}

@Composable
fun BridgeGame(
    referenceImageRes: DrawableResource,
    showReferenceDurationMs: Long = 5000L,
    drawDurationMs: Long = 30000L
) {
    // Game State
    var gameStage by remember { mutableStateOf(GameStage.SHOW_REFERENCE) }
    var pathPoints by remember { mutableStateOf(listOf<Offset>()) }
    var isDrawing by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Manage timers based on the game stage
    LaunchedEffect(gameStage) {
        when (gameStage) {
            GameStage.SHOW_REFERENCE -> {
                // Show reference image for 5 seconds
                delay(showReferenceDurationMs)
                gameStage = GameStage.DRAW
            }
            GameStage.DRAW -> {
                // Allow drawing for 30 seconds
                delay(drawDurationMs)
                gameStage = GameStage.COMPARISON
            }
            GameStage.COMPARISON -> {
                // No automatic transition here, game ends.
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        when (gameStage) {
            GameStage.SHOW_REFERENCE -> {
                // Show the reference image
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Memorize this image!", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Image(
                        painter = painterResource(referenceImageRes),
                        contentDescription = "Reference Image",
                        modifier = Modifier.size(200.dp)
                    )
                }
            }

            GameStage.DRAW -> {
                // Show a canvas for the user to draw
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Draw from memory!", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .size(300.dp)
                            .background(Color(0xFFEEEEEE))
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        pathPoints = listOf(offset)
                                        isDrawing = true
                                    },
                                    onDrag = { change, _ ->
                                        if (isDrawing) {
                                            pathPoints = pathPoints + change.position
                                        }
                                    },
                                    onDragEnd = {
                                        isDrawing = false
                                    },
                                    onDragCancel = {
                                        isDrawing = false
                                    }
                                )
                            }
                    ) {
                        // Draw the user's path
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            if (pathPoints.size > 1) {
                                val path = Path().apply {
                                    moveTo(pathPoints.first().x, pathPoints.first().y)
                                    for (point in pathPoints.drop(1)) {
                                        lineTo(point.x, point.y)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = Color.Black,
                                    style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                )
                            }
                        }
                    }
                }
            }

            GameStage.COMPARISON -> {
                // Show both the reference image and the user’s drawing side-by-side
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Time's up! Compare your drawing with the original.", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Original", style = MaterialTheme.typography.bodyMedium)
                            Image(
                                painter = painterResource(referenceImageRes),
                                contentDescription = "Reference Image",
                                modifier = Modifier.size(200.dp)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Your Drawing", style = MaterialTheme.typography.bodyMedium)
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .background(Color(0xFFEEEEEE))
                            ) {
                                // Draw the user's path here again
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    if (pathPoints.size > 1) {
                                        val path = Path().apply {
                                            moveTo(pathPoints.first().x, pathPoints.first().y)
                                            for (point in pathPoints.drop(1)) {
                                                lineTo(point.x, point.y)
                                            }
                                        }
                                        drawPath(
                                            path = path,
                                            color = Color.Black,
                                            style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
