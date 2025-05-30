package io.github.taalaydev.doodleverse.shared

import io.github.taalaydev.doodleverse.shared.storage.DataStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.Foundation.*

/**
 * iOS implementation of DataStorage using NSUserDefaults
 */
class IOSDataStorage : DataStorage {
    private val userDefaults = NSUserDefaults.standardUserDefaults

    override suspend fun putString(key: String, value: String) = withContext(Dispatchers.Default) {
        userDefaults.setObject(value, key)
    }

    override suspend fun getString(key: String, defaultValue: String): String = withContext(Dispatchers.Default) {
        (userDefaults.stringForKey(key) ?: defaultValue)
    }

    override suspend fun putInt(key: String, value: Int) = withContext(Dispatchers.Default) {
        userDefaults.setInteger(value.toLong(), key)
    }

    override suspend fun getInt(key: String, defaultValue: Int): Int = withContext(Dispatchers.Default) {
        userDefaults.objectForKey(key)?.let { NSNumber.numberWithInt(it as Int).intValue } ?: defaultValue
    }

    override suspend fun putLong(key: String, value: Long) = withContext(Dispatchers.Default) {
        userDefaults.setInteger(value, key)
    }

    override suspend fun getLong(key: String, defaultValue: Long): Long = withContext(Dispatchers.Default) {
        userDefaults.objectForKey(key)?.let { NSNumber.numberWithLong(it as Long).longValue } ?: defaultValue
    }

    override suspend fun putFloat(key: String, value: Float) = withContext(Dispatchers.Default) {
        userDefaults.setFloat(value, key)
    }

    override suspend fun getFloat(key: String, defaultValue: Float): Float = withContext(Dispatchers.Default) {
        userDefaults.objectForKey(key)?.let { NSNumber.numberWithFloat(it as Float).floatValue } ?: defaultValue
    }

    override suspend fun putBoolean(key: String, value: Boolean) = withContext(Dispatchers.Default) {
        userDefaults.setBool(value, key)
    }

    override suspend fun getBoolean(key: String, defaultValue: Boolean): Boolean = withContext(Dispatchers.Default) {
        userDefaults.objectForKey(key)?.let { NSNumber.numberWithBool(it as Boolean).boolValue } ?: defaultValue
    }

    override suspend fun putStringSet(key: String, value: Set<String>) = withContext(Dispatchers.Default) {
        val array = NSMutableArray()
        value.forEach { array.addObject(it) }
        userDefaults.setObject(array, key)
    }

    override suspend fun getStringSet(key: String, defaultValue: Set<String>): Set<String> = withContext(Dispatchers.Default) {
        (userDefaults.arrayForKey(key)?.map { it as String }?.toSet() ?: defaultValue)
    }

    override suspend fun contains(key: String): Boolean = withContext(Dispatchers.Default) {
        userDefaults.objectForKey(key) != null
    }

    override suspend fun remove(key: String) = withContext(Dispatchers.Default) {
        userDefaults.removeObjectForKey(key)
    }

    override suspend fun clear() = withContext(Dispatchers.Default) {
        val dictionary = userDefaults.dictionaryRepresentation()
        dictionary.keys.forEach { key ->
            if (key is String) {
                userDefaults.removeObjectForKey(key)
            }
        }
    }
}