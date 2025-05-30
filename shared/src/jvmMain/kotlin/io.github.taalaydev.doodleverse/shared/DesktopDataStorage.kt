package io.github.taalaydev.doodleverse.shared

import io.github.taalaydev.doodleverse.shared.storage.DataStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.prefs.Preferences

/**
 * Desktop (JVM) implementation of DataStorage using Java Preferences API
 */
class DesktopDataStorage : DataStorage {
    private val preferences: Preferences by lazy {
        Preferences.userRoot().node(PREFERENCES_PATH)
    }

    override suspend fun putString(key: String, value: String) = withContext(Dispatchers.IO) {
        preferences.put(key, value)
        preferences.flush()
    }

    override suspend fun getString(key: String, defaultValue: String): String = withContext(Dispatchers.IO) {
        preferences.get(key, defaultValue)
    }

    override suspend fun putInt(key: String, value: Int) = withContext(Dispatchers.IO) {
        preferences.putInt(key, value)
        preferences.flush()
    }

    override suspend fun getInt(key: String, defaultValue: Int): Int = withContext(Dispatchers.IO) {
        preferences.getInt(key, defaultValue)
    }

    override suspend fun putLong(key: String, value: Long) = withContext(Dispatchers.IO) {
        preferences.putLong(key, value)
        preferences.flush()
    }

    override suspend fun getLong(key: String, defaultValue: Long): Long = withContext(Dispatchers.IO) {
        preferences.getLong(key, defaultValue)
    }

    override suspend fun putFloat(key: String, value: Float) = withContext(Dispatchers.IO) {
        preferences.putFloat(key, value)
        preferences.flush()
    }

    override suspend fun getFloat(key: String, defaultValue: Float): Float = withContext(Dispatchers.IO) {
        preferences.getFloat(key, defaultValue)
    }

    override suspend fun putBoolean(key: String, value: Boolean) = withContext(Dispatchers.IO) {
        preferences.putBoolean(key, value)
        preferences.flush()
    }

    override suspend fun getBoolean(key: String, defaultValue: Boolean): Boolean = withContext(Dispatchers.IO) {
        preferences.getBoolean(key, defaultValue)
    }

    override suspend fun putStringSet(key: String, value: Set<String>) = withContext(Dispatchers.IO) {
        // Store size first
        preferences.putInt("$key.size", value.size)

        // Store each string with an index
        value.forEachIndexed { index, str ->
            preferences.put("$key.$index", str)
        }

        preferences.flush()
    }

    override suspend fun getStringSet(key: String, defaultValue: Set<String>): Set<String> = withContext(Dispatchers.IO) {
        val size = preferences.getInt("$key.size", -1)
        if (size == -1) return@withContext defaultValue

        val result = mutableSetOf<String>()
        for (i in 0 until size) {
            val value = preferences.get("$key.$i", null) ?: continue
            result.add(value)
        }

        result
    }

    override suspend fun contains(key: String): Boolean = withContext(Dispatchers.IO) {
        preferences.keys().contains(key)
    }

    override suspend fun remove(key: String) = withContext(Dispatchers.IO) {
        // Handle string sets too
        val size = preferences.getInt("$key.size", -1)
        if (size != -1) {
            for (i in 0 until size) {
                preferences.remove("$key.$i")
            }
            preferences.remove("$key.size")
        }

        preferences.remove(key)
        preferences.flush()
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        preferences.clear()
        preferences.flush()
    }

    companion object {
        private const val PREFERENCES_PATH = "io/github/taalaydev/doodleverse"
    }
}