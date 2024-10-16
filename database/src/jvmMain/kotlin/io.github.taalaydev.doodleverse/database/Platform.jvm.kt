package io.github.taalaydev.doodleverse.database

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

fun getDatabaseBuilder(): RoomDatabase.Builder<DoodleVerseDatabase> {
    val dbFile = File(System.getProperty("java.io.tmpdir"), "my_room.db")
    return Room.databaseBuilder<DoodleVerseDatabase>(
        name = dbFile.absolutePath,
    )
}

fun getDatabase(): DoodleVerseDatabase {
    return getDatabaseBuilder().build()
}

fun getRepository(): ProjectRepository {
    val database = getDatabase()
    return ProjectRepository(
        database.projectDao(),
        database.layerDao(),
    )
}