package br.com.atas.mobile.feature.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import br.com.atas.mobile.core.data.model.BackupReason
import br.com.atas.mobile.core.data.repository.BackupRepository
import br.com.atas.mobile.core.drive.datastore.DriveFolderSettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupRepository: BackupRepository,
    private val driveSettings: DriveFolderSettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState

    init {
        viewModelScope.launch {
            driveSettings.settings.collect { settings ->
                _uiState.update {
                    it.copy(
                        driveFolderName = settings.folderName,
                        driveFolderUri = settings.folderUri,
                        isDriveLinked = settings.folderUri != null
                    )
                }
            }
        }
    }

    fun exportLocal(uri: Uri) {
        viewModelScope.launch {
            runCatching { copyDatabaseTo(uri) }
                .onSuccess { _uiState.update { it.copy(localMessage = "Backup salvo com sucesso.") } }
                .onFailure { throwable ->
                    _uiState.update { it.copy(localMessage = "Falha ao salvar backup: ${throwable.message}") }
                }
        }
    }

    fun importLocal(uri: Uri) {
        viewModelScope.launch {
            runCatching { restoreDatabase(uri) }
                .onSuccess { _uiState.update { it.copy(localMessage = "Backup restaurado com sucesso.") } }
                .onFailure { throwable ->
                    _uiState.update { it.copy(localMessage = "Falha ao restaurar: ${throwable.message}") }
                }
        }
    }

    fun connectDriveFolder(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, flags)
                val folderName = DocumentFile.fromTreeUri(context, uri)?.name ?: "Pasta do Google Drive"
                driveSettings.saveFolder(uri, folderName)
            }.onSuccess {
                _uiState.update { it.copy(driveMessage = "Pasta conectada com sucesso.") }
            }.onFailure { throwable ->
                _uiState.update { it.copy(driveMessage = "Falha ao conectar: ${throwable.message}") }
            }
        }
    }

    fun uploadDriveBackup() {
        viewModelScope.launch {
            if (!_uiState.value.isDriveLinked) {
                _uiState.update { it.copy(driveMessage = "Selecione uma pasta do Google Drive primeiro.") }
                return@launch
            }
            _uiState.update { it.copy(isDriveBusy = true) }
            val result = backupRepository.enqueue(BackupReason.MANUAL)
            _uiState.update {
                it.copy(
                    driveMessage = result.fold(
                        onSuccess = { "Backup salvo no Google Drive." },
                        onFailure = { error -> "Falha ao salvar no Google Drive: ${error.message}" }
                    ),
                    isDriveBusy = false
                )
            }
        }
    }

    fun syncFromDrive() {
        viewModelScope.launch {
            if (!_uiState.value.isDriveLinked) {
                _uiState.update { it.copy(driveMessage = "Selecione uma pasta do Google Drive primeiro.") }
                return@launch
            }
            _uiState.update { it.copy(isDriveBusy = true) }
            val result = backupRepository.restoreLatest()
            _uiState.update {
                it.copy(
                    driveMessage = result.fold(
                        onSuccess = { "Dados sincronizados a partir do Google Drive." },
                        onFailure = { error -> "Falha ao sincronizar: ${error.message}" }
                    ),
                    isDriveBusy = false
                )
            }
        }
    }

    private suspend fun copyDatabaseTo(uri: Uri) = withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        val files = listOf(
            dbFile,
            File(dbFile.parentFile, "$DATABASE_NAME-wal"),
            File(dbFile.parentFile, "$DATABASE_NAME-shm")
        )
        context.contentResolver.openOutputStream(uri, "w")?.use { output ->
            ZipOutputStream(output).use { zip ->
                files.filter { it.exists() }.forEach { file ->
                    zip.putNextEntry(ZipEntry(file.name))
                    file.inputStream().use { input -> input.copyTo(zip) }
                    zip.closeEntry()
                }
            }
        } ?: error("Não foi possível acessar o local selecionado")
    }

    private suspend fun restoreDatabase(uri: Uri) = withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        val dbDir = dbFile.parentFile ?: context.filesDir
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(input).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val outFile = File(dbDir, entry.name)
                    if (!outFile.parentFile.exists()) outFile.parentFile.mkdirs()
                    FileOutputStream(outFile).use { output -> zip.copyTo(output) }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }
        } ?: error("Não foi possível ler o arquivo selecionado")
    }

    companion object {
        private const val DATABASE_NAME = "atas.db"
    }
}
