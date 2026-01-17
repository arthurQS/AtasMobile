package br.com.atas.mobile.core.drive.util

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class BackupZipUtilsTest {

    @Test
    fun restoresDatabaseFiles() {
        val zipBytes = zipOf(
            mapOf(
                "atas.db" to "db",
                "atas.db-wal" to "wal",
                "atas.db-shm" to "shm"
            )
        )
        val targetDir = Files.createTempDirectory("backup-restore").toFile()

        BackupZipUtils.restoreDatabaseZip(ByteArrayInputStream(zipBytes), targetDir, "atas.db")

        assertEquals("db", File(targetDir, "atas.db").readText())
        assertEquals("wal", File(targetDir, "atas.db-wal").readText())
        assertEquals("shm", File(targetDir, "atas.db-shm").readText())
    }

    @Test
    fun rejectsNestedEntries() {
        val zipBytes = zipOf(mapOf("../atas.db" to "db"))
        val targetDir = Files.createTempDirectory("backup-invalid").toFile()

        val error = assertFailsWith<IllegalArgumentException> {
            BackupZipUtils.restoreDatabaseZip(ByteArrayInputStream(zipBytes), targetDir, "atas.db")
        }

        assertTrue(error.message?.contains("Entrada invalida") == true)
    }

    @Test
    fun rejectsWhenNoDatabaseFilesFound() {
        val zipBytes = zipOf(mapOf("readme.txt" to "info"))
        val targetDir = Files.createTempDirectory("backup-empty").toFile()

        val error = assertFailsWith<IllegalArgumentException> {
            BackupZipUtils.restoreDatabaseZip(ByteArrayInputStream(zipBytes), targetDir, "atas.db")
        }

        assertTrue(error.message?.contains("Backup invalido") == true)
    }

    private fun zipOf(entries: Map<String, String>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, contents) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(contents.toByteArray())
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }
}
