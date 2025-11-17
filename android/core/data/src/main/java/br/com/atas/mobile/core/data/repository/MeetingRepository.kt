package br.com.atas.mobile.core.data.repository

import br.com.atas.mobile.core.data.model.Meeting
import br.com.atas.mobile.core.data.model.MeetingRangeFilter
import br.com.atas.mobile.core.data.model.MeetingSummary
import kotlinx.coroutines.flow.Flow

interface MeetingRepository {
    fun streamAll(): Flow<List<MeetingSummary>>
    fun streamByRange(filter: MeetingRangeFilter): Flow<List<MeetingSummary>>
    suspend fun get(id: Long): Meeting?
    suspend fun upsert(meeting: Meeting): Long
    suspend fun delete(id: Long)
}
