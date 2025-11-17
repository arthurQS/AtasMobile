package br.com.atas.mobile.feature.backup

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
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

    BackupScreen(
        message = uiState.localMessage,
        onBack = onBack,
        onExportLocal = {
            val defaultName = "atas-backup-${LocalDate.now()}.zip"
            exportLauncher.launch(defaultName)
        },
        onImportLocal = {
            importLauncher.launch(arrayOf("application/zip"))
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    message: String?,
    onBack: () -> Unit,
    onExportLocal: () -> Unit,
    onImportLocal: () -> Unit,
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
        }
    }
}
