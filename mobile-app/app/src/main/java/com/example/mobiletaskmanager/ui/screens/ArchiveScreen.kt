package com.example.mobiletaskmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.mobiletaskmanager.data.model.Issue
import com.example.mobiletaskmanager.ui.MainUiState
import com.example.mobiletaskmanager.ui.MainViewModel
import com.example.mobiletaskmanager.ui.components.*
import com.example.mobiletaskmanager.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onLoadArchive: () -> Unit
) {
    var showFilterSheet by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        onLoadArchive()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        // Header Row
        Surface(color = SurfaceColor, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Archive (${uiState.filteredClosedIssues.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                FilterSortButtons(
                    onFilterClick = { showFilterSheet = true },
                    onSortClick = { showSortSheet = true },
                    activeFilterCount = uiState.archiveFilterCriteria.selectedLabels.size +
                            (if (uiState.archiveFilterCriteria.dateQuery.isNotEmpty()) 1 else 0)
                )
            }
        }

        HorizontalDivider(color = DividerColor, thickness = 1.dp)

        if (uiState.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = PrimaryAccent)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(uiState.filteredClosedIssues) { issue ->
                ArchiveRow(issue = issue)
                HorizontalDivider(color = DividerColor, thickness = 1.dp)
            }
        }
    }

    // Sheets
    if (showSortSheet) {
        SortBottomSheet(
            currentSort = uiState.archiveSortOption,
            onSortSelected = viewModel::setArchiveSort,
            onDismiss = { showSortSheet = false }
        )
    }
    if (showFilterSheet) {
        TaskFilterBottomSheet(
            labels = uiState.labels,
            currentFilter = uiState.archiveFilterCriteria,
            onApply = viewModel::setArchiveFilter,
            onReset = { viewModel.setArchiveFilter(TaskFilterCriteria()) },
            onDismiss = { showFilterSheet = false }
        )
    }
}

@Composable
fun ArchiveRow(issue: Issue) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = issue.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Normal,
                color = TextSecondary,
                textDecoration = TextDecoration.LineThrough,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (issue.labels.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    issue.labels.forEach { label ->
                        if (label.name == "mobile-entry") return@forEach
                        val bgColor = label.color.toColor().copy(alpha = 0.3f)
                        Surface(
                            color = bgColor,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                                Text(
                                    text = label.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Closed",
            tint = TextSecondary,
            modifier = Modifier.size(24.dp)
        )
    }
}