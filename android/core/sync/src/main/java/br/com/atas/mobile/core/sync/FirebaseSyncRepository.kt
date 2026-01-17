package br.com.atas.mobile.core.sync

import br.com.atas.mobile.core.data.model.Meeting
import br.com.atas.mobile.core.data.model.MeetingDetails
import br.com.atas.mobile.core.data.repository.SyncConflictException
import br.com.atas.mobile.core.data.repository.SyncRepository
import br.com.atas.mobile.core.data.repository.SyncState
import br.com.atas.mobile.core.data.repository.SyncStatus
import br.com.atas.mobile.core.data.repository.WardMembership
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

class FirebaseSyncRepository(
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions,
    private val firestore: FirebaseFirestore
) : SyncRepository {
    private val status = MutableStateFlow(SyncStatus(SyncState.DISABLED))
    private var wardId: String? = null
    private val json = Json { ignoreUnknownKeys = true }

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
                "data" to meeting.details,
                "status" to STATUS_DRAFT
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

    override suspend fun fetchAgenda(meetingId: Long): Result<Meeting> {
        return runCatching {
            val currentWardId = wardId ?: error("Ward nao vinculada")
            val snapshot = firestore
                .collection("wards")
                .document(currentWardId)
                .collection("agendas")
                .document(meetingId.toString())
                .get()
                .await()
            if (!snapshot.exists()) {
                error("Agenda nao encontrada")
            }
            snapshotToMeeting(snapshot)
        }.onFailure { error ->
            status.value = SyncStatus(SyncState.ERROR, error.message)
        }
    }

    override suspend fun fetchAgendas(): Result<List<Meeting>> {
        return runCatching {
            val currentWardId = wardId ?: error("Ward nao vinculada")
            val snapshot = firestore
                .collection("wards")
                .document(currentWardId)
                .collection("agendas")
                .get()
                .await()
            snapshot.documents.map { doc -> snapshotToMeeting(doc) }
        }.onFailure { error ->
            status.value = SyncStatus(SyncState.ERROR, error.message)
        }
    }

    companion object {
        private const val STATUS_DRAFT = "draft"

        fun createDefault(): FirebaseSyncRepository {
            return FirebaseSyncRepository(
                auth = FirebaseAuth.getInstance(),
                functions = FirebaseFunctions.getInstance(),
                firestore = FirebaseFirestore.getInstance()
            )
        }
    }

    private fun snapshotToMeeting(snapshot: com.google.firebase.firestore.DocumentSnapshot): Meeting {
        val id = snapshot.id.toLongOrNull() ?: 0L
        val title = snapshot.getString("title").orEmpty()
        val date = snapshot.getString("date").orEmpty()
        val version = snapshot.getLong("version") ?: 0L
        val updatedAt = snapshot.getTimestamp("updatedAt")?.toDate()?.toInstant()?.toString()
        val data = snapshot.get("data") as? Map<String, Any?>
            ?: emptyMap<String, Any?>()
        val details = decodeDetails(data)
        return Meeting(
            id = id,
            date = date,
            title = title,
            details = details,
            createdAt = null,
            updatedAt = updatedAt,
            syncVersion = version
        )
    }

    private fun decodeDetails(data: Map<String, Any?>): MeetingDetails {
        val element = mapToJsonElement(data)
        return json.decodeFromJsonElement(MeetingDetails.serializer(), element)
    }

    private fun mapToJsonElement(value: Any?): JsonElement {
        return when (value) {
            null -> JsonNull
            is String -> JsonPrimitive(value)
            is Boolean -> JsonPrimitive(value)
            is Int -> JsonPrimitive(value)
            is Long -> JsonPrimitive(value)
            is Double -> JsonPrimitive(value)
            is Float -> JsonPrimitive(value)
            is Number -> JsonPrimitive(value.toString())
            is Map<*, *> -> {
                val content = value.entries
                    .mapNotNull { (k, v) ->
                        (k as? String)?.let { it to mapToJsonElement(v) }
                    }
                    .toMap()
                JsonObject(content)
            }
            is List<*> -> JsonArray(value.map { mapToJsonElement(it) })
            else -> JsonPrimitive(value.toString())
        }
    }
}
