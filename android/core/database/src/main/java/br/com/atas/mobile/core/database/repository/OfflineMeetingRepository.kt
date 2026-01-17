package br.com.atas.mobile.core.database.repository

import br.com.atas.mobile.core.data.model.Meeting
import br.com.atas.mobile.core.data.model.MeetingDetails
import br.com.atas.mobile.core.data.model.MeetingHymns
import br.com.atas.mobile.core.data.model.MeetingRangeFilter
import br.com.atas.mobile.core.data.model.MeetingSummary
import br.com.atas.mobile.core.data.repository.MeetingRepository
import br.com.atas.mobile.core.database.dao.MeetingDao
import br.com.atas.mobile.core.database.model.MeetingEntity
import br.com.atas.mobile.core.database.model.MeetingSummaryProjection
import br.com.atas.mobile.core.database.util.MeetingJsonAdapter
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class OfflineMeetingRepository @Inject constructor(
    private val meetingDao: MeetingDao,
    private val jsonAdapter: MeetingJsonAdapter
) : MeetingRepository {

    override fun streamAll(): Flow<List<MeetingSummary>> =
        meetingDao.observeSummaries().map { rows -> rows.map { it.toDomain() } }

    override fun streamByRange(filter: MeetingRangeFilter): Flow<List<MeetingSummary>> =
        meetingDao.observeSummariesBetween(filter.fromDateInclusive, filter.toDateExclusive)
            .map { rows -> rows.map { it.toDomain() } }

    override suspend fun get(id: Long): Meeting? =
        meetingDao.findById(id)?.toDomain(jsonAdapter::decode)

    override suspend fun upsert(meeting: Meeting): Long {
        val now = Instant.now().toString()
        val entity = MeetingEntity(
            id = meeting.id,
            date = meeting.date,
            title = meeting.title,
            detailsJson = jsonAdapter.encode(meeting.details.ifEmptyDefault(meeting.title)),
            createdAt = meeting.createdAt ?: now,
            updatedAt = now,
            syncVersion = meeting.syncVersion ?: 0L
        )
        val newId = meetingDao.upsert(entity)
        return if (meeting.id != 0L) meeting.id else newId
    }

    override suspend fun delete(id: Long) {
        meetingDao.delete(id)
    }

    private fun MeetingSummaryProjection.toDomain() = MeetingSummary(
        id = id,
        date = date,
        title = title,
        lastUpdatedAt = updatedAt
    )

    private fun MeetingEntity.toDomain(jsonDecoder: (String) -> MeetingDetails): Meeting =
        Meeting(
            id = id,
            date = date,
            title = title,
            details = jsonDecoder(detailsJson),
            createdAt = createdAt,
            updatedAt = updatedAt,
            syncVersion = syncVersion
        )

    private fun MeetingDetails.ifEmptyDefault(title: String): MeetingDetails {
        if (this != MeetingDetails()) return this
        return MeetingDetails(
            tipo = "Sacramental",
            hinos = MeetingHymns(
                abertura = title
            )
        )
    }
}
