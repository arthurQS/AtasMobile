package br.com.atas.mobile.feature.meetings

import br.com.atas.mobile.core.data.repository.SyncState
import br.com.atas.mobile.core.data.repository.SyncStatus

data class MeetingItemUiModel(
    val id: Long,
    val title: String,
    val dayLabel: String,
    val subtitle: String
)

data class MeetingsUiState(
    val isLoading: Boolean = true,
    val meetings: List<MeetingItemUiModel> = emptyList(),
    val errorMessage: String? = null,
    val syncStatus: SyncStatus = SyncStatus(SyncState.DISABLED),
    val isSyncDialogOpen: Boolean = false,
    val wardCode: String = "",
    val masterPassword: String = "",
    val isSyncing: Boolean = false,
    val syncDialogError: String? = null,
    val lastSyncLabel: String? = null,
    val isAutoSyncing: Boolean = false
)
