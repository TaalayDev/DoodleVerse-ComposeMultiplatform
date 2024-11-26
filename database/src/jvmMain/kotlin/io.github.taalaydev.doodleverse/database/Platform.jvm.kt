package io.github.taalaydev.doodleverse.database

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.taalaydev.doodleverse.data.database.dao.DrawingPathDao
import io.github.taalaydev.doodleverse.data.database.dao.LayerDao
import io.github.taalaydev.doodleverse.data.database.dao.PointDao
import io.github.taalaydev.doodleverse.data.database.dao.ProjectDao
import io.github.taalaydev.doodleverse.shared.ProjectRepository
import kotlinx.coroutines.Dispatchers
import java.io.File

fun getDatabaseBuilder(): RoomDatabase.Builder<DoodleVerseDatabase> {
    val dbFile = File(System.getProperty("java.io.tmpdir"), "database_6.db")
    return Room.databaseBuilder<DoodleVerseDatabase>(
        name = dbFile.absolutePath,
    )
}

fun getRepository(): ProjectRepository {
    val room = getRoomDatabase(getDatabaseBuilder())

    return ProjectRepositoryImpl(
        projectDao = room.projectDao(),
        animationStateDao = room.animationStateDao(),
        frameDao = room.frameDao(),
        layerDao = room.layerDao(),
        drawingPathDao = room.drawingPathDao(),
        pointDao = room.pointDao()
    )
}