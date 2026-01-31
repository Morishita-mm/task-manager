package com.example.mobiletaskmanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobiletaskmanager.data.model.Issue
import com.example.mobiletaskmanager.data.model.Label
import com.example.mobiletaskmanager.data.model.RepoContent
import com.example.mobiletaskmanager.data.repository.GithubRepository
import com.example.mobiletaskmanager.ui.components.SortOption
import com.example.mobiletaskmanager.ui.components.TaskFilterCriteria
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MainUiState(
    // フィルタリング適用後のデータ
    val filteredIssues: List<Issue> = emptyList(),
    val filteredClosedIssues: List<Issue> = emptyList(),
    val filteredReports: List<RepoContent> = emptyList(),

    val labels: List<Label> = emptyList(),
    val statusMessage: String = "Loading...",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val selectedReportContent: String? = null,

    // 現在のフィルタ・ソート状態
    val taskFilterCriteria: TaskFilterCriteria = TaskFilterCriteria(),
    val taskSortOption: SortOption = SortOption.CREATED_DESC,
    val archiveFilterCriteria: TaskFilterCriteria = TaskFilterCriteria(),
    val archiveSortOption: SortOption = SortOption.CREATED_DESC,
    val reportFilterQuery: String = "",
    val reportSortOption: SortOption = SortOption.NAME_DESC
)

