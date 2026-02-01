package com.example.mobiletaskmanager.ui.screens

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import com.example.mobiletaskmanager.data.model.Issue
import com.example.mobiletaskmanager.data.model.Label
import com.example.mobiletaskmanager.ui.MainUiState
import com.example.mobiletaskmanager.ui.components.*
import com.example.mobiletaskmanager.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    uiState: MainUiState,
    onRefresh: () -> Unit,
    onCloseIssue: (Issue) -> Unit,
    onUpdateStatus: (Issue, Label) -> Unit,
    onAddOneOff: (String, List<Label>) -> Unit,
    onAddRoutine: (String, String, List<Label>) -> Unit,
    onAddNote: (String) -> Unit,
    onSetTaskFilter: (TaskFilterCriteria) -> Unit,
    onSetTaskSort: (SortOption) -> Unit
) {
    var showNoteDialog by remember { mutableStateOf(false) }
    var showAddTaskDialog by remember { mutableStateOf(false) }

    var showFilterSheet by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val pullToRefreshState = rememberPullToRefreshState()

    val statusLabels = remember(uiState.labels) {
        uiState.labels.filter { it.name.startsWith("s:") }.sortedBy { it.name }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true },
                containerColor = PrimaryAccent,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        },
        containerColor = AppBackground
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = padding.calculateBottomPadding())
                .background(AppBackground)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header (Active Tasks + Buttons)
                Surface(color = SurfaceColor, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Active Tasks: ${uiState.filteredIssues.size}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        FilterSortButtons(
                            onFilterClick = { showFilterSheet = true },
                            onSortClick = { showSortSheet = true },
                            activeFilterCount = uiState.taskFilterCriteria.selectedLabels.size
                        )
                    }
                }
                HorizontalDivider(color = DividerColor)

                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = onRefresh,
                    state = pullToRefreshState,
                    modifier = Modifier.weight(1f)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp)
                    ) {
                        items(uiState.filteredIssues, key = { it.number }) { issue ->
                            TaskItemRow(
                                issue = issue,
                                allStatusLabels = statusLabels,
                                onClose = { onCloseIssue(issue) },
                                onStatusChange = { newLabel -> onUpdateStatus(issue, newLabel) }
                            )
                            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                        }
                    }
                }
            }

            FloatingActionButton(
                onClick = { showNoteDialog = true },
                containerColor = SurfaceColor,
                contentColor = PrimaryAccent,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Chat, contentDescription = "Quick Note")
            }
        }
    }

    // --- Dialogs ---
    if (showNoteDialog) {
        QuickNoteDialog(
            onDismiss = { showNoteDialog = false },
            onSend = {
                onAddNote(it)
                coroutineScope.launch { delay(500); onRefresh() }
                showNoteDialog = false
            }
        )
    }

    if (showAddTaskDialog) {
        AdvancedAddTaskDialog(
            availableLabels = uiState.labels,
            onDismiss = { showAddTaskDialog = false },
            onAddOneOff = { title, labels ->
                onAddOneOff(title, labels)
                coroutineScope.launch { delay(500); onRefresh() }
                showAddTaskDialog = false
            },
            onAddRoutine = { title, schedule, labels ->
                onAddRoutine(title, schedule, labels)
                coroutineScope.launch { delay(500); onRefresh() }
                showAddTaskDialog = false
            }
        )
    }

    // Sheets
    if (showSortSheet) {
        SortBottomSheet(
            currentSort = uiState.taskSortOption,
            onSortSelected = onSetTaskSort,
            onDismiss = { showSortSheet = false }
        )
    }
    if (showFilterSheet) {
        TaskFilterBottomSheet(
            labels = uiState.labels,
            currentFilter = uiState.taskFilterCriteria,
            onApply = onSetTaskFilter,
            onReset = { onSetTaskFilter(TaskFilterCriteria()) },
            onDismiss = { showFilterSheet = false }
        )
    }
}

// ... Helper関数, TaskItemRow, Dialogs は以前の内容を維持 ...
fun parseLabelColor(hex: String): Color {
    return try {
        val colorString = if (hex.startsWith("#")) hex else "#$hex"
        Color(AndroidColor.parseColor(colorString))
    } catch (e: Exception) {
        Color.Gray
    }
}

