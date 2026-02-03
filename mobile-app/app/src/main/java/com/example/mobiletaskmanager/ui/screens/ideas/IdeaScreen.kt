package com.example.mobiletaskmanager.ui.screens.ideas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mobiletaskmanager.data.model.Issue
import com.example.mobiletaskmanager.ui.theme.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeaScreen(
    uiState: IdeaUiState,
    viewModel: IdeaViewModel
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        if (uiState.isLoading && uiState.ideas.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryAccent)
            }
        } else if (uiState.error != null && uiState.ideas.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
            }
        } else if (uiState.selectedIdea != null) {
            IdeaDetailScreen(
                uiState = uiState,
                ideaWithFeatures = uiState.selectedIdea!!,
                onRefresh = { viewModel.loadIdeas(isRefresh = true) }, // リフレッシュ機能を追加
                onAddFeature = { title -> viewModel.addFeature(uiState.selectedIdea!!.idea, title) },
                onCloseIssue = { issue -> viewModel.closeIssueAndSubIssues(issue) },
                onBack = { viewModel.selectIdea(null) }
            )
        } else {
            IdeaListScreen(
                uiState = uiState,
                ideas = uiState.ideas,
                onRefresh = { viewModel.loadIdeas(isRefresh = true) },
                onAddIdea = { title, body -> viewModel.addIdea(title, body) },
                onIdeaClick = { idea -> viewModel.selectIdea(idea) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeaListScreen(
    uiState: IdeaUiState,
    ideas: List<IdeaWithFeatures>,
    onRefresh: () -> Unit,
    onAddIdea: (String, String) -> Unit,
    onIdeaClick: (IdeaWithFeatures) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(color = SurfaceColor, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Active Ideas: ${ideas.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(16.dp)
                )
            }
            HorizontalDivider(color = DividerColor)

            // ▼▼▼ リフレッシュ機能の追加 ▼▼▼
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                state = pullToRefreshState,
                modifier = Modifier.weight(1f)
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(ideas) { ideaWithFeatures ->
                        IdeaItem(
                            ideaWithFeatures = ideaWithFeatures,
                            onClick = { onIdeaClick(ideaWithFeatures) }
                        )
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = PrimaryAccent,
            contentColor = SurfaceColor,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Idea")
        }

        if (showAddDialog) {
            AddIdeaDialog(
                onAddIdea = { title, body ->
                    onAddIdea(title, body)
                    showAddDialog = false
                },
                onDismiss = { showAddDialog = false }
            )
        }
    }
}

@Composable
fun IdeaItem(
    ideaWithFeatures: IdeaWithFeatures,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = ideaWithFeatures.idea.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Features: ${ideaWithFeatures.features.size}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeaDetailScreen(
    uiState: IdeaUiState,
    ideaWithFeatures: IdeaWithFeatures,
    onRefresh: () -> Unit, // 追加
    onAddFeature: (String) -> Unit,
    onCloseIssue: (Issue) -> Unit,
    onBack: () -> Unit
) {
    var showAddFeatureDialog by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState() // 状態管理を追加

    Column(modifier = Modifier.fillMaxSize()) {
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
                    text = ideaWithFeatures.idea.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // ローディングインジケーター（リフレッシュ中でない通常のロード時）
                if (uiState.isLoading && !uiState.isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).padding(4.dp),
                        strokeWidth = 2.dp,
                        color = PrimaryAccent
                    )
                }
            }
        }
        HorizontalDivider(color = DividerColor)

        Box(modifier = Modifier.fillMaxSize()) {
            // ▼▼▼ 追加: 詳細画面全体をリフレッシュ可能にする ▼▼▼
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                state = pullToRefreshState,
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Description",
                        style = MaterialTheme.typography.titleSmall,
                        color = PrimaryAccent,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = ideaWithFeatures.idea.body ?: "No description.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Features",
                        style = MaterialTheme.typography.titleSmall,
                        color = PrimaryAccent,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ideaWithFeatures.features.forEach { feature ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, DividerColor)
                        ) {
                            Text(
                                text = feature.title,
                                modifier = Modifier.padding(16.dp),
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { onCloseIssue(ideaWithFeatures.idea) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close Idea and Features", color = Color.White)
                    }

                    Spacer(modifier = Modifier.height(88.dp))
                }
            }

            FloatingActionButton(
                onClick = { showAddFeatureDialog = true },
                containerColor = PrimaryAccent,
                contentColor = SurfaceColor,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Feature")
            }
        }
    }

    if (showAddFeatureDialog) {
        AddFeatureDialog(
            onAddFeature = { title ->
                onAddFeature(title)
                showAddFeatureDialog = false
            },
            onDismiss = { showAddFeatureDialog = false }
        )
    }
}

@Composable
fun AddIdeaDialog(
    onAddIdea: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Idea", color = TextPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAddIdea(title, body) },
                enabled = title.isNotBlank()
            ) {
                Text("Add", color = PrimaryAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceColor
    )
}

@Composable
fun AddFeatureDialog(
    onAddFeature: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Feature", color = TextPrimary) },
        text = {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onAddFeature(title) },
                enabled = title.isNotBlank()
            ) {
                Text("Add", color = PrimaryAccent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        },
        containerColor = SurfaceColor
    )
}