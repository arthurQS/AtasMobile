package br.com.atas.mobile.feature.backup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class BackupViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupUiState())
    val uiState: StateFlow<BackupUiState> = _uiState

    fun exportLocal(uri: Uri) {
        viewModelScope.launch {
            runCatching { copyDatabaseTo(uri) }
                .onSuccess { _uiState.value = BackupUiState("Backup salvo com sucesso.") }
                .onFailure { throwable ->
                    _uiState.value = BackupUiState("Falha ao salvar backup: ${throwable.message}")
                }
        }
    }

    fun importLocal(uri: Uri) {
        viewModelScope.launch {
            runCatching { restoreDatabase(uri) }
                .onSuccess { _uiState.value = BackupUiState("Backup restaurado com sucesso.") }
                .onFailure { throwable ->
                    _uiState.value = BackupUiState("Falha ao restaurar: ${throwable.message}")
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
