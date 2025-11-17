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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
        onMeetingSelected = onMeetingSelected
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingsScreen(
    state: MeetingsUiState,
    onOpenBackup: () -> Unit,
    onCreateMeeting: () -> Unit,
    onMeetingSelected: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(text = "Atas") },
                actions = {
                    IconButton(onClick = onOpenBackup) {
                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = "Backup")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onCreateMeeting) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Nova ata")
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
                text = "Nenhuma ata cadastrada",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Toque no botão “+” para criar a primeira ata sacramental.",
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
