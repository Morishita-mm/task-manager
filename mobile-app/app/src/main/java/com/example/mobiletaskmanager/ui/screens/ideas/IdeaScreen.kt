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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.mobiletaskmanager.data.model.Issue
import com.example.mobiletaskmanager.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeaScreen(uiState: IdeaUiState, viewModel: IdeaViewModel) {
    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        val filteredIdeas = if (uiState.showClosed) {
            uiState.ideas
        } else {
            uiState.ideas.filter { it.idea.state == "open" }
        }

        if (uiState.isLoading && uiState.ideas.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = PrimaryAccent) }
        } else if (uiState.error != null && uiState.ideas.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text(text = "Error: ${uiState.error}", color = MaterialTheme.colorScheme.error) }
        } else if (uiState.selectedIdea != null) {
            IdeaDetailScreen(
                uiState = uiState,
                ideaWithFeatures = uiState.selectedIdea!!,
                onRefresh = { viewModel.loadIdeas(isRefresh = true) },
                onToggleClosed = { viewModel.toggleShowClosed() },
                onAddFeature = { title -> viewModel.addFeature(uiState.selectedIdea!!.idea, title) },
                onCloseIdea = { issue -> viewModel.closeIssueAndSubIssues(issue) },
                onCloseFeature = { feature -> viewModel.closeFeature(feature) },
                onExport = { viewModel.exportIdeaToMarkdown(it) },
                onBack = { viewModel.selectIdea(null) }
            )
        } else {
            IdeaListScreen(
                uiState = uiState,
                ideas = filteredIdeas,
                onToggleClosed = { viewModel.toggleShowClosed() },
                onRefresh = { viewModel.loadIdeas(isRefresh = true) },
                onAddIdea = { title, body -> viewModel.addIdea(title, body) },
                onIdeaClick = { idea -> viewModel.selectIdea(idea) },
                onCloseIdea = { issue -> viewModel.closeIssueAndSubIssues(issue) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeaDetailScreen(
    uiState: IdeaUiState,
    ideaWithFeatures: IdeaWithFeatures,
    onToggleClosed: () -> Unit,
    onRefresh: () -> Unit,
    onAddFeature: (String) -> Unit,
    onCloseIdea: (Issue) -> Unit,
    onCloseFeature: (Issue) -> Unit,
    onExport: (IdeaWithFeatures) -> Unit,
    onBack: () -> Unit
) {
    var showAddFeatureDialog by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    val isIdeaOpen = ideaWithFeatures.idea.state == "open"

    val displayedFeatures = if (uiState.showClosed) {
        ideaWithFeatures.features
    } else {
        ideaWithFeatures.features.filter { it.state == "open" }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(color = SurfaceColor, modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary) }
                Text(
                    text = ideaWithFeatures.idea.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onToggleClosed) {
                    Icon(
                        if (uiState.showClosed) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Closed Features",
                        tint = if (uiState.showClosed) PrimaryAccent else TextSecondary
                    )
                }
                IconButton(onClick = { onExport(ideaWithFeatures) }) {
                    Icon(Icons.Default.Description, contentDescription = "Export to Markdown", tint = PrimaryAccent)
                }
                if (uiState.isLoading && !uiState.isRefreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(4.dp), strokeWidth = 2.dp, color = PrimaryAccent)
                }
            }
        }
        HorizontalDivider(color = DividerColor)
        Box(modifier = Modifier.fillMaxSize()) {
            PullToRefreshBox(isRefreshing = uiState.isRefreshing, onRefresh = onRefresh, state = pullToRefreshState, modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
                    Text(text = "Description", style = MaterialTheme.typography.titleSmall, color = PrimaryAccent, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = ideaWithFeatures.idea.body ?: "No description.", style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = "Features", style = MaterialTheme.typography.titleSmall, color = PrimaryAccent, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))

                    displayedFeatures.forEach { feature ->
                        val isClosed = feature.state == "closed"
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, DividerColor)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = feature.title,
                                    modifier = Modifier.weight(1f),
                                    color = if (isClosed) TextSecondary else TextPrimary,
                                    textDecoration = if (isClosed) TextDecoration.LineThrough else null
                                )
                                IconButton(onClick = { onCloseFeature(feature) }, enabled = !isClosed) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Close Feature",
                                        tint = if (isClosed) Color.Transparent else TextSecondary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    if (ideaWithFeatures.idea.state == "open") {
                        Button(onClick = { onCloseIdea(ideaWithFeatures.idea) }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent.copy(alpha = 0.6f)), shape = RoundedCornerShape(12.dp)) { Text("Close Idea and Features", color = Color.White) }
                    }
                    Spacer(modifier = Modifier.height(88.dp))
                }
            }
            if (isIdeaOpen) {
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
    }
    if (showAddFeatureDialog) { AddFeatureDialog(onAddFeature = { title -> onAddFeature(title); showAddFeatureDialog = false }, onDismiss = { showAddFeatureDialog = false }) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IdeaListScreen(
    uiState: IdeaUiState,
    ideas: List<IdeaWithFeatures>,
    onToggleClosed: () -> Unit,
    onRefresh: () -> Unit,
    onAddIdea: (String, String) -> Unit,
    onIdeaClick: (IdeaWithFeatures) -> Unit,
    onCloseIdea: (Issue) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Surface(color = SurfaceColor, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (uiState.showClosed) "All Ideas" else "Active Ideas: ${ideas.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f).padding(16.dp)
                    )
                    IconButton(onClick = onToggleClosed) {
                        Icon(
                            if (uiState.showClosed) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Closed",
                            tint = if (uiState.showClosed) PrimaryAccent else TextSecondary
                        )
                    }
                }
            }
            HorizontalDivider(color = DividerColor)
            PullToRefreshBox(isRefreshing = uiState.isRefreshing, onRefresh = onRefresh, state = pullToRefreshState, modifier = Modifier.weight(1f)) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(ideas) { ideaWithFeatures ->
                        IdeaItem(ideaWithFeatures = ideaWithFeatures, onClick = { onIdeaClick(ideaWithFeatures) }, onClose = { onCloseIdea(ideaWithFeatures.idea) })
                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
                    }
                }
            }
        }
        FloatingActionButton(onClick = { showAddDialog = true }, containerColor = PrimaryAccent, contentColor = SurfaceColor, shape = RoundedCornerShape(16.dp), modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) { Icon(Icons.Default.Add, contentDescription = "Add Idea") }
        if (showAddDialog) { AddIdeaDialog(onAddIdea = { title, body -> onAddIdea(title, body); showAddDialog = false }, onDismiss = { showAddDialog = false }) }
    }
}

