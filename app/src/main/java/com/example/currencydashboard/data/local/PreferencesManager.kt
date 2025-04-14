package com.example.currencydashboard.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Wrapper for SharedPreferences to encapsulate preferences operations
 * and provide reactive updates through Flow
 */
class PreferencesManager(context: Context) {
    
    companion object {
        private const val PREFERENCES_NAME = "currency_dashboard_prefs"
        private const val KEY_LAST_REFRESH_TIMESTAMP = "last_refresh_timestamp"
    }
    
    private val preferences: SharedPreferences = context.getSharedPreferences(
        PREFERENCES_NAME, Context.MODE_PRIVATE
    )
    
    private val _lastRefreshTimestampFlow = MutableStateFlow(getLastRefreshTimestamp())
    val lastRefreshTimestampFlow: Flow<Long> = _lastRefreshTimestampFlow.asStateFlow()

    fun getLastRefreshTimestamp(): Long {
        return preferences.getLong(KEY_LAST_REFRESH_TIMESTAMP, 0L)
    }

    fun saveLastRefreshTimestamp(timestamp: Long) {
        preferences.edit {
            putLong(KEY_LAST_REFRESH_TIMESTAMP, timestamp)
        }
        _lastRefreshTimestampFlow.value = timestamp
    }

    fun clearAll() {
        preferences.edit { clear() }
        _lastRefreshTimestampFlow.value = 0L
    }
} 