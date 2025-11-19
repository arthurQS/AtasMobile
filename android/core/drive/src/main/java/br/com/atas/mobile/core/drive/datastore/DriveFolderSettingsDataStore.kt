package br.com.atas.mobile.core.drive.datastore

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.PreferenceDataStoreFactory

data class DriveFolderSettings(
    val folderUri: String? = null,
    val folderName: String? = null
)

@Singleton
class DriveFolderSettingsDataStore @Inject constructor(
    @ApplicationContext context: Context
) {

    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile(FILE_NAME) }
    )

    val settings: Flow<DriveFolderSettings> = dataStore.data.map { prefs ->
        DriveFolderSettings(
            folderUri = prefs[KEY_FOLDER_URI],
            folderName = prefs[KEY_FOLDER_NAME]
        )
    }

    suspend fun saveFolder(uri: Uri, displayName: String?) {
        dataStore.edit { prefs ->
            prefs[KEY_FOLDER_URI] = uri.toString()
            prefs[KEY_FOLDER_NAME] = displayName ?: "Google Drive"
        }
    }

    suspend fun clearFolder() {
        dataStore.edit { prefs ->
            prefs.remove(KEY_FOLDER_URI)
            prefs.remove(KEY_FOLDER_NAME)
        }
    }

    private companion object {
        private const val FILE_NAME = "drive_folder_settings"
        private val KEY_FOLDER_URI = stringPreferencesKey("folder_uri")
        private val KEY_FOLDER_NAME = stringPreferencesKey("folder_name")
    }
}
