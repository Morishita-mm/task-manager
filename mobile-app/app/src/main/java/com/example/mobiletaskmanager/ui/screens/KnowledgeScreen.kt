package com.example.mobiletaskmanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import com.example.mobiletaskmanager.data.model.RepoContent
import com.example.mobiletaskmanager.ui.MainUiState
import com.example.mobiletaskmanager.ui.theme.*

@Composable
fun KnowledgeScreen(
    uiState: MainUiState,
    onLoad: () -> Unit,
    onSelect: (RepoContent) -> Unit,
    onCreate: () -> Unit,
    onCloseEditor: () -> Unit,
    onSave: (String, String) -> Unit
) {
    // 画面表示時にロード
    LaunchedEffect(Unit) {
        onLoad()
    }

    if (uiState.editingKnowledge != null) {
        // エディタモード
        KnowledgeEditor(
            initialName = uiState.editingKnowledge.name,
            initialContent = uiState.editingKnowledge.content,
            isNewFile = uiState.editingKnowledge.sha == null,
            statusMessage = uiState.statusMessage,
            isLoading = uiState.isLoading,
            onBack = onCloseEditor,
            onSave = onSave
        )
    } else {
        // 一覧モード
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(AppBackground)
            ) {
                Surface(color = SurfaceColor, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Knowledge Base",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                HorizontalDivider(color = DividerColor)

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(uiState.knowledgeFiles) { file ->
                        KnowledgeRow(file = file, onClick = { onSelect(file) })
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                    }
                    // ファイルがない場合の表示
                    if (uiState.knowledgeFiles.isEmpty() && !uiState.isLoading) {
                        item {
                            Text(
                                "No knowledge files yet.",
                                modifier = Modifier.padding(16.dp),
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // 新規作成ボタン
            FloatingActionButton(
                onClick = onCreate,
                containerColor = PrimaryAccent,
                contentColor = SurfaceColor,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Knowledge")
            }
        }
    }
}

@Composable
fun KnowledgeRow(file: RepoContent, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Description, contentDescription = null, tint = TextSecondary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = file.name,
            style = MaterialTheme.typography.bodyLarge,
            color = TextPrimary
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeEditor(
    initialName: String,
    initialContent: String,
    isNewFile: Boolean,
    statusMessage: String,
    isLoading: Boolean,
    onBack: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var content by remember { mutableStateOf(initialContent) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        // Toolbar
        Surface(color = SurfaceColor, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Text(
                    if (isNewFile) "New Knowledge" else "Edit Knowledge",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (isLoading) {
                    // ロード中はインジケーターを表示
                    Box(modifier = Modifier.padding(12.dp).size(24.dp)) {
                        CircularProgressIndicator(strokeWidth = 2.dp, color = PrimaryAccent)
                    }
                } else {
                    // 通常時は保存ボタン
                    IconButton(onClick = { onSave(name, content) }) {
                        Icon(Icons.Default.Save, contentDescription = "Save", tint = PrimaryAccent)
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            // ファイル名入力 (新規作成時のみ編集可能、または既存なら表示のみ)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Filename (e.g. docker-commands.md)") },
                modifier = Modifier.fillMaxWidth(),
                enabled = isNewFile, // 既存ファイルの名前変更は今回はサポート外（複雑になるため）
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 本文エディタ
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                label = { Text("Content (Markdown)") },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                // 上寄せにするための設定
                textStyle = LocalTextStyle.current.copy(lineHeight = 24.sp)            )
        }
    }
}