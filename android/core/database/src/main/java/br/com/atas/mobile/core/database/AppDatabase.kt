package br.com.atas.mobile.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import br.com.atas.mobile.core.database.dao.MeetingDao
import br.com.atas.mobile.core.database.model.MeetingEntity

@Database(
    entities = [MeetingEntity::class],
    version = 3,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun meetingDao(): MeetingDao
}
