package io.github.taalaydev.doodleverse.database

import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.taalaydev.doodleverse.shared.ProjectRepository
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask
import platform.UIKit.UIDevice

fun getDatabaseBuilder(): RoomDatabase.Builder<DoodleVerseDatabase> {
    val dbFilePath = documentDirectory() + "/my_room.db"
    return Room.databaseBuilder<DoodleVerseDatabase>(
        name = dbFilePath,
    )
}

fun getRepository(): ProjectRepository {
    val room = getDatabaseBuilder()
        .setDriver(BundledSQLiteDriver())
        .fallbackToDestructiveMigrationOnDowngrade(true)
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()

    return ProjectRepositoryImpl(
        projectDao = room.projectDao(),
        frameDao = room.frameDao(),
        layerDao = room.layerDao(),
        drawingPathDao = room.drawingPathDao(),
        pointDao = room.pointDao()
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(documentDirectory?.path)
}