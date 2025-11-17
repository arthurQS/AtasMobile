package br.com.atas.mobile.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import br.com.atas.mobile.core.database.model.MeetingEntity
import br.com.atas.mobile.core.database.model.MeetingSummaryProjection
import kotlinx.coroutines.flow.Flow

@Dao
interface MeetingDao {
    @Query("SELECT id, date, title, updated_at AS updatedAt FROM meetings ORDER BY date DESC, id DESC")
    fun observeSummaries(): Flow<List<MeetingSummaryProjection>>

    @Query(
        "SELECT id, date, title, updated_at AS updatedAt FROM meetings WHERE date >= :fromDate AND date < :toDate ORDER BY date DESC, id DESC"
    )
    fun observeSummariesBetween(fromDate: String, toDate: String): Flow<List<MeetingSummaryProjection>>

    @Query("SELECT * FROM meetings WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): MeetingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: MeetingEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<MeetingEntity>)

    @Query("DELETE FROM meetings WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT COUNT(*) FROM meetings")
    suspend fun count(): Int
}
