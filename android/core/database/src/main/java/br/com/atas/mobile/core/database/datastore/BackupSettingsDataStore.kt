package br.com.atas.mobile.core.database.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import br.com.atas.mobile.core.data.model.BackupReason
import br.com.atas.mobile.core.data.model.BackupState
import br.com.atas.mobile.core.data.model.BackupStatus
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import dagger.hilt.android.qualifiers.ApplicationContext

@Singleton
class BackupSettingsDataStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile("backup_settings") }
    )

    val status: Flow<BackupStatus> = dataStore.data.map { prefs ->
        BackupStatus(
            lastSuccessAt = prefs[KEY_LAST_SUCCESS],
            lastAttemptAt = prefs[KEY_LAST_ATTEMPT],
            state = prefs[KEY_STATE]?.let { BackupState.valueOf(it) } ?: BackupState.IDLE,
            pendingReason = prefs[KEY_PENDING_REASON]?.let { BackupReason.valueOf(it) }
        )
    }

    suspend fun markAttempt(reason: BackupReason) {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_ATTEMPT] = nowIso()
            prefs[KEY_STATE] = BackupState.RUNNING.name
            prefs[KEY_PENDING_REASON] = reason.name
        }
    }

    suspend fun markSuccess() {
        dataStore.edit { prefs ->
            prefs[KEY_LAST_SUCCESS] = nowIso()
            prefs[KEY_STATE] = BackupState.IDLE.name
            prefs.remove(KEY_PENDING_REASON)
        }
    }

    suspend fun markFailure() {
        dataStore.edit { prefs ->
            prefs[KEY_STATE] = BackupState.ERROR.name
        }
    }

    private fun nowIso(): String = Instant.now().toString()

    private companion object {
        val KEY_LAST_SUCCESS = stringPreferencesKey("last_success")
        val KEY_LAST_ATTEMPT = stringPreferencesKey("last_attempt")
        val KEY_STATE = stringPreferencesKey("state")
        val KEY_PENDING_REASON = stringPreferencesKey("pending_reason")
    }
}
