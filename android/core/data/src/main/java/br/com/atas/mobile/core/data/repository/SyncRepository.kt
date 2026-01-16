package br.com.atas.mobile.core.data.repository

import br.com.atas.mobile.core.data.model.Meeting
import kotlinx.coroutines.flow.Flow

data class WardMembership(
    val wardId: String,
    val role: String
)

enum class SyncState {
    DISABLED,
    CONNECTED,
    ERROR,
    CONFLICT
}

data class SyncStatus(
    val state: SyncState,
    val message: String? = null
)

interface SyncRepository {
    suspend fun signInAnonymously(): Result<String>
    suspend fun joinWard(wardCode: String, masterPassword: String): Result<WardMembership>
    suspend fun pushAgenda(meeting: Meeting): Result<Unit>
    fun observeStatus(): Flow<SyncStatus>
}
