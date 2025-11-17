package br.com.atas.mobile.feature.meetings

import br.com.atas.mobile.core.data.model.Hymn
import br.com.atas.mobile.core.data.model.MeetingDetails

data class MeetingEditorUiState(
    val meetingId: Long? = null,
    val date: String = "",
    val title: String = "",
    val details: MeetingDetails = MeetingDetails(),
    val createdAt: String? = null,
    val hymns: List<Hymn> = emptyList(),
    val isSaving: Boolean = false,
    val errorMessage: String? = null
)
