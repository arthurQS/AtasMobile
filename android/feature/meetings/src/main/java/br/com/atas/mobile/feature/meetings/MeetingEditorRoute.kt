package br.com.atas.mobile.feature.meetings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.com.atas.mobile.core.data.model.Hymn
import br.com.atas.mobile.core.data.model.Meeting
import br.com.atas.mobile.core.data.model.MeetingDetails
import br.com.atas.mobile.core.data.model.MeetingSpeaker
import br.com.atas.mobile.core.data.repository.SyncState
import br.com.atas.mobile.core.data.repository.SyncStatus
import kotlinx.coroutines.launch

@Composable
fun MeetingEditorRoute(
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
    viewModel: MeetingEditorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    MeetingEditorScreen(
        state = uiState,
        onBack = onBack,
        onDateChange = viewModel::updateDate,
        onTitleChange = viewModel::updateTitle,
        onDetailsChange = viewModel::updateDetails,
        onSave = { viewModel.save(onSaved) },
        onReloadRemote = viewModel::reloadFromRemote,
        onOverwriteRemote = viewModel::overwriteRemote
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingEditorScreen(
    state: MeetingEditorUiState,
    onBack: () -> Unit,
    onDateChange: (String) -> Unit,
    onTitleChange: (String) -> Unit,
    onDetailsChange: (MeetingDetails) -> Unit,
    onSave: () -> Unit,
    onReloadRemote: () -> Unit,
    onOverwriteRemote: () -> Unit
) {
    val scrollState = rememberScrollState()
    val details = state.details
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val pdfGenerator = remember { MeetingPdfGenerator() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(if (state.meetingId == null) "Nova agenda" else "Editar agenda")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                val meeting = state.toMeeting()
                                runCatching {
                                    val uri = pdfGenerator.generate(context, meeting)
                                    sharePdf(context, uri)
                                }.onFailure { throwable ->
                                    snackbarHostState.showSnackbar(
                                        throwable.message ?: "Erro ao gerar PDF"
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = "Exportar PDF")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            MeetingHeaderCard(state = state, details = details)

            SectionCard(
                title = "Informacoes gerais",
                subtitle = "Dados administrativos e lideranca",
                icon = Icons.Default.Info
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.title,
                        onValueChange = onTitleChange,
                        label = { Text("Titulo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = state.date,
                        onValueChange = onDateChange,
                        label = { Text("Data (AAAA-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = details.orgao,
                        onValueChange = { onDetailsChange(details.copy(orgao = it)) },
                        label = { Text("Orgao") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = details.frequencia,
                        onValueChange = { onDetailsChange(details.copy(frequencia = it)) },
                        label = { Text("Frequencia") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = details.preside,
                        onValueChange = { onDetailsChange(details.copy(preside = it)) },
                        label = { Text("Preside") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = details.dirige,
                        onValueChange = { onDetailsChange(details.copy(dirige = it)) },
                        label = { Text("Dirige") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = details.ala,
                        onValueChange = { onDetailsChange(details.copy(ala = it)) },
                        label = { Text("Ala") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = details.organista,
                        onValueChange = { onDetailsChange(details.copy(organista = it)) },
                        label = { Text("Organista") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = details.regente,
                        onValueChange = { onDetailsChange(details.copy(regente = it)) },
                        label = { Text("Regente") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = details.tipo,
                        onValueChange = { onDetailsChange(details.copy(tipo = it)) },
                        label = { Text("Tipo de reuniao") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            SectionCard(
                title = "Abertura",
                subtitle = "Defina hino, oracao e anuncios iniciais",
                icon = Icons.Default.CalendarToday
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = details.anuncios,
                        onValueChange = { onDetailsChange(details.copy(anuncios = it)) },
                        label = { Text("Anuncios") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    HymnPickerField(
                        label = "Hino de abertura",
                        value = details.hinos.abertura,
                        hymns = state.hymns,
                        onValueChange = { onDetailsChange(details.copy(hinos = details.hinos.copy(abertura = it))) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = details.oracoes.abertura,
                        onValueChange = { onDetailsChange(details.copy(oracoes = details.oracoes.copy(abertura = it))) },
                        label = { Text("Oracao de abertura") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            SectionCard(
                title = "Chamados e apoios",
                subtitle = "Sustentacoes, chamados e desobrigacoes",
                icon = Icons.Default.Description
            ) {
                OutlinedTextField(
                    value = details.desobrigacoes,
                    onValueChange = { onDetailsChange(details.copy(desobrigacoes = it)) },
                    label = { Text("Desobrigacoes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                OutlinedTextField(
                    value = details.chamados,
                    onValueChange = { onDetailsChange(details.copy(chamados = it)) },
                    label = { Text("Chamados, apoios e boas-vindas") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }

            SectionCard(
                title = "Sacramento",
                subtitle = "Detalhes do sacramento e dos oficiantes",
                icon = Icons.AutoMirrored.Filled.PlaylistAdd
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HymnPickerField(
                        label = "Hino do sacramento",
                        value = details.hinos.sacramento,
                        hymns = state.hymns,
                        onValueChange = { onDetailsChange(details.copy(hinos = details.hinos.copy(sacramento = it))) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = details.oficiantesSacramento,
                        onValueChange = { onDetailsChange(details.copy(oficiantesSacramento = it)) },
                        label = { Text("Oficiantes do sacramento") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            SpeakersSection(details = details, hymns = state.hymns, onDetailsChange = onDetailsChange)

            SectionCard(
                title = "Encerramento",
                subtitle = "Defina o encerramento e o agradecimento final",
                icon = Icons.Default.MusicNote
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    HymnPickerField(
                        label = "Hino de encerramento",
                        value = details.hinos.encerramento,
                        hymns = state.hymns,
                        onValueChange = { onDetailsChange(details.copy(hinos = details.hinos.copy(encerramento = it))) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = details.oracoes.encerramento,
                        onValueChange = { onDetailsChange(details.copy(oracoes = details.oracoes.copy(encerramento = it))) },
                        label = { Text("Oracao de encerramento") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            SectionCard(
                title = "Observacoes",
                subtitle = "Notas adicionais para a agenda",
                icon = Icons.Default.Info
            ) {
                OutlinedTextField(
                    value = details.observacoes,
                    onValueChange = { onDetailsChange(details.copy(observacoes = it)) },
                    label = { Text("Observacoes gerais") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4
                )
            }

            MeetingFooterActions(
                isSaving = state.isSaving,
                isReloading = state.isReloading,
                isOverwriting = state.isOverwriting,
                canOverwrite = state.canOverwrite,
                errorMessage = state.errorMessage,
                syncStatus = state.syncStatus,
                onSave = onSave,
                onReloadRemote = onReloadRemote,
                onOverwriteRemote = onOverwriteRemote
            )
        }
    }
}

@Composable
private fun MeetingFooterActions(
    isSaving: Boolean,
    isReloading: Boolean,
    isOverwriting: Boolean,
    canOverwrite: Boolean,
    errorMessage: String?,
    syncStatus: SyncStatus,
    onSave: () -> Unit,
    onReloadRemote: () -> Unit,
    onOverwriteRemote: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        errorMessage?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (syncStatus.state != SyncState.DISABLED) {
            val message = syncStatus.message?.takeIf { it.isNotBlank() }
            val label = when (syncStatus.state) {
                SyncState.CONNECTED -> "Sincronizacao conectada"
                SyncState.ERROR -> "Erro de sincronizacao"
                SyncState.CONFLICT -> "Conflito de sincronizacao"
                SyncState.DISABLED -> "Sincronizacao desativada"
            }
            Text(
                text = message?.let { "$label: $it" } ?: label,
                color = if (syncStatus.state == SyncState.ERROR || syncStatus.state == SyncState.CONFLICT) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (syncStatus.state == SyncState.CONFLICT) {
            OutlinedButton(
                onClick = onReloadRemote,
                enabled = !isSaving && !isReloading && !isOverwriting,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isReloading) "Recarregando..." else "Recarregar remoto")
            }
            if (canOverwrite) {
                OutlinedButton(
                    onClick = onOverwriteRemote,
                    enabled = !isSaving && !isReloading && !isOverwriting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isOverwriting) "Sobrescrevendo..." else "Forcar overwrite")
                }
            }
        }
        Button(
            onClick = onSave,
            enabled = !isSaving && !isReloading && !isOverwriting,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSaving) "Salvando..." else "Salvar agenda")
        }
        Text(
            text = "Uma copia local e salva automaticamente e pode ser enviada ao Google Drive nas configuracoes.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MeetingHeaderCard(
    state: MeetingEditorUiState,
    details: MeetingDetails
) {
    val title = state.title.ifBlank { "Agenda sem titulo" }
    val dateLabel = if (state.date.isBlank()) "Data ainda nao informada" else "Prevista para ${state.date}"
    val createdAt = state.createdAt?.takeIf { it.isNotBlank() }
    val hymnCount = listOf(
        details.hinos.abertura,
        details.hinos.sacramento,
        details.hinos.intermediario,
        details.hinos.encerramento
    ).count { it.isNotBlank() }
    val speakerCount = details.oradores.count { it.nome.isNotBlank() || it.assunto.isNotBlank() }.takeIf { it > 0 }
        ?: details.oradores.size
    val chamadosCount = listOf(details.chamados, details.desobrigacoes).count { it.isNotBlank() }

    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = dateLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            createdAt?.let {
                Text(
                    text = "Criada em $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MeetingMetadataChip("Orgao", details.orgao)
                MeetingMetadataChip("Ala", details.ala)
                MeetingMetadataChip("Frequencia", details.frequencia)
                MeetingMetadataChip("Tipo", details.tipo)
                MeetingMetadataChip("Preside", details.preside)
                MeetingMetadataChip("Dirige", details.dirige)
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                MeetingStatMetric(
                    label = "Hinos definidos",
                    value = hymnCount.toString()
                )
                MeetingStatMetric(
                    label = "Oradores",
                    value = speakerCount.toString()
                )
                MeetingStatMetric(
                    label = "Chamados",
                    value = chamadosCount.toString()
                )
            }
        }
    }
}

private fun MeetingEditorUiState.toMeeting(): Meeting =
    Meeting(
        id = meetingId ?: 0L,
        date = date,
        title = title.ifBlank { "Agenda" },
        details = details,
        createdAt = createdAt,
        syncVersion = syncVersion
    )

private fun sharePdf(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooser = Intent.createChooser(intent, "Compartilhar agenda em PDF")
    chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(chooser)
    } catch (ex: ActivityNotFoundException) {
        throw ex
    }
}

@Composable
private fun MeetingMetadataChip(label: String, value: String) {
    if (value.isBlank()) return
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)
    ) {
        Text(
            text = "$label - $value",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MeetingStatMetric(
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun SpeakersSection(
    details: MeetingDetails,
    hymns: List<Hymn>,
    onDetailsChange: (MeetingDetails) -> Unit
) {
    val speakers = details.oradores
    SectionCard(
        title = "Discursos",
        subtitle = "Organize oradores e temas",
        icon = Icons.Default.People
    ) {
        val hymnPlacement = calculateHymnPlacement(speakers)

        if (speakers.isEmpty()) {
            Text("Reuniao de Testemunhos", style = MaterialTheme.typography.bodyMedium)
        } else {
            speakers.forEachIndexed { index, speaker ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Orador ${index + 1}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = if (speaker.assunto.isBlank()) "Tema ainda nao definido" else speaker.assunto,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            IconButton(onClick = {
                                val updated = speakers.toMutableList().also { it.removeAt(index) }
                                onDetailsChange(details.copy(oradores = updated))
                            }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remover orador")
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        OutlinedTextField(
                            value = speaker.nome,
                            onValueChange = {
                                val updated = speakers.toMutableList()
                                updated[index] = speaker.copy(nome = it)
                                onDetailsChange(details.copy(oradores = updated))
                            },
                            label = { Text("Nome do orador") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = speaker.assunto,
                            onValueChange = {
                                val updated = speakers.toMutableList()
                                updated[index] = speaker.copy(assunto = it)
                                onDetailsChange(details.copy(oradores = updated))
                            },
                            label = { Text("Tema do discurso") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                hymnPlacement?.takeIf { index == it.insertAfterIndex }?.let { placement ->
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    ) {
                        HymnPickerField(
                            label = "Hino intermediario",
                            value = details.hinos.intermediario,
                            hymns = hymns,
                            onValueChange = {
                                onDetailsChange(details.copy(hinos = details.hinos.copy(intermediario = it)))
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = placement.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        OutlinedButton(
            onClick = {
                val updated = details.oradores + MeetingSpeaker()
                onDetailsChange(details.copy(oradores = updated))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Adicionar orador")
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                icon?.let {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    subtitle?.let { text ->
                        Text(
                            text = text,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            content()
        }
    }
}

@Composable
private fun HymnPickerField(
    label: String,
    value: String,
    hymns: List<Hymn>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var query by remember(value) { mutableStateOf(value) }

    LaunchedEffect(value) {
        if (value != query) query = value
    }

    val filtered = remember(query, hymns) {
        val normalized = query.substringBefore("-").trim().lowercase()
        if (normalized.isBlank()) hymns
        else hymns.filter {
            it.title.lowercase().contains(normalized) ||
                it.number.toString().contains(normalized)
        }
    }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    onValueChange(it)
                    expanded = true
                },
                singleLine = true,
                placeholder = { Text("Numero ou titulo") },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (query.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    query = ""
                                    onValueChange("")
                                    expanded = false
                                }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Limpar campo"
                                )
                            }
                        }
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { state ->
                        if (state.isFocused) expanded = true
                    }
            )
            DropdownMenu(
                expanded = expanded && filtered.isNotEmpty(),
                onDismissRequest = { expanded = false },
                modifier = Modifier.fillMaxWidth(),
                properties = PopupProperties(focusable = false)
            ) {
                Column(
                    modifier = Modifier
                        .requiredSizeIn(maxHeight = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    filtered.forEach { hymn ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(
                                        text = "${hymn.number} - ${hymn.title}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    hymn.category?.let {
                                        Text(
                                            text = it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            onClick = {
                                val selection = "${hymn.number} - ${hymn.title}"
                                query = selection
                                onValueChange(selection)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}
