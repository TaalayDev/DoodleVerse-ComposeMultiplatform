package io.github.taalaydev.doodleverse.shared

import android.content.Context
import android.content.SharedPreferences
import io.github.taalaydev.doodleverse.shared.storage.DataStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android implementation of DataStorage using SharedPreferences
 */
class AndroidDataStorage(private val context: Context) : DataStorage {
    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    }

    override suspend fun putString(key: String, value: String) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().putString(key, value).apply()
    }

    override suspend fun getString(key: String, defaultValue: String): String = withContext(Dispatchers.IO) {
        sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    override suspend fun putInt(key: String, value: Int) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().putInt(key, value).apply()
    }

    override suspend fun getInt(key: String, defaultValue: Int): Int = withContext(Dispatchers.IO) {
        sharedPreferences.getInt(key, defaultValue)
    }

    override suspend fun putLong(key: String, value: Long) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().putLong(key, value).apply()
    }

    override suspend fun getLong(key: String, defaultValue: Long): Long = withContext(Dispatchers.IO) {
        sharedPreferences.getLong(key, defaultValue)
    }

    override suspend fun putFloat(key: String, value: Float) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().putFloat(key, value).apply()
    }

    override suspend fun getFloat(key: String, defaultValue: Float): Float = withContext(Dispatchers.IO) {
        sharedPreferences.getFloat(key, defaultValue)
    }

    override suspend fun putBoolean(key: String, value: Boolean) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    override suspend fun getBoolean(key: String, defaultValue: Boolean): Boolean = withContext(Dispatchers.IO) {
        sharedPreferences.getBoolean(key, defaultValue)
    }

    override suspend fun putStringSet(key: String, value: Set<String>) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().putStringSet(key, value).apply()
    }

    override suspend fun getStringSet(key: String, defaultValue: Set<String>): Set<String> = withContext(Dispatchers.IO) {
        sharedPreferences.getStringSet(key, defaultValue) ?: defaultValue
    }

    override suspend fun contains(key: String): Boolean = withContext(Dispatchers.IO) {
        sharedPreferences.contains(key)
    }

    override suspend fun remove(key: String) = withContext(Dispatchers.IO) {
        sharedPreferences.edit().remove(key).apply()
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        sharedPreferences.edit().clear().apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "doodleverse_preferences"
    }
}

