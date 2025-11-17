package br.com.atas.mobile.feature.meetings

data class MeetingItemUiModel(
    val id: Long,
    val title: String,
    val dayLabel: String,
    val subtitle: String
)

data class MeetingsUiState(
    val isLoading: Boolean = true,
    val meetings: List<MeetingItemUiModel> = emptyList(),
    val errorMessage: String? = null
)
