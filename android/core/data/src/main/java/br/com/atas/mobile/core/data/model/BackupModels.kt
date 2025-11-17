package br.com.atas.mobile.core.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupStatus(
    val lastSuccessAt: String? = null,
    val lastAttemptAt: String? = null,
    val state: BackupState = BackupState.IDLE,
    val pendingReason: BackupReason? = null
)

@Serializable
enum class BackupState { IDLE, RUNNING, ERROR }

@Serializable
enum class BackupReason {
    MANUAL,
    SCHEDULED,
    AUTO_CHANGE
}
