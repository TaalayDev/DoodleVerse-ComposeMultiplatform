package io.github.taalaydev.doodleverse.data.models

import doodleverse.composeapp.generated.resources.Res
import doodleverse.composeapp.generated.resources.app_name
import doodleverse.composeapp.generated.resources.chibi_dog_lesson
import doodleverse.composeapp.generated.resources.chibi_dog_lesson_1
import doodleverse.composeapp.generated.resources.chibi_dog_lesson_1_description
import doodleverse.composeapp.generated.resources.chibi_dog_lesson_2
import doodleverse.composeapp.generated.resources.chibi_dog_lesson_3
import doodleverse.composeapp.generated.resources.chibi_dog_lesson_4
import doodleverse.composeapp.generated.resources.chibi_dog_lesson_5
import doodleverse.composeapp.generated.resources.chibi_dog_lesson_6
import doodleverse.composeapp.generated.resources.chibi_dog_lesson_7
import doodleverse.composeapp.generated.resources.chibi_dog_lesson_8
import doodleverse.composeapp.generated.resources.chibi_dog_lesson_9
import doodleverse.composeapp.generated.resources.chibi_dog_lesson_10
import doodleverse.composeapp.generated.resources.chibi_dog_lesson_colored
import doodleverse.composeapp.generated.resources.chibi_dog_lesson_description
import doodleverse.composeapp.generated.resources.chibi_dog_lesson_title
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource

enum class LessonDifficulty {
    EASY,
    MEDIUM,
    HARD,
}

enum class LessonCategory {
    DRAWING,
    ANIMATION,
    GAME_DEVELOPMENT,
    UI_DESIGN,
    OTHER
}

data class LessonModel(
    val id: Long,
    val title: StringResource,
    val preview:  DrawableResource,
    val description: StringResource,
    val difficulty: LessonDifficulty,
    val category: LessonCategory,
    val parts: List<LessonPartModel>,
)

data class LessonPartModel(
    val id: Long,
    val image: DrawableResource,
    val description: StringResource,
)