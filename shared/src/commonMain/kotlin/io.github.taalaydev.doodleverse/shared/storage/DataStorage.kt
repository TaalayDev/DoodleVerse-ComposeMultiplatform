package io.github.taalaydev.doodleverse.shared.storage

/**
 * A cross-platform interface for simple key-value storage,
 * similar to Android's SharedPreferences.
 */
interface DataStorage {
    /**
     * Store a string value
     */
    suspend fun putString(key: String, value: String)

    /**
     * Retrieve a string value or return defaultValue if not found
     */
    suspend fun getString(key: String, defaultValue: String = ""): String

    /**
     * Store an integer value
     */
    suspend fun putInt(key: String, value: Int)

    /**
     * Retrieve an integer value or return defaultValue if not found
     */
    suspend fun getInt(key: String, defaultValue: Int = 0): Int

    /**
     * Store a long value
     */
    suspend fun putLong(key: String, value: Long)

    /**
     * Retrieve a long value or return defaultValue if not found
     */
    suspend fun getLong(key: String, defaultValue: Long = 0L): Long

    /**
     * Store a float value
     */
    suspend fun putFloat(key: String, value: Float)

    /**
     * Retrieve a float value or return defaultValue if not found
     */
    suspend fun getFloat(key: String, defaultValue: Float = 0f): Float

    /**
     * Store a boolean value
     */
    suspend fun putBoolean(key: String, value: Boolean)

    /**
     * Retrieve a boolean value or return defaultValue if not found
     */
    suspend fun getBoolean(key: String, defaultValue: Boolean = false): Boolean

    /**
     * Store a set of strings
     */
    suspend fun putStringSet(key: String, value: Set<String>)

    /**
     * Retrieve a set of strings or return defaultValue if not found
     */
    suspend fun getStringSet(key: String, defaultValue: Set<String> = emptySet()): Set<String>

    /**
     * Check if the storage contains a value for the given key
     */
    suspend fun contains(key: String): Boolean

    /**
     * Remove a value for the given key
     */
    suspend fun remove(key: String)

    /**
     * Clear all values from the storage
     */
    suspend fun clear()
}