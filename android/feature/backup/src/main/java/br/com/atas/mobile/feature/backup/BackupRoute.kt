package br.com.atas.mobile.feature.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate

@Composable
fun BackupRoute(
    onBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let { viewModel.exportLocal(it) }
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.importLocal(it) }
    }
    val driveFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let { viewModel.connectDriveFolder(it) }
    }

    BackupScreen(
        message = uiState.localMessage,
        driveMessage = uiState.driveMessage,
        driveFolderName = uiState.driveFolderName,
        isDriveLinked = uiState.isDriveLinked,
        isDriveBusy = uiState.isDriveBusy,
        onBack = onBack,
        onExportLocal = {
            val defaultName = "atas-backup-${LocalDate.now()}.zip"
            exportLauncher.launch(defaultName)
        },
        onImportLocal = {
            importLauncher.launch(arrayOf("application/zip"))
        },
        onSelectDriveFolder = {
            driveFolderLauncher.launch(resolveDriveInitialUri(uiState.driveFolderUri, uiState.driveDefaultUri))
        },
        onUploadDrive = { viewModel.uploadDriveBackup() },
        onSyncDrive = { viewModel.syncFromDrive() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    message: String?,
    driveMessage: String?,
    driveFolderName: String?,
    isDriveLinked: Boolean,
    isDriveBusy: Boolean,
    onBack: () -> Unit,
    onExportLocal: () -> Unit,
    onImportLocal: () -> Unit,
    onSelectDriveFolder: () -> Unit,
    onUploadDrive: () -> Unit,
    onSyncDrive: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Backup local") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = "Salve um arquivo .zip com a base local e restaure quando precisar.",
                style = MaterialTheme.typography.bodyLarge
            )
            message?.let {
                Text(text = it, color = MaterialTheme.colorScheme.primary)
            }
            Button(
                onClick = onExportLocal,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                Text(text = "Salvar backup local", modifier = Modifier.padding(start = 8.dp))
            }
            Button(
                onClick = onImportLocal,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Restore, contentDescription = null)
                Text(text = "Restaurar de arquivo local", modifier = Modifier.padding(start = 8.dp))
            }
            Text(
                text = "Google Drive",
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = driveFolderName?.let { "Pasta selecionada: $it" } ?: "Nenhuma pasta selecionada.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            driveMessage?.let {
                Text(text = it, color = MaterialTheme.colorScheme.primary)
            }
            Button(
                onClick = onSelectDriveFolder,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Cloud, contentDescription = null)
                Text(text = "Selecionar pasta do Google Drive", modifier = Modifier.padding(start = 8.dp))
            }
            Button(
                onClick = onUploadDrive,
                enabled = isDriveLinked && !isDriveBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null)
                Text(
                    text = if (isDriveBusy) "Enviando..." else "Salvar no Google Drive",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Button(
                onClick = onSyncDrive,
                enabled = isDriveLinked && !isDriveBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Sync, contentDescription = null)
                Text(
                    text = if (isDriveBusy) "Sincronizando..." else "Sincronizar agora",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

private fun resolveDriveInitialUri(savedUri: String?, defaultUri: String?): Uri? =
    savedUri?.let(Uri::parse)
        ?: defaultUri?.let(Uri::parse)
