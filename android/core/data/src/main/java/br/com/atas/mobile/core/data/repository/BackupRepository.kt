package br.com.atas.mobile.core.data.repository

import br.com.atas.mobile.core.data.model.BackupReason
import br.com.atas.mobile.core.data.model.BackupStatus
import kotlinx.coroutines.flow.Flow

interface BackupRepository {
    fun status(): Flow<BackupStatus>
    suspend fun enqueue(reason: BackupReason): Result<Unit>
    suspend fun restoreLatest(): Result<Unit>
}
