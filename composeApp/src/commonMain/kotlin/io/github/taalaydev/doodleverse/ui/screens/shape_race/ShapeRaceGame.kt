package io.github.taalaydev.doodleverse.ui.screens.shape_race

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.*
import kotlinx.coroutines.*
import kotlin.math.*
import kotlin.random.Random

data class GameShape(
    val path: Path,
    val center: Offset,
    val radius: Float,
    val rotation: Float = 0f,
    val points: List<Offset> = emptyList()
)

data class ShapeRaceState(
    val isPlaying: Boolean = false,
    val timeLeft: Int = 60,
    val score: Int = 0,
    val currentLevel: Int = 1,
    val targetShape: GameShape? = null,
    val userPath: Path = Path(),
    val userPoints: List<Offset> = emptyList()
)

@Composable
fun ShapeRaceGame(
    modifier: Modifier = Modifier
) {
    var gameState by remember { mutableStateOf(ShapeRaceState()) }
    var showGameOver by remember { mutableStateOf(false) }
    var isDrawing by remember { mutableStateOf(false) }

    val primary = MaterialTheme.colorScheme.primary

    // Generate a random shape for the level
    fun generateShape(): GameShape {
        val center = Offset(200f, 200f)
        val radius = 100f
        val points = mutableListOf<Offset>()
        val path = Path()

        when (gameState.currentLevel % 3) {
            0 -> { // Triangle
                val angleStep = 2 * PI / 3
                for (i in 0..2) {
                    val angle = i * angleStep
                    val point = Offset(
                        center.x + radius * cos(angle).toFloat(),
                        center.y + radius * sin(angle).toFloat()
                    )
                    points.add(point)
                    if (i == 0) path.moveTo(point.x, point.y)
                    else path.lineTo(point.x, point.y)
                }
                path.close()
            }
            1 -> { // Square
                val sideLength = radius * 1.5f
                val startX = center.x - sideLength / 2
                val startY = center.y - sideLength / 2
                points.addAll(listOf(
                    Offset(startX, startY),
                    Offset(startX + sideLength, startY),
                    Offset(startX + sideLength, startY + sideLength),
                    Offset(startX, startY + sideLength)
                ))
                path.moveTo(points[0].x, points[0].y)
                for (i in 1 until points.size) {
                    path.lineTo(points[i].x, points[i].y)
                }
                path.close()
            }
            2 -> { // Circle
                val steps = 36
                val angleStep = 2 * PI / steps
                for (i in 0 until steps) {
                    val angle = i * angleStep
                    val point = Offset(
                        center.x + radius * cos(angle).toFloat(),
                        center.y + radius * sin(angle).toFloat()
                    )
                    points.add(point)
                    if (i == 0) path.moveTo(point.x, point.y)
                    else path.lineTo(point.x, point.y)
                }
                path.close()
            }
        }

        return GameShape(
            path = path,
            center = center,
            radius = radius,
            points = points,
            rotation = Random.nextFloat() * 360f
        )
    }

    fun startGame() {
        gameState = ShapeRaceState(
            isPlaying = true,
            targetShape = generateShape()
        )
        showGameOver = false
    }

    fun calculateScore(): Int {
        val userPoints = gameState.userPoints
        val targetPoints = gameState.targetShape?.points ?: return 0

        if (userPoints.isEmpty()) return 0

        var totalDistance = 0f
        var minDistance = Float.MAX_VALUE

        userPoints.forEach { userPoint ->
            targetPoints.forEach { targetPoint ->
                val distance = (userPoint - targetPoint).getDistance()
                minDistance = min(minDistance, distance)
            }
            totalDistance += minDistance
            minDistance = Float.MAX_VALUE
        }

        val avgDistance = totalDistance / userPoints.size
        val maxScore = 100
        val score = maxOf(0, (maxScore * (1 - avgDistance / 200f)).toInt())

        return score
    }

    fun submitShape() {
        val shapeScore = calculateScore()
        gameState = gameState.copy(
            score = gameState.score + shapeScore,
            currentLevel = gameState.currentLevel + 1,
            targetShape = generateShape(),
            userPath = Path(),
            userPoints = emptyList()
        )
    }

    // Game timer
    LaunchedEffect(gameState.isPlaying) {
        while (gameState.isPlaying && gameState.timeLeft > 0) {
            delay(1000)
            gameState = gameState.copy(timeLeft = gameState.timeLeft - 1)

            if (gameState.timeLeft == 0) {
                gameState = gameState.copy(isPlaying = false)
                showGameOver = true
            }
        }
    }

    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Shape Race",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Trace the shapes as accurately as you can!",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!gameState.isPlaying && !showGameOver) {
            Button(
                onClick = { startGame() },
                modifier = Modifier
                    .padding(vertical = 32.dp)
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Lucide.Play,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start Game")
            }
        }

        if (gameState.isPlaying) {
            Spacer(modifier = Modifier.height(16.dp))

            // Game stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Lucide.Timer, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${gameState.timeLeft}s",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Level ${gameState.currentLevel}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Lucide.Trophy, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${gameState.score}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Drawing canvas
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    isDrawing = true
                                    gameState = gameState.copy(
                                        userPath = Path().apply {
                                            moveTo(offset.x, offset.y)
                                        },
                                        userPoints = listOf(offset)
                                    )
                                },
                                onDrag = { change, dragAmount ->
                                    val newPoint = change.position
                                    gameState = gameState.copy(
                                        userPath = gameState.userPath.apply {
                                            lineTo(newPoint.x, newPoint.y)
                                        },
                                        userPoints = gameState.userPoints + newPoint
                                    )
                                },
                                onDragEnd = {
                                    isDrawing = false
                                }
                            )
                        }
                ) {
                    // Draw target shape
                    gameState.targetShape?.let { shape ->
                        withTransform({
                            translate(shape.center.x, shape.center.y)
                            rotate(shape.rotation)
                            translate(-shape.center.x, -shape.center.y)
                        }) {
                            drawPath(
                                path = shape.path,
                                color = Color.Gray.copy(alpha = 0.3f),
                                style = Stroke(
                                    width = 8f,
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                            )
                        }
                    }

                    // Draw user's path
                    drawPath(
                        path = gameState.userPath,
                        color = primary,
                        style = Stroke(
                            width = 8f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = {
                        gameState = gameState.copy(
                            userPath = Path(),
                            userPoints = emptyList()
                        )
                    }
                ) {
                    Icon(Lucide.Eraser, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear")
                }

                Button(
                    onClick = { submitShape() },
                    enabled = gameState.userPoints.isNotEmpty()
                ) {
                    Icon(Lucide.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit")
                }
            }
        }
    }

    if (showGameOver) {
        AlertDialog(
            onDismissRequest = { showGameOver = false },
            title = { Text("Game Over!") },
            text = {
                Text(
                    "You completed ${gameState.currentLevel - 1} shapes and scored ${gameState.score} points!"
                )
            },
            confirmButton = {
                Button(onClick = { startGame() }) {
                    Text("Play Again")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGameOver = false }) {
                    Text("Close")
                }
            }
        )
    }
}