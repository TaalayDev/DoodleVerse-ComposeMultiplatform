package io.github.taalaydev.doodleverse.database

import android.content.Context
import android.os.Build
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.taalaydev.doodleverse.data.database.dao.DrawingPathDao
import io.github.taalaydev.doodleverse.data.database.dao.LayerDao
import io.github.taalaydev.doodleverse.data.database.dao.PointDao
import io.github.taalaydev.doodleverse.data.database.dao.ProjectDao
import io.github.taalaydev.doodleverse.shared.ProjectRepository
import kotlinx.coroutines.Dispatchers

fun getRepository(ctx: Context): ProjectRepository {
    val appContext = ctx.applicationContext
    val dbFile = appContext.getDatabasePath("my_room.db")
    val room = Room.databaseBuilder<DoodleVerseDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
        .fallbackToDestructiveMigrationOnDowngrade(true)
        .setDriver(BundledSQLiteDriver()) // Very important
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()

    return ProjectRepositoryImpl(
        projectDao = room.projectDao(),
        frameDao = room.frameDao(),
        layerDao = room.layerDao()
    )
}