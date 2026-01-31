package com.example.mobiletaskmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobiletaskmanager.data.model.Issue
import com.example.mobiletaskmanager.data.model.Label
import com.example.mobiletaskmanager.ui.MainUiState
import com.example.mobiletaskmanager.ui.components.AddTaskSheetContent
import com.example.mobiletaskmanager.ui.components.TaskRow
import com.example.mobiletaskmanager.ui.theme.*
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    uiState: MainUiState,
    onCloseTask: (Issue) -> Unit,
    onRefresh: () -> Unit,
    onUpdateStatus: (Issue, Label) -> Unit,
    onAddOneOff: (String, List<Label>) -> Unit,
    onAddRoutine: (String, String, List<Label>) -> Unit
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pullToRefreshState = rememberPullToRefreshState()

    val statusLabels = remember(uiState.labels) {
        uiState.labels.filter { it.name.startsWith("s:") }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(AppBackground)) {
            // Header
            Surface(color = SurfaceColor, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = uiState.statusMessage,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(16.dp)
                )
            }
            HorizontalDivider(color = DividerColor, thickness = 1.dp)

            // Task List
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                state = pullToRefreshState,
                modifier = Modifier.weight(1f)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(uiState.issues) { issue ->
                        TaskRow(
                            issue = issue,
                            statusLabels = statusLabels,
                            onClose = { onCloseTask(it) },
                            onStatusChange = { newLabel -> onUpdateStatus(issue, newLabel) }
                        )
                        HorizontalDivider(color = DividerColor, thickness = 1.dp)
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { showBottomSheet = true },
            containerColor = PrimaryAccent,
            contentColor = SurfaceColor,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp).size(56.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Task", modifier = Modifier.size(28.dp))
        }

        // Bottom Sheet
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = SurfaceColor,
                contentColor = TextPrimary
            ) {
                AddTaskSheetContent(
                    availableLabels = uiState.labels,
                    onCancel = { showBottomSheet = false },
                    onSubmit = { title, isRoutine, selectedDays, selectedLabels ->
                        if (isRoutine) {
                            val schedule = if (selectedDays.size == 7) "daily" else {
                                val daysStr = selectedDays.joinToString(",") {
                                    it.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).lowercase()
                                }
                                "weekly:$daysStr"
                            }
                            onAddRoutine(title, schedule, selectedLabels)
                        } else {
                            onAddOneOff(title, selectedLabels)
                        }
                        showBottomSheet = false
                    }
                )
            }
        }
    }
}