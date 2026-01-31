package com.example.mobiletaskmanager.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobiletaskmanager.ui.GuidelineScreen
import com.example.mobiletaskmanager.ui.MainUiState
import com.example.mobiletaskmanager.ui.MainViewModel
import com.example.mobiletaskmanager.ui.screens.ReportScreen
import com.example.mobiletaskmanager.ui.screens.TaskScreen
import com.example.mobiletaskmanager.ui.theme.*
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Check // 追加
import com.example.mobiletaskmanager.ui.screens.ArchiveScreen

enum class Screen(val title: String, val icon: ImageVector) {
    Tasks("Tasks", Icons.Default.List),
    Reports("Reports", Icons.Default.Description),
    Archive("Archive", Icons.Default.Check),
    Guidelines("Guidelines", Icons.Default.Info)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    uiState: MainUiState,
    viewModel: MainViewModel
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
                    Screen.Tasks -> TaskScreen(
                        uiState = uiState,
                        onCloseTask = viewModel::closeIssue,
                        onRefresh = viewModel::refresh,
                        onUpdateStatus = viewModel::updateIssueStatus,
                        onAddOneOff = viewModel::addOneOffTask,
                        onAddRoutine = viewModel::addRoutineTask
                    )
                    Screen.Reports -> ReportScreen(
                        uiState = uiState,
                        onLoadReports = viewModel::loadReports,
                        onSelectReport = viewModel::selectReport,
                        onBackToList = viewModel::clearSelectedReport
                    )
                    Screen.Archive -> ArchiveScreen(
                        uiState = uiState,
                        onLoadArchive = viewModel::loadClosedIssues
                    )
                    Screen.Guidelines -> GuidelineScreen()
                }
            }
        }
    }
}