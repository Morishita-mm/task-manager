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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox // 追加
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState // 追加
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobiletaskmanager.data.model.RepoContent
import com.example.mobiletaskmanager.ui.MainUiState
import com.example.mobiletaskmanager.ui.theme.*
import dev.jeziellago.compose.markdowntext.MarkdownText

@OptIn(ExperimentalMaterial3Api::class) // PullToRefresh用
@Composable
fun ReportScreen(
    uiState: MainUiState,
    onLoadReports: () -> Unit,
    onSelectReport: (String) -> Unit,
    onBackToList: () -> Unit
) {
    LaunchedEffect(Unit) {
        if (uiState.reportFiles.isEmpty()) {
            onLoadReports()
        }
    }

    val pullToRefreshState = rememberPullToRefreshState()

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
            // ▼▼▼ 更新機能の追加 ▼▼▼
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onLoadReports,
                state = pullToRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                ReportListView(
                    reports = uiState.reportFiles,
                    onSelect = { report -> onSelectReport(report.path) }
                )
            }
            // ▲▲▲ 追加ここまで ▲▲▲
        }
    }
}

@Composable
fun ReportListView(
    reports: List<RepoContent>,
    onSelect: (RepoContent) -> Unit
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Text(
                "Daily Reports",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                modifier = Modifier.padding(16.dp),
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider(color = DividerColor)
        }

        items(reports) { report ->
            if (report.type == "file" && report.name.endsWith(".md")) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(report) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = PrimaryAccent)
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
}

@Composable
fun ReportDetailView(
    content: String,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // ヘッダー (戻るボタン)
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

        // コンテンツ (Markdown表示)
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
                // 必要に応じてリンクの色などもカスタマイズできます
                // linkColor = PrimaryAccent,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}