package br.com.atas.mobile.core.data.repository

import kotlinx.coroutines.flow.Flow

interface SyncSettingsRepository {
    val autoSyncIntervalMs: Flow<Long>
    suspend fun setAutoSyncIntervalMs(value: Long)
}
