package com.example.mobiletaskmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobiletaskmanager.data.model.RepoContent
import com.example.mobiletaskmanager.ui.MainUiState
import com.example.mobiletaskmanager.ui.MainViewModel
import com.example.mobiletaskmanager.ui.components.*
import com.example.mobiletaskmanager.ui.theme.*
import dev.jeziellago.compose.markdowntext.MarkdownText
import kotlinx.coroutines.delay // 追加
import kotlinx.coroutines.launch // 追加

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onLoadReports: () -> Unit,
    onSelectReport: (String) -> Unit,
    onBackToList: () -> Unit
) {
    var showFilterSheet by remember { mutableStateOf(false) }
    var showSortSheet by remember { mutableStateOf(false) }

    // ▼▼▼ 追加: ローカルでのリフレッシュ状態管理 ▼▼▼
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pullToRefreshState = rememberPullToRefreshState()

    LaunchedEffect(Unit) {
        if (uiState.filteredReports.isEmpty()) {
            onLoadReports()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        if (uiState.selectedReportContent != null) {
            ReportDetailView(
                content = uiState.selectedReportContent,
                onBack = onBackToList
            )
        } else {
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
                        text = "Daily Reports",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    FilterSortButtons(
                        onFilterClick = { showFilterSheet = true },
                        onSortClick = { showSortSheet = true },
                        activeFilterCount = if (uiState.reportFilterQuery.isNotEmpty()) 1 else 0
                    )
                }
            }

            HorizontalDivider(color = DividerColor)

            // ▼▼▼ 修正: ローカルの isRefreshing を使用 ▼▼▼
            PullToRefreshBox(
                isRefreshing = isRefreshing, // UI側で制御する変数に変更
                onRefresh = {
                    isRefreshing = true
                    onLoadReports()
                    // 確実にアニメーションを終了させるために遅延後にフラグを戻す
                    scope.launch {
                        delay(1000)
                        isRefreshing = false
                    }
                },
                state = pullToRefreshState,
                modifier = Modifier.weight(1f)
            ) {
                ReportListView(
                    reports = uiState.filteredReports,
                    onSelect = { report -> onSelectReport(report.path) }
                )
            }
        }
    }

    if (showSortSheet) {
        SortBottomSheet(
            currentSort = uiState.reportSortOption,
            onSortSelected = viewModel::setReportSort,
            onDismiss = { showSortSheet = false }
        )
    }
    if (showFilterSheet) {
        ReportFilterBottomSheet(
            currentQuery = uiState.reportFilterQuery,
            onApply = viewModel::setReportFilter,
            onReset = { viewModel.setReportFilter("") },
            onDismiss = { showFilterSheet = false }
        )
    }
}

// ... ReportListView, ReportDetailView はそのまま ...
@Composable
fun ReportListView(
    reports: List<RepoContent>,
    onSelect: (RepoContent) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        items(reports) { report ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(report) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Description, contentDescription = null, tint = PrimaryAccent)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = report.name.removeSuffix(".md"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary
                )
            }
            HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
        }
    }
}

@Composable
fun ReportDetailView(
    content: String,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = SurfaceColor, modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text("Report Detail", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            MarkdownText(
                markdown = content,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}