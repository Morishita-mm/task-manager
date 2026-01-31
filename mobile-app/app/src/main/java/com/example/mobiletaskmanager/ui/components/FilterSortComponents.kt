package com.example.mobiletaskmanager.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobiletaskmanager.data.model.Label

// --- Data Models ---

enum class SortOption(val label: String) {
    CREATED_DESC("Newest Created"),
    CREATED_ASC("Oldest Created"),
    PRIORITY_DESC("Priority High"),
    PRIORITY_ASC("Priority Low"),
    NAME_ASC("Name A-Z"),
    NAME_DESC("Name Z-A")
}

data class TaskFilterCriteria(
    val selectedLabels: Set<String> = emptySet(),
    val dateQuery: String = ""
)

// --- UI Components ---

/**
 * ヘッダーの右側に配置するためのフィルタ・ソートボタン群
 */
@Composable
fun FilterSortButtons(
    onFilterClick: () -> Unit,
    onSortClick: () -> Unit,
    activeFilterCount: Int
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Filter Button
        IconButton(onClick = onFilterClick) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Filter",
                // フィルタ適用中はアクセントカラーにする
                tint = if (activeFilterCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        }

        // Sort Button
        IconButton(onClick = onSortClick) {
            Icon(
                imageVector = Icons.Default.Sort,
                contentDescription = "Sort",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    currentSort: SortOption,
    onSortSelected: (SortOption) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                "Sort by",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(16.dp)
            )
            SortOption.values().forEach { option ->
                ListItem(
                    headlineContent = { Text(option.label) },
                    leadingContent = {
                        RadioButton(
                            selected = option == currentSort,
                            onClick = null
                        )
                    },
                    modifier = Modifier.clickable {
                        onSortSelected(option)
                        onDismiss()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskFilterBottomSheet(
    labels: List<Label>,
    currentFilter: TaskFilterCriteria,
    onApply: (TaskFilterCriteria) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedLabels by remember { mutableStateOf(currentFilter.selectedLabels) }
    var dateQuery by remember { mutableStateOf(currentFilter.dateQuery) }

    val statusLabels = remember(labels) { labels.filter { it.name.startsWith("s:") } }
    val priorityLabels = remember(labels) { labels.filter { it.name.startsWith("p:") } }
    val contextLabels = remember(labels) { labels.filter { it.name.startsWith("c:") } }
    val timeLabels = remember(labels) { labels.filter { it.name.startsWith("t:") } }
    val otherLabels = remember(labels) {
        labels.filter {
            !it.name.startsWith("s:") && !it.name.startsWith("p:") &&
                    !it.name.startsWith("c:") && !it.name.startsWith("t:") &&
                    it.name != "mobile-entry" && it.name != "routine"
        }
    }

    fun toggleLabel(name: String) {
        selectedLabels = if (selectedLabels.contains(name)) {
            selectedLabels - name
        } else {
            selectedLabels + name
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxHeight(0.85f)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Filter Tasks", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = {
                    onReset()
                    onDismiss()
                }) {
                    Text("Reset")
                }
            }

            HorizontalDivider()

            LazyColumn(
                modifier = Modifier.weight(1f).padding(vertical = 16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = dateQuery,
                        onValueChange = { dateQuery = it },
                        label = { Text("Date (e.g. 2024-02)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item { FilterLabelGroup("Status", statusLabels, selectedLabels, ::toggleLabel, "s:") }
                item { FilterLabelGroup("Priority", priorityLabels, selectedLabels, ::toggleLabel, "p:") }
                item { FilterLabelGroup("Context", contextLabels, selectedLabels, ::toggleLabel, "c:") }
                item { FilterLabelGroup("Time", timeLabels, selectedLabels, ::toggleLabel, "t:") }
                item { FilterLabelGroup("Other", otherLabels, selectedLabels, ::toggleLabel, "") }
            }

            Button(
                onClick = {
                    onApply(TaskFilterCriteria(selectedLabels, dateQuery))
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
            ) {
                Text("Apply Filters")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterLabelGroup(
    title: String,
    labels: List<Label>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    prefixToRemove: String
) {
    if (labels.isEmpty()) return

    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(androidx.compose.foundation.rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEach { label ->
            val isSelected = selected.contains(label.name)
            val displayName = if (prefixToRemove.isNotEmpty()) label.name.removePrefix(prefixToRemove) else label.name

            FilterChip(
                selected = isSelected,
                onClick = { onToggle(label.name) },
                label = { Text(displayName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportFilterBottomSheet(
    currentQuery: String,
    onApply: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf(currentQuery) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
            Text("Filter Reports", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Year-Month (e.g. 2026-02)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = {
                    onReset()
                    onDismiss()
                }) { Text("Reset") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = {
                    onApply(query)
                    onDismiss()
                }) { Text("Apply") }
            }
        }
    }
}