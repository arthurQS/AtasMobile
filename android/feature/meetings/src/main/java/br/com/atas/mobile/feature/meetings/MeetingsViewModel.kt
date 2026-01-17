package br.com.atas.mobile.feature.meetings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.atas.mobile.core.data.model.MeetingSummary
import br.com.atas.mobile.core.data.repository.MeetingRepository
import br.com.atas.mobile.core.data.repository.SyncRepository
import br.com.atas.mobile.core.data.repository.SyncSettingsRepository
import br.com.atas.mobile.core.data.repository.SyncState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@HiltViewModel
class MeetingsViewModel @Inject constructor(
    private val meetingRepository: MeetingRepository,
    private val syncRepository: SyncRepository,
    private val syncSettingsRepository: SyncSettingsRepository
) : ViewModel() {

    private val dialogState = MutableStateFlow(SyncDialogState())
    private var autoSyncJob: Job? = null
    private var autoSyncIntervalMs: Long? = null

    val uiState: StateFlow<MeetingsUiState> = combine(
        meetingRepository.streamAll()
            .map { summaries ->
                MeetingsUiState(
                    isLoading = false,
                    meetings = summaries.map { it.toUiModel() }
                )
            }
            .catch { cause -> emit(MeetingsUiState(isLoading = false, errorMessage = cause.message)) },
        syncRepository.observeStatus(),
        dialogState
    ) { meetingState, syncStatus, dialog ->
        meetingState.copy(
            syncStatus = syncStatus,
            isSyncDialogOpen = dialog.isOpen,
            wardCode = dialog.wardCode,
            masterPassword = dialog.masterPassword,
            isSyncing = dialog.isJoining,
            syncDialogError = dialog.errorMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MeetingsUiState()
    )

    init {
        viewModelScope.launch {
            combine(
                syncRepository.observeStatus(),
                syncRepository.observeMembership(),
                syncSettingsRepository.autoSyncIntervalMs
            ) { status, membership, interval ->
                Triple(status, membership, interval)
            }.collect { (status, membership, interval) ->
                val shouldSync = status.state == SyncState.CONNECTED && membership != null
                if (shouldSync && (autoSyncJob == null || autoSyncIntervalMs != interval)) {
                    autoSyncJob?.cancel()
                    autoSyncIntervalMs = interval
                    autoSyncJob = launchAutoSync(interval)
                } else if (!shouldSync) {
                    autoSyncJob?.cancel()
                    autoSyncJob = null
                    autoSyncIntervalMs = null
                }
            }
        }
    }

    fun refresh() {
        // Flow is backed by Room, so it stays in sync automatically.
    }

    fun openSyncDialog() {
        dialogState.value = dialogState.value.copy(isOpen = true, errorMessage = null)
    }

    fun dismissSyncDialog() {
        dialogState.value = dialogState.value.copy(
            isOpen = false,
            masterPassword = "",
            isJoining = false,
            errorMessage = null
        )
    }

    fun onWardCodeChange(value: String) {
        dialogState.value = dialogState.value.copy(wardCode = value, errorMessage = null)
    }

    fun onMasterPasswordChange(value: String) {
        dialogState.value = dialogState.value.copy(masterPassword = value, errorMessage = null)
    }

    fun joinWard() {
        val wardCode = dialogState.value.wardCode.trim()
        val password = dialogState.value.masterPassword
        if (wardCode.isBlank() || password.isBlank()) {
            dialogState.value = dialogState.value.copy(
                errorMessage = "Informe codigo e senha"
            )
            return
        }
        viewModelScope.launch {
            dialogState.value = dialogState.value.copy(isJoining = true, errorMessage = null)
            val signInResult = syncRepository.signInAnonymously()
            if (signInResult.isFailure) {
                dialogState.value = dialogState.value.copy(
                    isJoining = false,
                    errorMessage = signInResult.exceptionOrNull()?.message
                        ?: "Falha ao autenticar"
                )
                return@launch
            }
            val joinResult = syncRepository.joinWard(wardCode, password)
            if (joinResult.isSuccess) {
                syncRepository.fetchAgendas()
                    .onSuccess { meetings ->
                        meetings.forEach { meetingRepository.upsert(it) }
                    }
                dialogState.value = dialogState.value.copy(
                    isOpen = false,
                    masterPassword = "",
                    isJoining = false,
                    errorMessage = null
                )
            } else {
                dialogState.value = dialogState.value.copy(
                    isJoining = false,
                    errorMessage = joinResult.exceptionOrNull()?.message
                        ?: "Falha ao conectar"
                )
            }
        }
    }

    private fun MeetingSummary.toUiModel(): MeetingItemUiModel =
        MeetingItemUiModel(
            id = id,
            title = title.ifBlank { "Agenda sem titulo" },
            dayLabel = date,
            subtitle = "Atualizada em ${lastUpdatedAt.take(10)}"
        )

    private fun launchAutoSync(intervalMs: Long): Job {
        return viewModelScope.launch {
            while (isActive) {
                syncRepository.fetchAgendas()
                    .onSuccess { meetings ->
                        meetings.forEach { meetingRepository.upsert(it) }
                    }
                delay(intervalMs)
            }
        }
    }
}

private data class SyncDialogState(
    val isOpen: Boolean = false,
    val wardCode: String = "",
    val masterPassword: String = "",
    val isJoining: Boolean = false,
    val errorMessage: String? = null
)