fun getContrastTextColor(backgroundColor: Color): Color {
    val androidColor = backgroundColor.toArgb()
    val luminance = ColorUtils.calculateLuminance(androidColor)
    return if (luminance > 0.5) Color.Black else Color.White
}

@Composable
fun TaskItemRow(issue: Issue, allStatusLabels: List<Label>, onClose: () -> Unit, onStatusChange: (Label) -> Unit) {
    val currentStatusLabel = issue.labels.find { it.name.startsWith("s:") }
    var showStatusMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth().background(SurfaceColor).padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    Surface(
                        onClick = { showStatusMenu = true },
                        shape = RoundedCornerShape(4.dp),
                        color = if (currentStatusLabel != null) parseLabelColor(currentStatusLabel.color) else Color.LightGray.copy(alpha = 0.3f),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) {
                            Text(
                                text = currentStatusLabel?.name?.removePrefix("s:") ?: "No Status",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (currentStatusLabel != null) getContrastTextColor(parseLabelColor(currentStatusLabel.color)) else TextSecondary
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Change Status", tint = if (currentStatusLabel != null) getContrastTextColor(parseLabelColor(currentStatusLabel.color)) else TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                    DropdownMenu(expanded = showStatusMenu, onDismissRequest = { showStatusMenu = false }, modifier = Modifier.background(SurfaceColor)) {
                        allStatusLabels.forEach { label ->
                            DropdownMenuItem(
                                text = { Text(text = label.name.removePrefix("s:"), color = TextPrimary) },
                                onClick = { onStatusChange(label); showStatusMenu = false },
                                leadingIcon = { Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(parseLabelColor(label.color))) }
                            )
                        }
                        if (allStatusLabels.isEmpty()) {
                            DropdownMenuItem(text = { Text("No status labels found", color = TextSecondary) }, onClick = { showStatusMenu = false }, enabled = false)
                        }
                    }
                }
                Text(text = issue.title, style = MaterialTheme.typography.bodyLarge, color = TextPrimary, modifier = Modifier.weight(1f))
            }
            if (issue.labels.isNotEmpty()) {
                val visibleLabels = issue.labels.filter { !it.name.startsWith("s:") && it.name != "mobile-entry" && it.name != "routine" && it.name != "mobile-added" }
                if (visibleLabels.isNotEmpty()) {
                    Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        visibleLabels.take(4).forEach { label ->
                            val bgColor = parseLabelColor(label.color)
                            Text(text = label.name, style = MaterialTheme.typography.labelSmall, color = getContrastTextColor(bgColor), modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(bgColor).padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(onClick = { onClose() }, modifier = Modifier.size(40.dp).background(Color.Gray.copy(alpha = 0.1f), CircleShape)) {
            Icon(imageVector = Icons.Default.Check, contentDescription = "Complete Task", tint = PrimaryAccent)
        }
    }
}

@Composable
fun QuickNoteDialog(onDismiss: () -> Unit, onSend: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(100); focusRequester.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Note") },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, placeholder = { Text("What's on your mind?") }, modifier = Modifier.fillMaxWidth().focusRequester(focusRequester), maxLines = 5) },
        confirmButton = { TextButton(onClick = { if (text.isNotBlank()) onSend(text) }, enabled = text.isNotBlank()) { Text("Send", color = PrimaryAccent) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } },
        containerColor = SurfaceColor, titleContentColor = TextPrimary, textContentColor = TextPrimary
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AdvancedAddTaskDialog(availableLabels: List<Label>, onDismiss: () -> Unit, onAddOneOff: (String, List<Label>) -> Unit, onAddRoutine: (String, String, List<Label>) -> Unit) {
    var title by remember { mutableStateOf("") }
    var selectedLabels by remember { mutableStateOf<List<Label>>(emptyList()) }
    var isRoutine by remember { mutableStateOf(false) }
    val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    var selectedDays by remember { mutableStateOf<Set<String>>(emptySet()) }
    val priorityLabels = remember(availableLabels) { availableLabels.filter { it.name.startsWith("p:") } }
    val timeLabels = remember(availableLabels) { availableLabels.filter { it.name.startsWith("t:") } }
    val contextLabels = remember(availableLabels) { availableLabels.filter { it.name.startsWith("c:") } }
    val otherLabels = remember(availableLabels) { availableLabels.filter { !it.name.startsWith("p:") && !it.name.startsWith("c:") && !it.name.startsWith("t:") && !it.name.startsWith("s:") } }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(100); focusRequester.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Task", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Task Title") }, singleLine = true, modifier = Modifier.fillMaxWidth().focusRequester(focusRequester))
                LabelSection(title = "Priority", labels = priorityLabels, selectedLabels = selectedLabels, prefixToRemove = "p:", onSelectionChanged = { l, isSelected -> val others = selectedLabels.filter { !it.name.startsWith("p:") }; selectedLabels = if (isSelected) others else others + l })
                LabelSection(title = "Time Estimate", labels = timeLabels, selectedLabels = selectedLabels, prefixToRemove = "t:", onSelectionChanged = { l, isSelected -> val others = selectedLabels.filter { !it.name.startsWith("t:") }; selectedLabels = if (isSelected) others else others + l })
                LabelSection(title = "Context", labels = contextLabels, selectedLabels = selectedLabels, prefixToRemove = "c:", onSelectionChanged = { l, isSelected -> selectedLabels = if (isSelected) selectedLabels - l else selectedLabels + l })
                LabelSection(title = "Other Labels", labels = otherLabels.take(20), selectedLabels = selectedLabels, prefixToRemove = "", onSelectionChanged = { l, isSelected -> selectedLabels = if (isSelected) selectedLabels - l else selectedLabels + l })
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Repeat Task (Routine)", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isRoutine, onCheckedChange = { isRoutine = it }, colors = SwitchDefaults.colors(checkedThumbColor = PrimaryAccent, checkedTrackColor = PrimaryAccent.copy(alpha = 0.3f)))
                }
                if (isRoutine) {
                    Text("Select Days:", style = MaterialTheme.typography.bodySmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        days.forEach { day ->
                            val isSelected = selectedDays.contains(day)
                            FilterChip(selected = isSelected, onClick = { selectedDays = if (isSelected) selectedDays - day else selectedDays + day }, label = { Text(day) })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        // ▼▼▼ 修正: s:todo を自動付与 ▼▼▼
                        val todoLabel = availableLabels.find { it.name == "s:todo" }
                        val finalLabels = if (todoLabel != null && !selectedLabels.contains(todoLabel)) {
                            selectedLabels + todoLabel
                        } else {
                            selectedLabels
                        }

                        if (isRoutine) {
                            val schedule = if (selectedDays.size == 7) "daily" else "weekly:${selectedDays.joinToString(",").lowercase()}"
                            onAddRoutine(title, schedule, finalLabels)
                        } else {
                            onAddOneOff(title, finalLabels)
                        }
                    }
                },
                enabled = title.isNotBlank() && (!isRoutine || selectedDays.isNotEmpty())
            ) {
                Text(if (isRoutine) "Add Routine" else "Add Task", color = PrimaryAccent)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } },
        containerColor = SurfaceColor, titleContentColor = TextPrimary, textContentColor = TextPrimary
    )
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LabelSection(title: String, labels: List<Label>, selectedLabels: List<Label>, prefixToRemove: String, onSelectionChanged: (Label, Boolean) -> Unit) {
    if (labels.isNotEmpty()) {
        Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TextPrimary)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            labels.forEach { label ->
                val isSelected = selectedLabels.contains(label)
                val displayName = if (prefixToRemove.isNotEmpty()) label.name.removePrefix(prefixToRemove) else label.name
                val labelColor = parseLabelColor(label.color)
                FilterChip(
                    selected = isSelected, onClick = { onSelectionChanged(label, isSelected) }, label = { Text(text = displayName, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    enabled = true,
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = labelColor, selectedLabelColor = getContrastTextColor(labelColor), containerColor = Color.Transparent, labelColor = TextPrimary, disabledContainerColor = Color.Gray.copy(alpha = 0.1f), disabledLabelColor = Color.Gray),
                    border = FilterChipDefaults.filterChipBorder(enabled = true, selected = isSelected, borderColor = labelColor, selectedBorderColor = labelColor, borderWidth = 1.dp),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }
    }
}