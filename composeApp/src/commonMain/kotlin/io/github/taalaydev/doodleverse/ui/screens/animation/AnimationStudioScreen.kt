package io.github.taalaydev.doodleverse.features.animation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.composables.icons.lucide.*
import io.github.taalaydev.doodleverse.Platform
import io.github.taalaydev.doodleverse.data.models.AnimationStateModel
import io.github.taalaydev.doodleverse.data.models.FrameModel
import io.github.taalaydev.doodleverse.data.models.ProjectModel
// import io.github.taalaydev.doodleverse.ui.components.ExportSettingsDialog
// import io.github.taalaydev.doodleverse.ui.components.TimelineLayer
import io.github.taalaydev.doodleverse.ui.screens.animation.AnimationViewModel
import io.github.taalaydev.doodleverse.ui.screens.draw.DrawViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/**
 * The main Animation Studio component that provides animation creation and editing capabilities
 */
@Composable
fun AnimationStudioScreen(
    navController: NavHostController = rememberNavController(),
    platform: Platform,
    viewModel: AnimationViewModel = viewModel {
        AnimationViewModel(DrawViewModel(platform.projectRepo, platform.dispatcherIO), 1)
    },
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentProject by viewModel.project.collectAsStateWithLifecycle()
    val animationState by viewModel.currentAnimationState.collectAsStateWithLifecycle()
    val frames by viewModel.frames.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentFrameIndex by viewModel.currentFrameIndex.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()

    var showExportDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AnimationStudioTopBar(
                title = currentProject?.name ?: "Animation",
                onClose = onClose,
                onExport = { showExportDialog = true },
                onSettings = { showSettingsDialog = true }
            )
        },
        bottomBar = {
            AnimationTimeline(
                frames = frames,
                currentFrameIndex = currentFrameIndex,
                isPlaying = isPlaying,
                onFrameSelect = { viewModel.selectFrame(it) },
                onAddFrame = { viewModel.addFrame() },
                onDeleteFrame = { viewModel.deleteFrame(it) },
                onDuplicateFrame = { viewModel.duplicateFrame(it) },
                onPlayPause = { viewModel.togglePlayback() },
                onNextFrame = { viewModel.nextFrame() },
                onPreviousFrame = { viewModel.previousFrame() }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Drawing canvas area for the current frame
            AnimationCanvas(
                frame = frames.getOrNull(currentFrameIndex),
                modifier = Modifier.fillMaxSize()
            )

            // Animation controls overlay
            AnimationControlsOverlay(
                fps = 12,
                onChangeFps = { viewModel.updateFps(it) },
                totalFrames = frames.size,
                currentFrame = currentFrameIndex + 1,
                duration = viewModel.calculateDuration(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            )

            // Onion skinning controls
            OnionSkinningControls(
                enabled = viewModel.onionSkinningEnabled.value,
                onToggle = { viewModel.toggleOnionSkinning() },
                prevFramesCount = viewModel.prevOnionFrames.value,
                nextFramesCount = viewModel.nextOnionFrames.value,
                onPrevCountChange = { viewModel.updatePrevOnionFrames(it) },
                onNextCountChange = { viewModel.updateNextOnionFrames(it) },
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(16.dp)
            )
        }
    }

    if (showExportDialog) {
        ExportAnimationDialog(
            onDismiss = { showExportDialog = false },
            onExport = { format, quality, resolution ->
                viewModel.exportAnimation(format, quality, resolution)
                showExportDialog = false
            }
        )
    }

    if (showSettingsDialog) {
        AnimationSettingsDialog(
            fps = 12,
            loopType = viewModel.loopType.value,
            onFpsChange = { viewModel.updateFps(it) },
            onLoopTypeChange = { viewModel.updateLoopType(it) },
            onDismiss = { showSettingsDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimationStudioTopBar(
    title: String,
    onClose: () -> Unit,
    onExport: () -> Unit,
    onSettings: () -> Unit
) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Lucide.X, contentDescription = "Close Animation Studio")
            }
        },
        actions = {
            IconButton(onClick = onExport) {
                Icon(Lucide.File, contentDescription = "Export Animation")
            }
            IconButton(onClick = onSettings) {
                Icon(Lucide.Settings, contentDescription = "Animation Settings")
            }
        }
    )
}

