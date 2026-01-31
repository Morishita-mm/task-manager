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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.mobiletaskmanager.data.model.Issue
import com.example.mobiletaskmanager.ui.MainUiState
import com.example.mobiletaskmanager.ui.components.toColor
import com.example.mobiletaskmanager.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    uiState: MainUiState,
    onLoadArchive: () -> Unit
) {
    // 画面表示時にロード
    LaunchedEffect(Unit) {
        onLoadArchive()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        // Header
        Surface(color = SurfaceColor, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Archive (${uiState.closedIssues.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(16.dp)
            )
        }
        HorizontalDivider(color = DividerColor, thickness = 1.dp)

        if (uiState.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = PrimaryAccent)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(uiState.closedIssues) { issue ->
                ArchiveRow(issue = issue)
                HorizontalDivider(color = DividerColor, thickness = 1.dp)
            }
        }
    }
}

@Composable
fun ArchiveRow(issue: Issue) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor.copy(alpha = 0.5f)) // 少し暗くして「完了感」を出す
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "#${issue.number} ${issue.title}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Normal, // 太字をやめる
                color = TextSecondary, // 色を薄くする
                textDecoration = TextDecoration.LineThrough, // 取り消し線を入れる
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (issue.labels.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    issue.labels.forEach { label ->
                        if (label.name == "mobile-entry") return@forEach
                        // ラベルも彩度を落として表示
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

        // 完了済みアイコン (操作不可)
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Closed",
            tint = TextSecondary, // グレーアウト
            modifier = Modifier.size(24.dp)
        )
    }
}