package com.example.mobiletaskmanager.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobiletaskmanager.ui.GuidelineScreen
import com.example.mobiletaskmanager.ui.screens.archive.ArchiveScreen
import com.example.mobiletaskmanager.ui.screens.archive.ArchiveViewModel
import com.example.mobiletaskmanager.ui.screens.ideas.IdeaScreen
import com.example.mobiletaskmanager.ui.screens.ideas.IdeaViewModel
import com.example.mobiletaskmanager.ui.screens.knowledge.KnowledgeScreen
import com.example.mobiletaskmanager.ui.screens.knowledge.KnowledgeViewModel
import com.example.mobiletaskmanager.ui.screens.notes.NoteScreen
import com.example.mobiletaskmanager.ui.screens.notes.NoteViewModel
import com.example.mobiletaskmanager.ui.screens.reports.ReportScreen
import com.example.mobiletaskmanager.ui.screens.reports.ReportViewModel
import com.example.mobiletaskmanager.ui.screens.tasks.TaskScreen
import com.example.mobiletaskmanager.ui.screens.tasks.TaskViewModel
import com.example.mobiletaskmanager.ui.theme.*
import kotlinx.coroutines.launch

enum class Screen(val title: String, val icon: ImageVector) {
    Tasks("Tasks", Icons.Default.List),
    Notes("Notes", Icons.Default.ChatBubbleOutline),
    Ideas("Ideas", Icons.Default.Lightbulb),
    Reports("Reports", Icons.Default.Description),
    Knowledge("Knowledge", Icons.Default.EditNote),
    Archive("Archive", Icons.Default.Check),
    Guidelines("Guidelines", Icons.Default.Info)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    taskViewModel: TaskViewModel,
    noteViewModel: NoteViewModel,
    ideaViewModel: IdeaViewModel,
    reportViewModel: ReportViewModel,
    knowledgeViewModel: KnowledgeViewModel,
    archiveViewModel: ArchiveViewModel
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf(Screen.Tasks) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = SurfaceColor,
                drawerContentColor = TextPrimary
            ) {
                Spacer(Modifier.height(12.dp))
                Text("Task Manager", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                HorizontalDivider(color = DividerColor)

                Screen.values().forEach { screen ->
                    NavigationDrawerItem(
                        icon = { Icon(screen.icon, contentDescription = null, tint = if(screen == currentScreen) PrimaryAccent else TextSecondary) },
                        label = { Text(screen.title, color = if(screen == currentScreen) PrimaryAccent else TextPrimary) },
                        selected = screen == currentScreen,
                        onClick = {
                            currentScreen = screen
                            scope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = PrimaryAccent.copy(alpha = 0.1f),
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            containerColor = AppBackground,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            currentScreen.title,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = SurfaceColor
                    )
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentScreen) {
                    Screen.Tasks -> {
                        val state by taskViewModel.uiState.collectAsState()
                        TaskScreen(
                            uiState = state,
                            onRefresh = taskViewModel::refresh,
                            onCloseIssue = taskViewModel::closeIssue,
                            onUpdateStatus = taskViewModel::updateIssueStatus,
                            onAddOneOff = taskViewModel::addOneOffTask,
                            onAddRoutine = taskViewModel::addRoutineTask,
                            onAddNote = noteViewModel::addNote,
                            onSetTaskFilter = taskViewModel::setFilter,
                            onSetTaskSort = taskViewModel::setSort
                        )
                    }
                    Screen.Notes -> {
                        val notes by noteViewModel.notes.collectAsState()
                        val isRefreshing by noteViewModel.isRefreshing.collectAsState()
                        NoteScreen(
                            notes = notes,
                            isRefreshing = isRefreshing,
                            onAddNote = noteViewModel::addNote,
                            onRefresh = noteViewModel::refresh
                        )
                    }
                    Screen.Ideas -> {
                        val state by ideaViewModel.uiState.collectAsState()
                        IdeaScreen(
                            uiState = state,
                            viewModel = ideaViewModel
                        )
                    }
                    Screen.Reports -> {
                        val state by reportViewModel.uiState.collectAsState()
                        ReportScreen(
                            uiState = state,
                            onLoadReports = reportViewModel::loadReports,
                            onSelectReport = reportViewModel::selectReport,
                            onBackToList = reportViewModel::clearSelectedReport,
                            onSetFilter = reportViewModel::setFilter,
                            onSetSort = reportViewModel::setSort
                        )
                    }
                    Screen.Knowledge -> {
                        val state by knowledgeViewModel.uiState.collectAsState()
                        KnowledgeScreen(
                            uiState = state,
                            onLoad = knowledgeViewModel::loadKnowledgeFiles,
                            onSelect = knowledgeViewModel::selectKnowledge,
                            onCreate = knowledgeViewModel::startCreateKnowledge,
                            onCloseEditor = knowledgeViewModel::closeEditor,
                            onSave = knowledgeViewModel::saveKnowledge
                        )
                    }
                    Screen.Archive -> {
                        val state by archiveViewModel.uiState.collectAsState()
                        ArchiveScreen(
                            uiState = state,
                            onLoadArchive = archiveViewModel::loadInitialData,
                            onSetFilter = archiveViewModel::setFilter,
                            onSetSort = archiveViewModel::setSort
                        )
                    }
                    Screen.Guidelines -> GuidelineScreen()
                }
            }
        }
    }
}
