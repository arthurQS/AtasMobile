package br.com.atas.mobile.core.database.repository

import br.com.atas.mobile.core.data.model.BackupReason
import br.com.atas.mobile.core.data.model.BackupStatus
import br.com.atas.mobile.core.data.repository.BackupRepository
import br.com.atas.mobile.core.database.datastore.BackupSettingsDataStore
import br.com.atas.mobile.core.drive.api.DriveBackupCoordinator
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class DefaultBackupRepository @Inject constructor(
    private val settings: BackupSettingsDataStore,
    private val coordinator: DriveBackupCoordinator
) : BackupRepository {

    override fun status(): Flow<BackupStatus> = settings.status

    override suspend fun enqueue(reason: BackupReason): Result<Unit> {
        settings.markAttempt(reason)
        val result = coordinator.backupNow()
        if (result.isSuccess) {
            settings.markSuccess()
        } else {
            settings.markFailure()
        }
        return result
    }

    override suspend fun restoreLatest(): Result<Unit> {
        settings.markAttempt(BackupReason.MANUAL)
        val result = coordinator.restoreLatest()
        if (result.isSuccess) {
            settings.markSuccess()
        } else {
            settings.markFailure()
        }
        return result
    }
}
