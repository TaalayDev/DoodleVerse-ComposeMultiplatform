package io.github.taalaydev.doodleverse.ui.screens.lesson

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.BookOpen
import com.composables.icons.lucide.Lucide
import io.github.taalaydev.doodleverse.core.lessons
import io.github.taalaydev.doodleverse.data.models.LessonCategory
import io.github.taalaydev.doodleverse.data.models.LessonDifficulty
import io.github.taalaydev.doodleverse.data.models.LessonModel
import io.github.taalaydev.doodleverse.navigation.Destination
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun LessonsScreen(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    var selectedCategory by remember { mutableStateOf<LessonCategory?>(null) }
    var selectedDifficulty by remember { mutableStateOf<LessonDifficulty?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lessons") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Lucide.ArrowLeft, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filters
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Category filter
                FilterChip(
                    selected = selectedCategory != null,
                    onClick = {
                        if (selectedCategory != null) selectedCategory = null
                    },
                    label = { Text(selectedCategory?.name ?: "All Categories") },
                    leadingIcon = {
                        Icon(
                            Lucide.BookOpen,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )

                // Difficulty filter
                FilterChip(
                    selected = selectedDifficulty != null,
                    onClick = {
                        if (selectedDifficulty != null) selectedDifficulty = null
                    },
                    label = { Text(selectedDifficulty?.name ?: "All Difficulties") }
                )
            }

            // Lessons grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(300.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                val filteredLessons = lessons.filter {
                    (selectedCategory == null || it.category == selectedCategory) &&
                            (selectedDifficulty == null || it.difficulty == selectedDifficulty)
                }

                items(filteredLessons) { lesson ->
                    LessonCard(
                        lesson = lesson,
                        onClick = {
                            navController.navigate(Destination.LessonDetail(lesson.id))
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
@Composable
fun LessonCard(
    lesson: LessonModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Preview image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Image(
                    painter = painterResource(lesson.preview),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )

                // Difficulty badge
                Surface(
                    color = when (lesson.difficulty) {
                        LessonDifficulty.EASY -> Color(0xFF4CAF50)
                        LessonDifficulty.MEDIUM -> Color(0xFFFFC107)
                        LessonDifficulty.HARD -> Color(0xFFF44336)
                    },
                    shape = RoundedCornerShape(bottomEnd = 8.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = lesson.difficulty.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                // Category badge
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(bottomStart = 8.dp),
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Text(
                        text = lesson.category.name,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Lesson info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(lesson.title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(lesson.description),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2
                )
            }
        }
    }
}