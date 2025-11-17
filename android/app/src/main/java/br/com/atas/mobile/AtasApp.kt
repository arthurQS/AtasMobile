package br.com.atas.mobile

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import br.com.atas.mobile.core.ui.theme.AtasTheme
import br.com.atas.mobile.feature.backup.BackupRoute
import br.com.atas.mobile.feature.meetings.MeetingEditorRoute
import br.com.atas.mobile.feature.meetings.MeetingsRoute

@Composable
fun AtasApp() {
    val navController = rememberNavController()

    AtasTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = AppDestinations.MEETINGS
            ) {
                composable(AppDestinations.MEETINGS) {
                    MeetingsRoute(
                        onOpenBackup = {
                            navController.navigate(AppDestinations.BACKUP)
                        },
                        onCreateMeeting = {
                            navController.navigate("${AppDestinations.MEETING_EDITOR}?${AppDestinations.MEETING_ID_ARG}=0")
                        },
                        onMeetingSelected = { id ->
                            navController.navigate("${AppDestinations.MEETING_EDITOR}?${AppDestinations.MEETING_ID_ARG}=$id")
                        }
                    )
                }
                composable(AppDestinations.BACKUP) {
                    BackupRoute(
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = "${AppDestinations.MEETING_EDITOR}?${AppDestinations.MEETING_ID_ARG}={${AppDestinations.MEETING_ID_ARG}}",
                    arguments = listOf(
                        navArgument(AppDestinations.MEETING_ID_ARG) {
                            type = NavType.LongType
                            defaultValue = 0L
                        }
                    )
                ) {
                    MeetingEditorRoute(
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

object AppDestinations {
    const val MEETINGS = "meetings"
    const val BACKUP = "backup"
    const val MEETING_EDITOR = "meeting_editor"
    const val MEETING_ID_ARG = "meetingId"
}
