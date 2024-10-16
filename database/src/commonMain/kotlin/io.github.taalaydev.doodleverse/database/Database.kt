package io.github.taalaydev.doodleverse.database

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.taalaydev.doodleverse.data.database.dao.DrawingPathDao
import io.github.taalaydev.doodleverse.data.database.dao.LayerDao
import io.github.taalaydev.doodleverse.data.database.dao.PointDao
import io.github.taalaydev.doodleverse.data.database.dao.ProjectDao
import io.github.taalaydev.doodleverse.database.entities.DrawingPathEntity
import io.github.taalaydev.doodleverse.database.entities.LayerEntity
import io.github.taalaydev.doodleverse.database.entities.PointEntity
import io.github.taalaydev.doodleverse.database.entities.ProjectEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [
        ProjectEntity::class
    ],
    version = 1
)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class DoodleVerseDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    abstract fun layerDao(): LayerDao

    abstract fun drawingPathDao(): DrawingPathDao

    abstract fun pointDao(): PointDao
}

// The Room compiler generates the `actual` implementations.
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<DoodleVerseDatabase> {
    override fun initialize(): DoodleVerseDatabase
}

fun getRoomDatabase(
    builder: RoomDatabase.Builder<DoodleVerseDatabase>
): DoodleVerseDatabase {
    return builder
        .fallbackToDestructiveMigrationOnDowngrade(true)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}