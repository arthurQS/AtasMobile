package br.com.atas.mobile.core.database.datastore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SyncSettingsDataStoreTest {

    @Test
    fun savesIntervalAndLastSync() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dataStore = SyncSettingsDataStore(context)

        dataStore.setAutoSyncIntervalMs(10_000L)
        val interval = dataStore.autoSyncIntervalMs.first()
        assertEquals(30_000L, interval)

        val timestamp = "2026-01-17T02:10:00Z"
        dataStore.setLastSyncAt(timestamp)
        val lastSyncAt = dataStore.lastSyncAt.first()
        assertEquals(timestamp, lastSyncAt)
    }
}
