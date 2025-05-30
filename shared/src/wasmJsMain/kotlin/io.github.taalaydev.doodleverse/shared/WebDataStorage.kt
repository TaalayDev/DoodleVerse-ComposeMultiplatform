package io.github.taalaydev.doodleverse.shared

import io.github.taalaydev.doodleverse.shared.storage.DataStorage
import kotlinx.browser.localStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Web implementation of DataStorage using Browser's localStorage
 */
class WebDataStorage : DataStorage {

    override suspend fun putString(key: String, value: String) = withContext(Dispatchers.Default) {
        localStorage.setItem(key, value)
    }

    override suspend fun getString(key: String, defaultValue: String): String = withContext(Dispatchers.Default) {
        localStorage.getItem(key) ?: defaultValue
    }

    override suspend fun putInt(key: String, value: Int) = withContext(Dispatchers.Default) {
        localStorage.setItem(key, value.toString())
    }

    override suspend fun getInt(key: String, defaultValue: Int): Int = withContext(Dispatchers.Default) {
        localStorage.getItem(key)?.toIntOrNull() ?: defaultValue
    }

    override suspend fun putLong(key: String, value: Long) = withContext(Dispatchers.Default) {
        localStorage.setItem(key, value.toString())
    }

    override suspend fun getLong(key: String, defaultValue: Long): Long = withContext(Dispatchers.Default) {
        localStorage.getItem(key)?.toLongOrNull() ?: defaultValue
    }

    override suspend fun putFloat(key: String, value: Float) = withContext(Dispatchers.Default) {
        localStorage.setItem(key, value.toString())
    }

    override suspend fun getFloat(key: String, defaultValue: Float): Float = withContext(Dispatchers.Default) {
        localStorage.getItem(key)?.toFloatOrNull() ?: defaultValue
    }

    override suspend fun putBoolean(key: String, value: Boolean) = withContext(Dispatchers.Default) {
        localStorage.setItem(key, value.toString())
    }

    override suspend fun getBoolean(key: String, defaultValue: Boolean): Boolean = withContext(Dispatchers.Default) {
        when (localStorage.getItem(key)?.lowercase()) {
            "true" -> true
            "false" -> false
            else -> defaultValue
        }
    }

    override suspend fun putStringSet(key: String, value: Set<String>) = withContext(Dispatchers.Default) {
        localStorage.setItem(key, value.joinToString(","))
    }

    override suspend fun getStringSet(key: String, defaultValue: Set<String>): Set<String> = withContext(Dispatchers.Default) {
        try {
            localStorage.getItem(key)?.split(",")?.toSet() ?: defaultValue
        } catch (e: Exception) {
            defaultValue
        }
    }

    override suspend fun contains(key: String): Boolean = withContext(Dispatchers.Default) {
        localStorage.getItem(key) != null
    }

    override suspend fun remove(key: String) = withContext(Dispatchers.Default) {
        localStorage.removeItem(key)
    }

    override suspend fun clear() = withContext(Dispatchers.Default) {
        localStorage.clear()
    }

}
