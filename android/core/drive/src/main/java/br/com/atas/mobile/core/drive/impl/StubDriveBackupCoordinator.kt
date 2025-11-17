package br.com.atas.mobile.core.drive.impl

import br.com.atas.mobile.core.drive.api.DriveBackupCoordinator
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class StubDriveBackupCoordinator @Inject constructor() : DriveBackupCoordinator {
    override suspend fun backupNow(): Result<Unit> {
        Timber.w("Google Drive backup not implemented yet")
        return Result.success(Unit)
    }

    override suspend fun restoreLatest(): Result<Unit> {
        Timber.w("Google Drive restore not implemented yet")
        return Result.success(Unit)
    }
}
