package br.com.atas.mobile.core.data.repository

import kotlinx.coroutines.flow.Flow

interface SyncSettingsRepository {
    val autoSyncIntervalMs: Flow<Long>
    val lastSyncAt: Flow<String?>
    suspend fun setAutoSyncIntervalMs(value: Long)
    suspend fun setLastSyncAt(value: String)
}
