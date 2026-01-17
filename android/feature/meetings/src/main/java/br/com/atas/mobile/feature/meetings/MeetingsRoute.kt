package br.com.atas.mobile.feature.meetings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.SyncDisabled
import androidx.compose.material.icons.filled.SyncProblem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.atas.mobile.core.data.repository.SyncState
import br.com.atas.mobile.core.data.repository.SyncStatus

@Composable
fun MeetingsRoute(
    onOpenBackup: () -> Unit,
    onCreateMeeting: () -> Unit,
    onMeetingSelected: (Long) -> Unit = {},
    viewModel: MeetingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MeetingsScreen(
        state = uiState,
        onOpenBackup = onOpenBackup,
        onCreateMeeting = onCreateMeeting,
        onMeetingSelected = onMeetingSelected,
        onOpenSync = viewModel::openSyncDialog,
        onDismissSync = viewModel::dismissSyncDialog,
        onWardCodeChange = viewModel::onWardCodeChange,
        onMasterPasswordChange = viewModel::onMasterPasswordChange,
        onJoinWard = viewModel::joinWard
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingsScreen(
    state: MeetingsUiState,
    onOpenBackup: () -> Unit,
    onCreateMeeting: () -> Unit,
    onMeetingSelected: (Long) -> Unit,
    onOpenSync: () -> Unit,
    onDismissSync: () -> Unit,
    onWardCodeChange: (String) -> Unit,
    onMasterPasswordChange: (String) -> Unit,
    onJoinWard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(state.syncStatus) {
        if (state.syncStatus.state == SyncState.ERROR || state.syncStatus.state == SyncState.CONFLICT) {
            val message = state.syncStatus.message ?: "Falha ao sincronizar. Verifique os dados."
            snackbarHostState.showSnackbar(message)
        }
    }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Agendas") },
                actions = {
                    IconButton(onClick = onOpenSync) {
                        val icon = when (state.syncStatus.state) {
                            SyncState.DISABLED -> Icons.Default.SyncDisabled
                            SyncState.ERROR -> Icons.Default.SyncProblem
                            SyncState.CONFLICT -> Icons.Default.SyncProblem
                            SyncState.CONNECTED -> Icons.Default.Sync
                        }
                        val label = when (state.syncStatus.state) {
                            SyncState.DISABLED -> "Sync desativado"
                            SyncState.ERROR -> "Sync com erro"
                            SyncState.CONFLICT -> "Sync com conflito"
                            SyncState.CONNECTED -> "Sync conectado"
                        }
                        Icon(imageVector = icon, contentDescription = label)
                    }
                    IconButton(onClick = onOpenBackup) {
                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = "Backup")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateMeeting) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Nova agenda")
            }
        }
    ) { padding ->
        MeetingsList(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            state = state,
            onMeetingSelected = onMeetingSelected
        )
    }

    if (state.isSyncDialogOpen) {
        SyncDialog(
            wardCode = state.wardCode,
            masterPassword = state.masterPassword,
            isSyncing = state.isSyncing,
            errorMessage = state.syncDialogError,
            syncStatus = state.syncStatus,
            onWardCodeChange = onWardCodeChange,
            onMasterPasswordChange = onMasterPasswordChange,
            onDismiss = onDismissSync,
            onConfirm = onJoinWard
        )
    }
}

@Composable
private fun MeetingsList(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues,
    state: MeetingsUiState,
    onMeetingSelected: (Long) -> Unit
) {
    val list = state.meetings
    LazyColumn(
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (state.syncStatus.state != SyncState.DISABLED) {
            item {
                SyncStatusCard(
                    state = state.syncStatus,
                    lastSyncLabel = state.lastSyncLabel,
                    isAutoSyncing = state.isAutoSyncing
                )
            }
        }
        if (state.isLoading) {
            item {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        } else if (list.isEmpty()) {
            item {
                EmptyStateCard()
            }
        }
        items(list, key = { it.id }) { meeting ->
            MeetingCard(
                meeting = meeting,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMeetingSelected(meeting.id) }
            )
        }
    }
}

@Composable
private fun EmptyStateCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Nenhuma agenda cadastrada",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Toque no botao \"+\" para criar a primeira agenda sacramental.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MeetingCard(
    meeting: MeetingItemUiModel,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(
                text = meeting.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = meeting.dayLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = meeting.subtitle,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 6.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SyncStatusCard(state: SyncStatus, lastSyncLabel: String?, isAutoSyncing: Boolean) {
    val message = state.message?.takeIf { it.isNotBlank() }
    val title = when (state.state) {
        SyncState.CONNECTED -> "Sincronizacao conectada"
        SyncState.ERROR -> "Erro de sincronizacao"
        SyncState.CONFLICT -> "Conflito de sincronizacao"
        SyncState.DISABLED -> "Sincronizacao desativada"
    }
    val detail = when {
        state.state == SyncState.CONNECTED && isAutoSyncing -> "Sincronizando..."
        state.state == SyncState.CONNECTED && !lastSyncLabel.isNullOrBlank() ->
            "Ultima sync: $lastSyncLabel"
        else -> null
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (message != null) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            detail?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SyncDialog(
    wardCode: String,
    masterPassword: String,
    isSyncing: Boolean,
    errorMessage: String?,
    syncStatus: SyncStatus,
    onWardCodeChange: (String) -> Unit,
    onMasterPasswordChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Conectar sincronizacao") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = wardCode,
                    onValueChange = onWardCodeChange,
                    label = { Text("Codigo da unidade") },
                    singleLine = true,
                    enabled = !isSyncing,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = masterPassword,
                    onValueChange = onMasterPasswordChange,
                    label = { Text("Senha master") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    enabled = !isSyncing,
                    modifier = Modifier.fillMaxWidth()
                )
                errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (syncStatus.state == SyncState.ERROR || syncStatus.state == SyncState.CONFLICT) {
                    val message = syncStatus.message ?: "Falha ao sincronizar. Verifique os dados."
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !isSyncing) {
                Text(if (isSyncing) "Conectando..." else "Conectar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isSyncing) {
                Text("Cancelar")
            }
        }
    )
}
