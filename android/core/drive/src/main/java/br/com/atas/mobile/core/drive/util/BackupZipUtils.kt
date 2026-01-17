package br.com.atas.mobile.core.drive.util

import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

object BackupZipUtils {

    fun restoreDatabaseZip(
        input: InputStream,
        targetDir: File,
        databaseName: String
    ) {
        val allowedEntries = setOf(databaseName, "$databaseName-wal", "$databaseName-shm")
        var restoredFiles = 0
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val entryName = entry.name.replace("\\", "/")
                if (!entry.isDirectory) {
                    if (entryName.contains("/")) {
                        throw IllegalArgumentException("Entrada invalida no backup.")
                    }
                    if (entryName in allowedEntries) {
                        val outFile = File(targetDir, entryName)
                        outFile.parentFile?.takeIf { !it.exists() }?.mkdirs()
                        FileOutputStream(outFile).use { output -> zip.copyTo(output) }
                        restoredFiles += 1
                    }
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        if (restoredFiles == 0) {
            throw IllegalArgumentException("Backup invalido: nenhum arquivo do banco encontrado.")
        }
    }
}
