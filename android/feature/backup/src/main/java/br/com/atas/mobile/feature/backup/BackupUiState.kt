package br.com.atas.mobile.feature.backup

data class BackupUiState(
    val localMessage: String? = null,
    val driveMessage: String? = null,
    val driveFolderName: String? = null,
    val driveFolderUri: String? = null,
    val driveDefaultUri: String? = null,
    val isDriveLinked: Boolean = false,
    val isDriveBusy: Boolean = false
)
