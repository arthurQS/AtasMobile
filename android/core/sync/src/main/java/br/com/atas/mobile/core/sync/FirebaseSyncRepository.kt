package br.com.atas.mobile.core.sync

import br.com.atas.mobile.core.data.model.Meeting
import br.com.atas.mobile.core.data.repository.SyncRepository
import br.com.atas.mobile.core.data.repository.SyncState
import br.com.atas.mobile.core.data.repository.SyncStatus
import br.com.atas.mobile.core.data.repository.WardMembership
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class FirebaseSyncRepository(
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions,
    private val firestore: FirebaseFirestore
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

    override suspend fun pushAgenda(meeting: Meeting): Result<Unit> {
        return runCatching {
            val currentWardId = wardId ?: error("Ward nao vinculada")
            val uid = auth.currentUser?.uid ?: error("Usuario anonimo nao autenticado")
            val agendaId = meeting.id.toString()

            val now = System.currentTimeMillis()
            val payload = mapOf(
                "title" to meeting.title,
                "date" to meeting.date,
                "data" to meeting.details,
                "updatedAt" to now,
                "updatedBy" to uid,
                "version" to now
            )

            firestore
                .collection("wards")
                .document(currentWardId)
                .collection("agendas")
                .document(agendaId)
                .set(payload, SetOptions.merge())
                .await()
        }.onFailure { error ->
            status.value = SyncStatus(SyncState.ERROR, error.message)
        }
    }

    companion object {
        fun createDefault(): FirebaseSyncRepository {
            return FirebaseSyncRepository(
                auth = FirebaseAuth.getInstance(),
                functions = FirebaseFunctions.getInstance(),
                firestore = FirebaseFirestore.getInstance()
            )
        }
    }
}