@Composable
fun AnimationCanvas(
    frame: FrameModel?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(Color.LightGray.copy(alpha = 0.2f))
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (frame != null) {
            // The actual canvas would be connected to the drawing system
            // This is a placeholder for the actual drawing canvas
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .aspectRatio(1f)
                    .fillMaxSize(0.9f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Frame ${frame.name}")
                }
            }
        } else {
            Text("No frames available")
        }
    }
}

@Composable
fun AnimationTimeline(
    frames: List<FrameModel>,
    currentFrameIndex: Int,
    isPlaying: Boolean,
    onFrameSelect: (Int) -> Unit,
    onAddFrame: () -> Unit,
    onDeleteFrame: (Int) -> Unit,
    onDuplicateFrame: (Int) -> Unit,
    onPlayPause: () -> Unit,
    onNextFrame: () -> Unit,
    onPreviousFrame: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Playback controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onPreviousFrame) {
                Icon(Lucide.SkipBack, contentDescription = "Previous Frame")
            }

            IconButton(onClick = onPlayPause) {
                Icon(
                    if (isPlaying) Lucide.Pause else Lucide.Play,
                    contentDescription = if (isPlaying) "Pause" else "Play"
                )
            }

            IconButton(onClick = onNextFrame) {
                Icon(Lucide.SkipForward, contentDescription = "Next Frame")
            }

            Spacer(modifier = Modifier.weight(1f))

            OutlinedButton(
                onClick = onAddFrame,
                contentPadding = PaddingValues(8.dp)
            ) {
                Icon(Lucide.Plus, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Frame")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Timeline frames
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                itemsIndexed(frames) { index, frame ->
                    TimelineFrameItem(
                        frame = frame,
                        isSelected = index == currentFrameIndex,
                        onSelect = { onFrameSelect(index) },
                        onDelete = { onDeleteFrame(index) },
                        onDuplicate = { onDuplicateFrame(index) },
                        modifier = Modifier.height(110.dp)
                    )
                }
            }

            // Empty state
            if (frames.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Add frames to start animating",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
fun TimelineFrameItem(
    frame: FrameModel,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showOptions by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .width(80.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color.White)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onSelect() }
            .padding(4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            // Frame thumbnail
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.LightGray.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                // Placeholder for actual frame content
                Text(
                    (frame.order + 1).toString(),
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Frame label and options
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Frame ${frame.order + 1}",
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1
                )

                IconButton(
                    onClick = { showOptions = !showOptions },
                    modifier = Modifier.size(16.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Frame Options",
                        modifier = Modifier.size(12.dp)
                    )
                }

                DropdownMenu(
                    expanded = showOptions,
                    onDismissRequest = { showOptions = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Duplicate") },
                        onClick = {
                            onDuplicate()
                            showOptions = false
                        },
                        leadingIcon = {
                            Icon(Lucide.Copy, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            onDelete()
                            showOptions = false
                        },
                        leadingIcon = {
                            Icon(Lucide.Trash2, contentDescription = null)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AnimationControlsOverlay(
    fps: Int,
    onChangeFps: (Int) -> Unit,
    totalFrames: Int,
    currentFrame: Int,
    duration: Float, // in seconds
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        modifier = modifier
            .width(180.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                "Animation Info",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("FPS:", style = MaterialTheme.typography.bodySmall)
                Text("$fps", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Frames:", style = MaterialTheme.typography.bodySmall)
                Text("$currentFrame/$totalFrames", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Duration:", style = MaterialTheme.typography.bodySmall)
                //Text("${String.format("%.2f", duration)}s", style = MaterialTheme.typography.bodySmall)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("FPS:", style = MaterialTheme.typography.bodySmall)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { if (fps > 1) onChangeFps(fps - 1) },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            Lucide.Minus,
                            contentDescription = "Decrease FPS",
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    Text(
                        "$fps",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(24.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    IconButton(
                        onClick = { onChangeFps(fps + 1) },
                        modifier = Modifier.size(18.dp)
                    ) {
                        Icon(
                            Lucide.Plus,
                            contentDescription = "Increase FPS",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

enum class OnionSkinMode {
    PREVIOUS, NEXT, BOTH
}

@Composable
fun OnionSkinningControls(
    enabled: Boolean,
    onToggle: () -> Unit,
    prevFramesCount: Int,
    nextFramesCount: Int,
    onPrevCountChange: (Int) -> Unit,
    onNextCountChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        modifier = modifier
            .width(180.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "Onion Skinning",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                Switch(
                    checked = enabled,
                    onCheckedChange = { onToggle() },
                    modifier = Modifier.scale(0.7f)
                )
            }

            if (enabled) {
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Previous:", style = MaterialTheme.typography.bodySmall)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (prevFramesCount > 0) onPrevCountChange(prevFramesCount - 1) },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                Lucide.Minus,
                                contentDescription = "Decrease Previous Frames",
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Text(
                            "$prevFramesCount",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(24.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        IconButton(
                            onClick = { onPrevCountChange(prevFramesCount + 1) },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                Lucide.Plus,
                                contentDescription = "Increase Previous Frames",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Next:", style = MaterialTheme.typography.bodySmall)

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { if (nextFramesCount > 0) onNextCountChange(nextFramesCount - 1) },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                Lucide.Minus,
                                contentDescription = "Decrease Next Frames",
                                modifier = Modifier.size(14.dp)
                            )
                        }

                        Text(
                            "$nextFramesCount",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.width(24.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        IconButton(
                            onClick = { onNextCountChange(nextFramesCount + 1) },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(
                                Lucide.Plus,
                                contentDescription = "Increase Next Frames",
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(Color.Red.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    )
                    Text("Previous", style = MaterialTheme.typography.labelSmall)

                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(Color.Blue.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    )
                    Text("Next", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

enum class AnimationLoopType {
    NONE, LOOP, PING_PONG
}

enum class AnimationExportFormat {
    GIF, MP4, WEBP, PNG_SEQUENCE
}

data class ExportSettings(
    val format: AnimationExportFormat,
    val quality: Int, // 1-100
    val resolution: Float // Scale factor
)

@Composable
fun ExportAnimationDialog(
    onDismiss: () -> Unit,
    onExport: (format: AnimationExportFormat, quality: Int, resolution: Float) -> Unit
) {
    var selectedFormat by remember { mutableStateOf(AnimationExportFormat.GIF) }
    var quality by remember { mutableStateOf(80) }
    var resolution by remember { mutableStateOf(1f) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .width(300.dp)
            ) {
                Text(
                    "Export Animation",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Format", style = MaterialTheme.typography.titleSmall)

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AnimationExportFormat.values().forEach { format ->
                        FilterChip(
                            selected = selectedFormat == format,
                            onClick = { selectedFormat = format },
                            label = { Text(format.name) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Quality: $quality%", style = MaterialTheme.typography.titleSmall)

                Slider(
                    value = quality.toFloat(),
                    onValueChange = { quality = it.toInt() },
                    valueRange = 10f..100f,
                    steps = 9,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Resolution: ${(resolution * 100).toInt()}%", style = MaterialTheme.typography.titleSmall)

                Slider(
                    value = resolution,
                    onValueChange = { resolution = it },
                    valueRange = 0.25f..2f,
                    steps = 7,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(onClick = { onExport(selectedFormat, quality, resolution) }) {
                        Text("Export")
                    }
                }
            }
        }
    }
}

@Composable
fun AnimationSettingsDialog(
    fps: Int,
    loopType: AnimationLoopType,
    onFpsChange: (Int) -> Unit,
    onLoopTypeChange: (AnimationLoopType) -> Unit,
    onDismiss: () -> Unit
) {
    var fpsValue by remember { mutableStateOf(fps) }
    var selectedLoopType by remember { mutableStateOf(loopType) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .width(300.dp)
            ) {
                Text(
                    "Animation Settings",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Frames Per Second (FPS)", style = MaterialTheme.typography.titleSmall)

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Slider(
                        value = fpsValue.toFloat(),
                        onValueChange = { fpsValue = it.toInt() },
                        valueRange = 1f..60f,
                        steps = 11,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        "$fpsValue",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Loop Type", style = MaterialTheme.typography.titleSmall)

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AnimationLoopType.values().forEach { type ->
                        FilterChip(
                            selected = selectedLoopType == type,
                            onClick = { selectedLoopType = type },
                            label = {
                                Text(
                                    when(type) {
                                        AnimationLoopType.NONE -> "No Loop"
                                        AnimationLoopType.LOOP -> "Loop"
                                        AnimationLoopType.PING_PONG -> "Ping Pong"
                                    }
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            onFpsChange(fpsValue)
                            onLoopTypeChange(selectedLoopType)
                            onDismiss()
                        }
                    ) {
                        Text("Apply")
                    }
                }
            }
        }
    }
}
