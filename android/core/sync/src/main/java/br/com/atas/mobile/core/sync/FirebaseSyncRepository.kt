package br.com.atas.mobile.core.sync

import br.com.atas.mobile.core.data.model.Meeting
import br.com.atas.mobile.core.data.repository.SyncConflictException
import br.com.atas.mobile.core.data.repository.SyncRepository
import br.com.atas.mobile.core.data.repository.SyncState
import br.com.atas.mobile.core.data.repository.SyncStatus
import br.com.atas.mobile.core.data.repository.WardMembership
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class FirebaseSyncRepository(
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions
) : SyncRepository {
    private val status = MutableStateFlow(SyncStatus(SyncState.DISABLED))
    private var wardId: String? = null

    override fun observeStatus(): Flow<SyncStatus> = status.asStateFlow()

    override suspend fun signInAnonymously(): Result<String> {
        return runCatching {
            val current = auth.currentUser
            if (current != null) {
                status.value = SyncStatus(SyncState.CONNECTED)
                return@runCatching current.uid
            }

            val result = auth.signInAnonymously().await()
            val uid = result.user?.uid ?: error("UID nao encontrado")
            status.value = SyncStatus(SyncState.CONNECTED)
            uid
        }.onFailure { error ->
            status.value = SyncStatus(SyncState.ERROR, error.message)
        }
    }

    override suspend fun joinWard(wardCode: String, masterPassword: String): Result<WardMembership> {
        return runCatching {
            val uid = auth.currentUser?.uid ?: error("Usuario anonimo nao autenticado")
            val payload = hashMapOf(
                "wardCode" to wardCode,
                "password" to masterPassword,
                "uid" to uid
            )
            val response = functions
                .getHttpsCallable("joinWard")
                .call(payload)
                .await()
                .data as? Map<*, *> ?: error("Resposta invalida do servidor")

            val serverWardId = response["wardId"] as? String ?: error("wardId ausente")
            val role = response["role"] as? String ?: "editor"
            wardId = serverWardId
            WardMembership(serverWardId, role)
        }.onFailure { error ->
            status.value = SyncStatus(SyncState.ERROR, error.message)
        }
    }

    override suspend fun pushAgenda(meeting: Meeting): Result<Long> {
        return runCatching {
            val currentWardId = wardId ?: error("Ward nao vinculada")
            val uid = auth.currentUser?.uid ?: error("Usuario anonimo nao autenticado")
            val agendaId = meeting.id.toString()
            val expectedVersion = meeting.syncVersion ?: 0L
            val payload = mapOf(
                "title" to meeting.title,
                "date" to meeting.date,
                "data" to meeting.details
            )
            val response = functions
                .getHttpsCallable("updateAgenda")
                .call(
                    mapOf(
                        "wardId" to currentWardId,
                        "agendaId" to agendaId,
                        "payload" to payload,
                        "expectedVersion" to expectedVersion
                    )
                )
                .await()
                .data as? Map<*, *> ?: emptyMap<String, Any>()
            val version = (response["version"] as? Number)?.toLong()
            if (version == null) {
                throw SyncConflictException("Falha ao atualizar versao remota")
            }
            version
        }.onFailure { error ->
            val conflict = error is SyncConflictException ||
                (error is FirebaseFunctionsException &&
                    error.code == FirebaseFunctionsException.Code.FAILED_PRECONDITION)
            status.value = if (conflict) {
                SyncStatus(SyncState.CONFLICT, error.message)
            } else {
                SyncStatus(SyncState.ERROR, error.message)
            }
        }
    }

    companion object {
        fun createDefault(): FirebaseSyncRepository {
            return FirebaseSyncRepository(
                auth = FirebaseAuth.getInstance(),
                functions = FirebaseFunctions.getInstance()
            )
        }
    }
}