class MainViewModel(
    private val repository: GithubRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // 生データ保持用
    private var _rawIssues: List<Issue> = emptyList()
    private var _rawClosedIssues: List<Issue> = emptyList()
    private var _rawReports: List<RepoContent> = emptyList()

    // 状態管理用
    private val _taskFilter = MutableStateFlow(TaskFilterCriteria())
    private val _taskSort = MutableStateFlow(SortOption.CREATED_DESC)
    private val _archiveFilter = MutableStateFlow(TaskFilterCriteria())
    private val _archiveSort = MutableStateFlow(SortOption.CREATED_DESC)
    private val _reportFilterQuery = MutableStateFlow("")
    private val _reportSort = MutableStateFlow(SortOption.NAME_DESC)

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            fetchDataInternal()
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            fetchDataInternal()
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    private suspend fun fetchDataInternal() {
        try {
            val labels = repository.getLabels()
            val issues = repository.getIssues()

            _rawIssues = issues // 生データを保存

            _uiState.value = _uiState.value.copy(
                labels = labels,
                statusMessage = "Active Tasks: ${issues.size}"
            )
            applyFilters() // フィルタ適用
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(statusMessage = "Error: ${e.message}")
        }
    }

    fun loadClosedIssues() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val closed = repository.getClosedIssues()
                _rawClosedIssues = closed
                _uiState.value = _uiState.value.copy(isLoading = false)
                applyFilters()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusMessage = "Failed to load archive: ${e.message}"
                )
            }
        }
    }

    fun loadReports() {
        viewModelScope.launch {
            try {
                val reports = repository.getReportFiles()
                _rawReports = reports
                applyFilters()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(statusMessage = "Failed to load reports: ${e.message}")
            }
        }
    }

    // --- フィルタ適用ロジック ---

    private fun applyFilters() {
        _uiState.value = _uiState.value.copy(
            filteredIssues = filterAndSortTasks(_rawIssues, _taskFilter.value, _taskSort.value),
            filteredClosedIssues = filterAndSortTasks(_rawClosedIssues, _archiveFilter.value, _archiveSort.value),
            filteredReports = filterAndSortReports(_rawReports, _reportFilterQuery.value, _reportSort.value),

            taskFilterCriteria = _taskFilter.value,
            taskSortOption = _taskSort.value,
            archiveFilterCriteria = _archiveFilter.value,
            archiveSortOption = _archiveSort.value,
            reportFilterQuery = _reportFilterQuery.value,
            reportSortOption = _reportSort.value
        )
    }

    private fun filterAndSortTasks(issues: List<Issue>, filter: TaskFilterCriteria, sort: SortOption): List<Issue> {
        var result = issues.filter { issue ->
            if (filter.selectedLabels.isEmpty()) true
            else filter.selectedLabels.all { reqLabel -> issue.labels.any { it.name == reqLabel } }
        }

        if (filter.dateQuery.isNotEmpty()) {
            result = result.filter {
                it.createdAt.toString().contains(filter.dateQuery)
            }
        }

        result = when (sort) {
            SortOption.CREATED_DESC -> result.sortedByDescending { it.createdAt }
            SortOption.CREATED_ASC -> result.sortedBy { it.createdAt }
            SortOption.PRIORITY_DESC -> result.sortedByDescending { getPriorityScore(issue = it) }
            SortOption.PRIORITY_ASC -> result.sortedBy { getPriorityScore(issue = it) }
            SortOption.NAME_ASC -> result.sortedBy { it.title }
            SortOption.NAME_DESC -> result.sortedByDescending { it.title }
        }
        return result
    }

    private fun getPriorityScore(issue: Issue): Int {
        val pLabel = issue.labels.find { it.name.startsWith("p:") }?.name ?: return 0
        return when (pLabel) {
            "p:critical" -> 4
            "p:high" -> 3
            "p:medium" -> 2
            "p:low" -> 1
            else -> 0
        }
    }

    private fun filterAndSortReports(reports: List<RepoContent>, query: String, sort: SortOption): List<RepoContent> {
        var result = reports.filter { it.type == "file" && it.name.endsWith(".md") }

        if (query.isNotEmpty()) {
            result = result.filter { it.name.contains(query) }
        }

        result = when (sort) {
            SortOption.NAME_ASC, SortOption.CREATED_ASC -> result.sortedBy { it.name }
            SortOption.NAME_DESC, SortOption.CREATED_DESC, SortOption.PRIORITY_DESC, SortOption.PRIORITY_ASC -> result.sortedByDescending { it.name }
        }
        return result
    }

    // --- UIからの操作用メソッド ---

    fun setTaskFilter(criteria: TaskFilterCriteria) {
        _taskFilter.value = criteria
        applyFilters()
    }
    fun setTaskSort(option: SortOption) {
        _taskSort.value = option
        applyFilters()
    }

    fun setArchiveFilter(criteria: TaskFilterCriteria) {
        _archiveFilter.value = criteria
        applyFilters()
    }
    fun setArchiveSort(option: SortOption) {
        _archiveSort.value = option
        applyFilters()
    }

    fun setReportFilter(query: String) {
        _reportFilterQuery.value = query
        applyFilters()
    }
    fun setReportSort(option: SortOption) {
        _reportSort.value = option
        applyFilters()
    }

    fun selectReport(path: String) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true, selectedReportContent = null)
                val content = repository.getFileContent(path)
                _uiState.value = _uiState.value.copy(isLoading = false, selectedReportContent = content)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, statusMessage = "Failed to load content: ${e.message}")
            }
        }
    }

    fun clearSelectedReport() {
        _uiState.value = _uiState.value.copy(selectedReportContent = null)
    }

    // --- 既存のタスク操作メソッド ---

    fun closeIssue(issue: Issue) {
        viewModelScope.launch {
            try {
                // UI上での楽観的更新 (filteredリストからも消す)
                _uiState.value = _uiState.value.copy(
                    filteredIssues = _uiState.value.filteredIssues.filter { it.number != issue.number }
                )

                repository.closeIssue(issue.number)
                refresh()
            } catch (e: Exception) {
                refresh()
            }
        }
    }

    fun updateIssueStatus(issue: Issue, newStatusLabel: Label) {
        viewModelScope.launch {
            try {
                val otherLabels = issue.labels.filter { !it.name.startsWith("s:") }.map { it.name }
                val newLabelList = otherLabels + newStatusLabel.name
                repository.updateIssueLabels(issue.number, newLabelList)
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(statusMessage = "Error: ${e.message}")
            }
        }
    }

    fun addOneOffTask(title: String, selectedLabels: List<Label>) {
        viewModelScope.launch {
            try {
                val labelNames = selectedLabels.map { it.name } + "mobile-entry"
                repository.createIssue(title, labelNames)
                refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(statusMessage = "Error: ${e.message}")
            }
        }
    }

    fun addRoutineTask(title: String, schedule: String, selectedLabels: List<Label>) {
        viewModelScope.launch {
            try {
                val labelNames = selectedLabels.map { it.name } + "routine" + "mobile-added"
                repository.createRoutineTask(title, schedule, labelNames)
                _uiState.value = _uiState.value.copy(statusMessage = "Routine Added!")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(statusMessage = "Error: ${e.message}")
            }
        }
    }
}