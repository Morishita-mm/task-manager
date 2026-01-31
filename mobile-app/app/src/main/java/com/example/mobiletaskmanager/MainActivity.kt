package com.example.mobiletaskmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobiletaskmanager.data.api.GithubApiService
import com.example.mobiletaskmanager.data.model.Issue
import com.example.mobiletaskmanager.data.model.Label
import com.example.mobiletaskmanager.data.repository.GithubRepository
import com.example.mobiletaskmanager.ui.MainUiState
import com.example.mobiletaskmanager.ui.MainViewModel
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 依存関係の構築 (本来は Hilt などの DI コンテナで行う作業)
        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory(contentType))
            .build()
        val apiService = retrofit.create(GithubApiService::class.java)

        val repository = GithubRepository(
            api = apiService,
            token = BuildConfig.GITHUB_TOKEN,
            owner = BuildConfig.GITHUB_OWNER,
            repo = BuildConfig.GITHUB_REPO
        )

        // ViewModel Factory の作成 (引数付き ViewModel のため必要)
        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(repository) as T
            }
        }

        setContent {
            // 2. ViewModel の取得
            val viewModel: MainViewModel = viewModel(factory = viewModelFactory)

            // 3. StateFlow を Compose の State として監視
            val uiState by viewModel.uiState.collectAsState()

            // 4. UI の描画
            TaskScreen(
                uiState = uiState,
                onCloseTask = viewModel::closeIssue,
                onAddOneOff = viewModel::addOneOffTask,
                onAddRoutine = viewModel::addRoutineTask
            )
        }
    }
}

// --- Main Screen Composable ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    uiState: MainUiState,
    onCloseTask: (Issue) -> Unit,
    onAddOneOff: (String, List<Label>) -> Unit,
    onAddRoutine: (String, String, List<Label>) -> Unit
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    // シートの高さを安定させる設定
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showBottomSheet = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add Task")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text(
                text = uiState.statusMessage,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // ローディングインジケータ (簡易実装)
            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
            }

            LazyColumn {
                items(uiState.issues) { issue ->
                    TaskRow(issue = issue, onClose = { onCloseTask(it) })
                    Divider()
                }
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState
            ) {
                AddTaskSheetContent(
                    availableLabels = uiState.labels,
                    onCancel = { showBottomSheet = false },
                    onSubmit = { title, isRoutine, selectedDays, selectedLabels ->
                        if (isRoutine) {
                            // 曜日リストを YAML 形式の文字列に変換
                            val schedule = if (selectedDays.size == 7) "daily" else {
                                val daysStr = selectedDays.joinToString(",") {
                                    it.getDisplayName(TextStyle.SHORT, Locale.ENGLISH).lowercase()
                                }
                                "weekly:$daysStr"
                            }
                            onAddRoutine(title, schedule, selectedLabels)
                        } else {
                            onAddOneOff(title, selectedLabels)
                        }
                        showBottomSheet = false
                    }
                )
            }
        }
    }
}

// --- UI Components ---

// GitHubの色コード変換拡張関数
fun String.toColor(): Color {
    return try {
        Color(android.graphics.Color.parseColor("#$this"))
    } catch (e: Exception) {
        Color.Gray
    }
}

@Composable
fun TaskRow(issue: Issue, onClose: (Issue) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "#${issue.number} ${issue.title}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { onClose(issue) }) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Done",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (issue.labels.isNotEmpty()) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                issue.labels.forEach { label ->
                    if (label.name == "mobile-entry") return@forEach
                    AssistChip(
                        onClick = {},
                        label = { Text(label.name, style = MaterialTheme.typography.labelSmall) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = label.color.toColor().copy(alpha = 0.2f),
                            labelColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = null,
                        modifier = Modifier.height(24.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelSelectionSection(
    title: String,
    labels: List<Label>,
    selectedLabels: List<Label>,
    onLabelToggle: (Label) -> Unit,
    prefixToRemove: String = ""
) {
    if (labels.isEmpty()) return

    Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(4.dp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEach { label ->
            val isSelected = selectedLabels.contains(label)
            val displayName = if (prefixToRemove.isNotEmpty() && label.name.startsWith(prefixToRemove)) {
                label.name.removePrefix(prefixToRemove)
            } else {
                label.name
            }

            FilterChip(
                selected = isSelected,
                onClick = { onLabelToggle(label) },
                label = { Text(displayName) },
                leadingIcon = if (isSelected) {
                    {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = label.color.toColor().copy(alpha = 0.4f)
                )
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskSheetContent(
    availableLabels: List<Label>,
    onCancel: () -> Unit,
    onSubmit: (String, Boolean, Set<DayOfWeek>, List<Label>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var isRoutine by remember { mutableStateOf(false) }
    var selectedDays by remember { mutableStateOf(emptySet<DayOfWeek>()) }
    var selectedLabels by remember { mutableStateOf(emptyList<Label>()) }

    // ラベルのフィルタリング
    val priorityLabels = remember(availableLabels) { availableLabels.filter { it.name.startsWith("p:") } }
    val contextLabels = remember(availableLabels) { availableLabels.filter { it.name.startsWith("c:") } }
    val otherLabels = remember(availableLabels) {
        availableLabels.filter {
            !it.name.startsWith("p:") && !it.name.startsWith("c:") && !it.name.startsWith("s:") && it.name != "mobile-entry"
        }
    }

    val dayScrollState = rememberScrollState()
    val contentScrollState = rememberScrollState()

    // 画面の60%の高さを確保して、トグル操作でのガタつきを防ぐ
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.6f)
            .padding(24.dp)
            .navigationBarsPadding()
            .verticalScroll(contentScrollState)
    ) {

        Text("New Task", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // Title Input
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Task Title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Label Sections
        LabelSelectionSection(
            "Priority",
            priorityLabels,
            selectedLabels,
            { l -> selectedLabels = if (selectedLabels.contains(l)) selectedLabels - l else selectedLabels + l },
            "p:"
        )
        LabelSelectionSection(
            "Context",
            contextLabels,
            selectedLabels,
            { l -> selectedLabels = if (selectedLabels.contains(l)) selectedLabels - l else selectedLabels + l },
            "c:"
        )
        LabelSelectionSection(
            "Other Labels",
            otherLabels,
            selectedLabels,
            { l -> selectedLabels = if (selectedLabels.contains(l)) selectedLabels - l else selectedLabels + l }
        )

        Spacer(modifier = Modifier.height(8.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        // Routine Switch
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Repeat Task (Routine)",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(checked = isRoutine, onCheckedChange = { isRoutine = it })
        }

        // Animated Day Selection
        AnimatedVisibility(
            visible = isRoutine,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(dayScrollState),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DayOfWeek.values().forEach { day ->
                        val selected = selectedDays.contains(day)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedDays =
                                    if (selected) selectedDays - day else selectedDays + day
                            },
                            label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Action Buttons
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onCancel) { Text("Cancel") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onSubmit(title, isRoutine, selectedDays, selectedLabels) },
                enabled = title.isNotBlank() && (!isRoutine || selectedDays.isNotEmpty())
            ) {
                Text(if (isRoutine) "Add Routine" else "Add Task")
            }
        }
    }
}