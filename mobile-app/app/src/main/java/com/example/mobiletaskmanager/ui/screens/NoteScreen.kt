package com.example.mobiletaskmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.mobiletaskmanager.data.model.Issue
import com.example.mobiletaskmanager.ui.MainUiState
import com.example.mobiletaskmanager.ui.theme.*
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteScreen(
    uiState: MainUiState,
    onAddNote: (String) -> Unit,
    onRefresh: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    // 投稿中のローカル状態（API通信中の連打防止とフィードバック用）
    var isPosting by remember { mutableStateOf(false) }

    val pullToRefreshState = rememberPullToRefreshState()
    val focusManager = LocalFocusManager.current

    // 初回ロード
    LaunchedEffect(Unit) {
        if (uiState.notes.isEmpty() && !uiState.isLoading) {
            onRefresh()
        }
    }

    // uiState.isRefreshing が false に戻ったら投稿完了とみなしてローカルのローディングを解除
    LaunchedEffect(uiState.isRefreshing) {
        if (!uiState.isRefreshing) {
            isPosting = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        // Header
        Surface(color = SurfaceColor, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Microblog / Notes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(16.dp)
            )
        }
        HorizontalDivider(color = DividerColor)

        // Timeline with PullToRefresh
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            state = pullToRefreshState,
            modifier = Modifier.weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                // itemsにkeyを指定して描画効率とアニメーションを最適化
                content = {
                    items(uiState.notes, key = { it.number }) { note ->
                        NoteItem(note)
                    }
                }
            )
        }

        // Input Area
        Surface(
            color = SurfaceColor,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("What's on your mind?") },
                    modifier = Modifier.weight(1f),
                    maxLines = 4,
                    enabled = !isPosting, // 投稿中は入力を無効化
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (text.isNotBlank() && !isPosting) {
                            isPosting = true
                            onAddNote(text)
                            text = ""
                            focusManager.clearFocus()
                        }
                    })
                )

                Spacer(modifier = Modifier.width(8.dp))

                // 送信ボタンまたはローディングインジケータ
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPosting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = PrimaryAccent,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = {
                                if (text.isNotBlank()) {
                                    isPosting = true
                                    onAddNote(text)
                                    text = ""
                                    focusManager.clearFocus()
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (text.isNotBlank()) PrimaryAccent else Color.Gray.copy(alpha = 0.5f),
                                    androidx.compose.foundation.shape.CircleShape
                                ),
                            enabled = text.isNotBlank()
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Post", tint = SurfaceColor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoteItem(note: Issue) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = note.title,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatTime(note.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

fun formatTime(isoString: String): String {
    return try {
        val instant = java.time.Instant.parse(isoString)
        val zonedDateTime = instant.atZone(ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("MM/dd HH:mm")
        zonedDateTime.format(formatter)
    } catch (e: Exception) {
        isoString
    }
}