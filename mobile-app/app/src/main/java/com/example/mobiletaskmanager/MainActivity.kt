package com.example.mobiletaskmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobiletaskmanager.data.api.GithubApiService
import com.example.mobiletaskmanager.data.model.Issue
import com.example.mobiletaskmanager.data.model.Label
import com.example.mobiletaskmanager.data.repository.GithubRepository
import com.example.mobiletaskmanager.ui.GuidelineScreen
import com.example.mobiletaskmanager.ui.MainUiState
import com.example.mobiletaskmanager.ui.MainViewModel
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

// --- 🎨 Modern Dark Theme Palette ---
val AppBackground = Color(0xFF121212)       // Material Design推奨のダーク背景
val SurfaceColor = Color(0xFF1E1E1E)        // カード、モーダル、AppBarの背景
val PrimaryAccent = Color(0xFF81C784)       // GitHubライクで目に優しいパステルグリーン
val TextPrimary = Color(0xFFE6E6E6)         // メイン文字色 (白すぎない白)
val TextSecondary = Color(0xFFAAAAAA)       // 補足文字色
val DividerColor = Color(0xFF333333)        // 区切り線

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // DI Setup
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

        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(repository) as T
            }
        }

        setContent {
            // アプリ全体のテーマ適用
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = AppBackground,
                    surface = SurfaceColor,
                    primary = PrimaryAccent,
                    onBackground = TextPrimary,
                    onSurface = TextPrimary
                )
            ) {
                val viewModel: MainViewModel = viewModel(factory = viewModelFactory)
                val uiState by viewModel.uiState.collectAsState()

                AppNavigation(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }
}

