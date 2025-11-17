package br.com.atas.mobile.core.data.model

import kotlinx.serialization.Serializable

@Serializable
data class Meeting(
    val id: Long = 0L,
    val date: String,
    val title: String,
    val details: MeetingDetails = MeetingDetails(),
    val createdAt: String? = null,
    val updatedAt: String? = null
)

@Serializable
data class MeetingSummary(
    val id: Long,
    val date: String,
    val title: String,
    val lastUpdatedAt: String
)

@Serializable
data class MeetingRangeFilter(
    val fromDateInclusive: String,
    val toDateExclusive: String
)
