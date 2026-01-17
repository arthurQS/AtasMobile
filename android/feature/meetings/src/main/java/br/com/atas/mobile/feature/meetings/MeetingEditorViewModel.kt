package br.com.atas.mobile.feature.meetings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.atas.mobile.core.data.model.Meeting
import br.com.atas.mobile.core.data.model.MeetingDetails
import br.com.atas.mobile.core.data.repository.HymnRepository
import br.com.atas.mobile.core.data.repository.MeetingRepository
import br.com.atas.mobile.core.data.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class MeetingEditorViewModel @Inject constructor(
    private val meetingRepository: MeetingRepository,
    private val hymnRepository: HymnRepository,
    private val syncRepository: SyncRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val meetingId: Long? =
        savedStateHandle.get<Long>("meetingId")?.takeIf { it > 0 }

    private val _uiState = MutableStateFlow(
        MeetingEditorUiState(
            date = LocalDate.now().toString(),
            details = MeetingDetails()
        )
    )
    val uiState: StateFlow<MeetingEditorUiState> = _uiState

    init {
        meetingId?.let { id ->
            viewModelScope.launch {
                meetingRepository.get(id)?.let { meeting ->
                    _uiState.update {
                        it.copy(
                            meetingId = meeting.id,
                            date = meeting.date,
                            title = meeting.title,
                            details = meeting.details,
                            createdAt = meeting.createdAt,
                            syncVersion = meeting.syncVersion
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            syncRepository.observeStatus().collect { status ->
                _uiState.update { it.copy(syncStatus = status) }
            }
        }
        viewModelScope.launch {
            syncRepository.observeMembership().collect { membership ->
                _uiState.update { it.copy(canOverwrite = membership?.role == "admin") }
            }
        }

        viewModelScope.launch {
            hymnRepository.watchAll().collect { list ->
                if (list.isNotEmpty()) {
                    _uiState.update { it.copy(hymns = list) }
                } else {
                    val fallback = hymnRepository.search("")
                    if (fallback.isNotEmpty()) {
                        _uiState.update { it.copy(hymns = fallback) }
                    }
                }
            }
        }
    }

    fun updateDate(newDate: String) {
        _uiState.update { it.copy(date = newDate) }
    }

    fun updateTitle(newTitle: String) {
        _uiState.update { it.copy(title = newTitle) }
    }

    fun updateDetails(details: MeetingDetails) {
        _uiState.update { it.copy(details = details) }
    }

    fun save(onSuccess: (Long) -> Unit) {
        val current = _uiState.value
        val validationError = MeetingInputValidator.validate(current.date, current.title)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val meeting = Meeting(
                id = current.meetingId ?: 0L,
                date = current.date,
                title = current.title.trim(),
                details = current.details,
                createdAt = current.createdAt,
                syncVersion = current.syncVersion
            )
            runCatching {
                meetingRepository.upsert(meeting)
            }
                .onSuccess { id ->
                    syncRepository.pushAgenda(meeting.copy(id = id, syncVersion = current.syncVersion))
                        .onSuccess { version ->
                            meetingRepository.upsert(meeting.copy(id = id, syncVersion = version))
                            _uiState.update { it.copy(syncVersion = version) }
                        }
                    _uiState.update { it.copy(isSaving = false) }
                    onSuccess(id)
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = throwable.message ?: "Erro ao salvar"
                        )
                    }
                }
        }
    }

    fun reloadFromRemote() {
        val id = _uiState.value.meetingId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isReloading = true, errorMessage = null) }
            syncRepository.fetchAgenda(id)
                .onSuccess { remote ->
                    val existing = meetingRepository.get(id)
                    val merged = remote.copy(createdAt = existing?.createdAt)
                    meetingRepository.upsert(merged)
                    _uiState.update {
                        it.copy(
                            date = merged.date,
                            title = merged.title,
                            details = merged.details,
                            createdAt = merged.createdAt,
                            syncVersion = merged.syncVersion,
                            isReloading = false
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isReloading = false,
                            errorMessage = throwable.message ?: "Falha ao recarregar"
                        )
                    }
                }
        }
    }

    fun overwriteRemote() {
        val current = _uiState.value
        val meetingId = current.meetingId ?: return
        if (!current.canOverwrite) return
        viewModelScope.launch {
            _uiState.update { it.copy(isOverwriting = true, errorMessage = null) }
            val meeting = Meeting(
                id = meetingId,
                date = current.date,
                title = current.title.trim(),
                details = current.details,
                createdAt = current.createdAt,
                syncVersion = current.syncVersion
            )
            syncRepository.pushAgendaOverride(meeting)
                .onSuccess { version ->
                    meetingRepository.upsert(meeting.copy(syncVersion = version))
                    _uiState.update { it.copy(syncVersion = version, isOverwriting = false) }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isOverwriting = false,
                            errorMessage = throwable.message ?: "Falha ao sobrescrever"
                        )
                    }
                }
        }
    }
}
