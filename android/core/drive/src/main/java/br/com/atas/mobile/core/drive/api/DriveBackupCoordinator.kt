package br.com.atas.mobile.core.drive.api

interface DriveBackupCoordinator {
    suspend fun backupNow(): Result<Unit>
    suspend fun restoreLatest(): Result<Unit>
}
