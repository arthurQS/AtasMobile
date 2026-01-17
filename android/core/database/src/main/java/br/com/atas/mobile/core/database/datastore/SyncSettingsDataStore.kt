package br.com.atas.mobile.core.database.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import br.com.atas.mobile.core.data.repository.SyncSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class SyncSettingsDataStore @Inject constructor(
    @ApplicationContext context: Context
) : SyncSettingsRepository {
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("sync_settings") }
    )

    override val autoSyncIntervalMs: Flow<Long> = dataStore.data.map { prefs ->
        prefs[KEY_AUTO_SYNC_INTERVAL] ?: DEFAULT_INTERVAL_MS
    }

    override suspend fun setAutoSyncIntervalMs(value: Long) {
        dataStore.edit { prefs ->
            prefs[KEY_AUTO_SYNC_INTERVAL] = value.coerceAtLeast(MIN_INTERVAL_MS)
        }
    }

    private companion object {
        val KEY_AUTO_SYNC_INTERVAL = longPreferencesKey("auto_sync_interval_ms")
        const val DEFAULT_INTERVAL_MS = 120_000L
        const val MIN_INTERVAL_MS = 30_000L
    }
}
