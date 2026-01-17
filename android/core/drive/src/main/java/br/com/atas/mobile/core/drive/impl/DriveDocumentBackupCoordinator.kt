package br.com.atas.mobile.core.drive.impl

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import br.com.atas.mobile.core.drive.api.DriveBackupCoordinator
import br.com.atas.mobile.core.drive.datastore.DriveFolderSettingsDataStore
import br.com.atas.mobile.core.drive.util.BackupZipUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class DriveDocumentBackupCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val folderSettings: DriveFolderSettingsDataStore
) : DriveBackupCoordinator {

    override suspend fun backupNow(): Result<Unit> = runCatching {
        val folder = resolveFolder()
        val tempZip = createBackupZip()
        val backupName = "agenda-backup-${timestamp()}.zip"
        folder.findFile(backupName)?.delete()
        val driveFile = folder.createFile(MIME_ZIP, backupName)
            ?: error("Nao foi possivel criar o arquivo no Google Drive.")
        context.contentResolver.openOutputStream(driveFile.uri, "w")?.use { output ->
            tempZip.inputStream().use { input -> input.copyTo(output) }
        } ?: error("Nao foi possivel escrever o backup no Google Drive.")
        tempZip.delete()
    }

    override suspend fun restoreLatest(): Result<Unit> = runCatching {
        val folder = resolveFolder()
        val latest = folder.listFiles()
            .filter { it.isFile && it.name?.endsWith(".zip", ignoreCase = true) == true }
            .maxByOrNull { it.lastModified() }
            ?: error("Nenhum backup encontrado na pasta do Google Drive.")
        context.contentResolver.openInputStream(latest.uri)?.use { input ->
            restoreFromZip(input)
        } ?: error("Nao foi possivel ler o backup armazenado no Google Drive.")
    }

    private suspend fun resolveFolder(): DocumentFile {
        val settings = folderSettings.settings.first()
        val uri = settings.folderUri?.let { Uri.parse(it) }
            ?: error("Selecione uma pasta do Google Drive antes de sincronizar.")
        return DocumentFile.fromTreeUri(context, uri)
            ?: error("Nao foi possivel acessar a pasta configurada no Google Drive.")
    }

    private fun createBackupZip(): File {
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        val walFile = File(dbFile.parentFile, "$DATABASE_NAME-wal")
        val shmFile = File(dbFile.parentFile, "$DATABASE_NAME-shm")
        val tempFile = File.createTempFile("agenda-backup", ".zip", context.cacheDir)
        ZipOutputStream(FileOutputStream(tempFile)).use { zip ->
            listOf(dbFile, walFile, shmFile).filter { it.exists() }.forEach { file ->
                zip.putNextEntry(ZipEntry(file.name))
                file.inputStream().use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return tempFile
    }

    private fun restoreFromZip(input: InputStream) {
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        val dbDir = dbFile.parentFile ?: context.filesDir
        BackupZipUtils.restoreDatabaseZip(input, dbDir, DATABASE_NAME)
    }

    private fun timestamp(): String =
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss", Locale.getDefault()))

    companion object {
        private const val DATABASE_NAME = "atas.db"
        private const val MIME_ZIP = "application/zip"
    }
}
