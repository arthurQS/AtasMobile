package br.com.atas.mobile.feature.meetings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.atas.mobile.core.data.model.MeetingSummary
import br.com.atas.mobile.core.data.repository.MeetingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MeetingsViewModel @Inject constructor(
    private val meetingRepository: MeetingRepository
) : ViewModel() {

    val uiState: StateFlow<MeetingsUiState> = meetingRepository
        .streamAll()
        .map { summaries ->
            MeetingsUiState(
                isLoading = false,
                meetings = summaries.map { it.toUiModel() }
            )
        }
        .catch { cause -> emit(MeetingsUiState(isLoading = false, errorMessage = cause.message)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MeetingsUiState()
        )

    fun refresh() {
        // Flow is backed by Room, so it stays in sync automatically.
    }

    private fun MeetingSummary.toUiModel(): MeetingItemUiModel =
        MeetingItemUiModel(
            id = id,
            title = title.ifBlank { "Ata sem título" },
            dayLabel = date,
            subtitle = "Atualizada em ${lastUpdatedAt.take(10)}"
        )
}