enum class Screen(val title: String, val icon: ImageVector) {
    Tasks("Tasks", Icons.Default.List),
    Guidelines("Guidelines", Icons.Default.Info)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(
    uiState: MainUiState,
    viewModel: MainViewModel
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf(Screen.Tasks) }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = SurfaceColor,
                drawerContentColor = TextPrimary
            ) {
                Spacer(Modifier.height(12.dp))
                Text("Task Manager", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                HorizontalDivider(color = DividerColor)

                Screen.values().forEach { screen ->
                    NavigationDrawerItem(
                        icon = { Icon(screen.icon, contentDescription = null, tint = if(screen == currentScreen) PrimaryAccent else TextSecondary) },
                        label = { Text(screen.title, color = if(screen == currentScreen) PrimaryAccent else TextPrimary) },
                        selected = screen == currentScreen,
                        onClick = {
                            currentScreen = screen
                            scope.launch { drawerState.close() }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = PrimaryAccent.copy(alpha = 0.1f),
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            containerColor = AppBackground,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            currentScreen.title,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = TextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = SurfaceColor
                    )
                )
            }
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                when (currentScreen) {
                    Screen.Tasks -> TaskScreenContent(
                        uiState = uiState,
                        onCloseTask = viewModel::closeIssue,
                        onRefresh = viewModel::refresh,
                        onAddOneOff = viewModel::addOneOffTask,
                        onAddRoutine = viewModel::addRoutineTask
                    )
                    Screen.Guidelines -> GuidelineScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreenContent(
    uiState: MainUiState,
    onCloseTask: (Issue) -> Unit,
    onRefresh: () -> Unit,
    onAddOneOff: (String, List<Label>) -> Unit,
    onAddRoutine: (String, String, List<Label>) -> Unit
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val pullToRefreshState = rememberPullToRefreshState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBackground)
        ) {
            // --- Header ---
            Surface(
                color = SurfaceColor,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = uiState.statusMessage,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(16.dp)
                )
            }

            HorizontalDivider(color = DividerColor, thickness = 1.dp)

            // --- List ---
            PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                state = pullToRefreshState,
                modifier = Modifier.weight(1f)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(uiState.issues) { issue ->
                        TaskRow(issue = issue, onClose = { onCloseTask(it) })
                        HorizontalDivider(color = DividerColor, thickness = 1.dp)
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { showBottomSheet = true },
            containerColor = PrimaryAccent, // アクセントカラーを使用
            contentColor = SurfaceColor,    // アイコンは暗い色でコントラスト確保
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(56.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Task", modifier = Modifier.size(28.dp))
        }

        // Bottom Sheet
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = SurfaceColor, // ▼ 修正: モーダル背景をダークに
                contentColor = TextPrimary     // ▼ 修正: コンテンツ色を白系に
            ) {
                AddTaskSheetContent(
                    availableLabels = uiState.labels,
                    onCancel = { showBottomSheet = false },
                    onSubmit = { title, isRoutine, selectedDays, selectedLabels ->
                        if (isRoutine) {
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

fun String.toColor(): Color {
    return try {
        Color(android.graphics.Color.parseColor("#$this"))
    } catch (e: Exception) {
        Color.Gray
    }
}

@Composable
fun TaskRow(issue: Issue, onClose: (Issue) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "#${issue.number} ${issue.title}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (issue.labels.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    issue.labels.forEach { label ->
                        if (label.name == "mobile-entry") return@forEach

                        // GitHubラベルの色をそのまま使用
                        val bgColor = label.color.toColor()

                        Surface(
                            color = bgColor,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                                Text(
                                    text = label.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White, // ラベル内文字は白固定で視認性確保
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        // ▼ 修正: チェックボタンのデザイン変更
        IconButton(
            onClick = { onClose(issue) },
            modifier = Modifier
                .size(36.dp)
                .background(PrimaryAccent.copy(alpha = 0.2f), CircleShape) // 薄いグリーンの背景
        ) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Done",
                tint = PrimaryAccent, // グリーンのチェックマーク
                modifier = Modifier.size(20.dp)
            )
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

    Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = TextPrimary) // 色修正
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
                    selectedContainerColor = label.color.toColor().copy(alpha = 0.6f),
                    selectedLabelColor = Color.White,
                    containerColor = SurfaceColor, // 未選択時の背景
                    labelColor = TextPrimary       // 未選択時の文字
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = DividerColor,
                    enabled = true,
                    selected = isSelected
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

    // --- Label Logic ---
    val priorityLabels = remember(availableLabels) { availableLabels.filter { it.name.startsWith("p:") } }
    val contextLabels = remember(availableLabels) { availableLabels.filter { it.name.startsWith("c:") } }
    val timeLabels = remember(availableLabels) { availableLabels.filter { it.name.startsWith("t:") } }
    val otherLabels = remember(availableLabels) {
        availableLabels.filter {
            !it.name.startsWith("p:") && !it.name.startsWith("c:") && !it.name.startsWith("t:") &&
                    !it.name.startsWith("s:") && it.name != "mobile-entry" && it.name != "routine"
        }
    }

    val dayScrollState = rememberScrollState()
    val contentScrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.6f)
            .padding(24.dp)
            .navigationBarsPadding()
            .verticalScroll(contentScrollState)
    ) {
        // タイトル色を修正
        Text("New Task", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Spacer(modifier = Modifier.height(16.dp))

        // テキストフィールドをダークモード対応に
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Task Title") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryAccent,
                unfocusedBorderColor = TextSecondary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedLabelColor = PrimaryAccent,
                unfocusedLabelColor = TextSecondary,
                cursorColor = PrimaryAccent
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        LabelSelectionSection("Priority", priorityLabels, selectedLabels, { l -> selectedLabels = if (selectedLabels.contains(l)) selectedLabels - l else selectedLabels + l }, "p:")
        LabelSelectionSection("Context", contextLabels, selectedLabels, { l -> selectedLabels = if (selectedLabels.contains(l)) selectedLabels - l else selectedLabels + l }, "c:")
        LabelSelectionSection("Time", timeLabels, selectedLabels, { l -> selectedLabels = if (selectedLabels.contains(l)) selectedLabels - l else selectedLabels + l }, "t:")
        LabelSelectionSection("Other Labels", otherLabels, selectedLabels, { l -> selectedLabels = if (selectedLabels.contains(l)) selectedLabels - l else selectedLabels + l })

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = DividerColor)
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "Repeat Task (Routine)",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary // 色修正
            )
            Switch(
                checked = isRoutine,
                onCheckedChange = { isRoutine = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = SurfaceColor,
                    checkedTrackColor = PrimaryAccent,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = AppBackground
                )
            )
        }

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
                            onClick = { selectedDays = if (selected) selectedDays - day else selectedDays + day },
                            label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryAccent,
                                selectedLabelColor = SurfaceColor,
                                containerColor = SurfaceColor,
                                labelColor = TextPrimary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (selected) PrimaryAccent else DividerColor,
                                selected = selected,
                                enabled = true
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            TextButton(
                onClick = onCancel,
                colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)
            ) {
                Text("Cancel")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onSubmit(title, isRoutine, selectedDays, selectedLabels) },
                enabled = title.isNotBlank() && (!isRoutine || selectedDays.isNotEmpty()),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryAccent,
                    contentColor = SurfaceColor,
                    disabledContainerColor = DividerColor,
                    disabledContentColor = TextSecondary
                )
            ) {
                Text(if (isRoutine) "Add Routine" else "Add Task")
            }
        }
    }
}