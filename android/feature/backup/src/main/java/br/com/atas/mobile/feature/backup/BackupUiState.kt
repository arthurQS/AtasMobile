package br.com.atas.mobile.feature.backup

import br.com.atas.mobile.core.data.repository.SyncState
import br.com.atas.mobile.core.data.repository.SyncStatus

data class BackupUiState(
    val localMessage: String? = null,
    val driveMessage: String? = null,
    val driveFolderName: String? = null,
    val driveFolderUri: String? = null,
    val driveDefaultUri: String? = null,
    val isDriveLinked: Boolean = false,
    val isDriveBusy: Boolean = false,
    val syncStatus: SyncStatus = SyncStatus(SyncState.DISABLED)
)
