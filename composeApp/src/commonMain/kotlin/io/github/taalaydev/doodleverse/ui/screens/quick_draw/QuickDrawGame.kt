package io.github.taalaydev.doodleverse.ui.screens.quick_draw

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.composables.icons.lucide.*
import kotlinx.coroutines.*
import kotlin.random.Random

private const val GAME_TIME = 30 // seconds

data class DrawingPath(
    val path: Path = Path(),
    val color: Color = Color.Black,
    val strokeWidth: Float = 5f
)

data class GameState(
    val isPlaying: Boolean = false,
    val timeLeft: Int = GAME_TIME,
    val score: Int = 0,
    val currentPrompt: String = "",
    val paths: List<DrawingPath> = emptyList()
)

@Composable
fun QuickDrawGame(
    modifier: Modifier = Modifier
) {
    var gameState by remember { mutableStateOf(GameState()) }
    var showGameOver by remember { mutableStateOf(false) }
    var currentPath by remember { mutableStateOf<DrawingPath?>(null) }

    val prompts = remember {
        listOf(
            "house", "cat", "tree", "sun", "flower",
            "car", "bird", "fish", "boat", "dog"
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

    fun startGame() {
        gameState = GameState(
            isPlaying = true,
            timeLeft = GAME_TIME,
            score = 0,
            currentPrompt = prompts[Random.nextInt(prompts.size)]
        )
        showGameOver = false
    }

    fun submitDrawing() {
        // Score based on time left
        val points = (gameState.timeLeft / 2)
        gameState = gameState.copy(
            score = gameState.score + points,
            currentPrompt = prompts[Random.nextInt(prompts.size)],
            paths = emptyList()
        )
    }

    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Quick Draw!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Draw the prompt as quickly as you can",
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Lucide.Trophy, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${gameState.score} pts",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Current prompt
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Draw: ${gameState.currentPrompt}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp),
                    fontWeight = FontWeight.Bold
                )
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
                                    currentPath = DrawingPath().apply {
                                        path.moveTo(offset.x, offset.y)
                                    }
                                },
                                onDrag = { change, dragAmount ->
                                    currentPath?.let { path ->
                                        val newX = change.position.x
                                        val newY = change.position.y
                                        path.path.lineTo(newX, newY)
                                        currentPath = path
                                    }
                                },
                                onDragEnd = {
                                    currentPath?.let { path ->
                                        gameState = gameState.copy(
                                            paths = gameState.paths + path
                                        )
                                        currentPath = null
                                    }
                                }
                            )
                        }
                ) {
                    // Draw existing paths
                    gameState.paths.forEach { path ->
                        drawPath(
                            path = path.path,
                            color = path.color,
                            style = Stroke(
                                width = path.strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    // Draw current path
                    currentPath?.let { path ->
                        drawPath(
                            path = path.path,
                            color = path.color,
                            style = Stroke(
                                width = path.strokeWidth,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
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
                        gameState = gameState.copy(paths = emptyList())
                    }
                ) {
                    Icon(Lucide.Eraser, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear")
                }

                Button(
                    onClick = { submitDrawing() },
                    enabled = gameState.paths.isNotEmpty()
                ) {
                    Icon(Lucide.Send, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Submit & Next")
                }
            }
        }
    }

    // Game over dialog
    if (showGameOver) {
        Dialog(onDismissRequest = { showGameOver = false }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Game Over!",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "You scored ${gameState.score} points!",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { startGame() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Play Again")
                    }
                }
            }
        }
    }
}