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

class SyncConflictException(message: String) : Exception(message)

data class SyncStatus(
    val state: SyncState,
    val message: String? = null
)

interface SyncRepository {
    suspend fun signInAnonymously(): Result<String>
    suspend fun joinWard(wardCode: String, masterPassword: String): Result<WardMembership>
    suspend fun pushAgenda(meeting: Meeting): Result<Long>
    suspend fun pushAgendaOverride(meeting: Meeting): Result<Long>
    suspend fun fetchAgenda(meetingId: Long): Result<Meeting>
    suspend fun fetchAgendas(): Result<List<Meeting>>
    suspend fun fetchAgendasSince(lastSyncAt: String): Result<List<Meeting>>
    fun observeMembership(): Flow<WardMembership?>
    fun observeStatus(): Flow<SyncStatus>
}