@Composable
fun IdeaItem(ideaWithFeatures: IdeaWithFeatures, onClick: () -> Unit, onClose: () -> Unit) {
    val isClosed = ideaWithFeatures.idea.state == "closed"
    Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), color = Color.Transparent) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = ideaWithFeatures.idea.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isClosed) TextSecondary else TextPrimary,
                    textDecoration = if (isClosed) TextDecoration.LineThrough else null
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Features: ${ideaWithFeatures.features.size}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
            if (!isClosed) {
                IconButton(onClick = onClose) { Icon(Icons.Default.Delete, contentDescription = "Close Idea", tint = TextSecondary) }
            }
        }
    }
}

@Composable
fun AddIdeaDialog(onAddIdea: (String, String) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add New Idea", color = TextPrimary) }, text = { Column { OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true); Spacer(modifier = Modifier.height(12.dp)); OutlinedTextField(value = body, onValueChange = { body = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), minLines = 3) } }, confirmButton = { TextButton(onClick = { onAddIdea(title, body) }, enabled = title.isNotBlank()) { Text("Add", color = PrimaryAccent) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }, containerColor = SurfaceColor)
}

@Composable
fun AddFeatureDialog(onAddFeature: (String) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Add New Feature", color = TextPrimary) }, text = { OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true) }, confirmButton = { TextButton(onClick = { onAddFeature(title) }, enabled = title.isNotBlank()) { Text("Add", color = PrimaryAccent) } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = TextSecondary) } }, containerColor = SurfaceColor)
}