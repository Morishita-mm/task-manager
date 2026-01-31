package com.example.mobiletaskmanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobiletaskmanager.data.model.Issue
import com.example.mobiletaskmanager.data.model.Label
import com.example.mobiletaskmanager.data.model.RepoContent // 追加
import com.example.mobiletaskmanager.data.repository.GithubRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MainUiState(
    val issues: List<Issue> = emptyList(),
    val closedIssues: List<Issue> = emptyList(), // 追加: 完了済みタスク
    val labels: List<Label> = emptyList(),
    val reportFiles: List<RepoContent> = emptyList(), // 追加: レポートファイル一覧
    val selectedReportContent: String? = null,        // 追加: 選択されたレポートの中身
    val statusMessage: String = "Loading...",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false
)

class MainViewModel(
    private val repository: GithubRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

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
            // 並列実行するのが理想ですが、簡易的に直列で記述します
            val labels = repository.getLabels()
            val issues = repository.getIssues()
            // ※ ClosedIssuesやReportsは、画面を開いた時に取得する設計もアリですが、
            // 今回はシンプルに一括取得します（通信量が増える点に注意）
            // val closed = repository.getClosedIssues()
            // val reports = repository.getReportFiles()
            // ↑ 全てここで呼ぶと重いので、専用のロード関数を作ります

            _uiState.value = _uiState.value.copy(
                labels = labels,
                issues = issues,
                statusMessage = "Active Tasks: ${issues.size}"
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(statusMessage = "Error: ${e.message}")
        }
    }

    // ▼▼▼ 追加: レポート一覧のロード ▼▼▼
    fun loadReports() {
        viewModelScope.launch {
            try {
                val reports = repository.getReportFiles()
                // 日付順などでソートしたい場合はここで (nameが "YYYY-MM-DD.md" なら名前降順で新しい順になる)
                val sortedReports = reports.sortedByDescending { it.name }
                _uiState.value = _uiState.value.copy(reportFiles = sortedReports)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(statusMessage = "Failed to load reports: ${e.message}")
            }
        }
    }

    // ▼▼▼ 追加: 特定のレポート内容のロード ▼▼▼
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

    // ▼▼▼ 追加: 選択解除（一覧に戻る） ▼▼▼
    fun clearSelectedReport() {
        _uiState.value = _uiState.value.copy(selectedReportContent = null)
    }

    fun closeIssue(issue: Issue) {
        viewModelScope.launch {
            try {
                // UI上で先行してリストから削除（楽観的UI更新）
                val currentList = _uiState.value.issues
                _uiState.value = _uiState.value.copy(
                    issues = currentList.filter { it.number != issue.number }
                )

                repository.closeIssue(issue.number)

                _uiState.value = _uiState.value.copy(
                    statusMessage = "Active Tasks: ${_uiState.value.issues.size}"
                )
            } catch (e: Exception) {
                refresh()
            }
        }
    }

    fun updateIssueStatus(issue: Issue, newStatusLabel: Label) {
        viewModelScope.launch {
            try {
                // 1. 現在のラベルリストから、既存のステータス(s:...)を除去
                val otherLabels = issue.labels.filter { !it.name.startsWith("s:") }.map { it.name }

                // 2. 新しいステータスを追加
                val newLabelList = otherLabels + newStatusLabel.name

                // 3. API更新
                repository.updateIssueLabels(issue.number, newLabelList)

                // 4. UI更新（再取得）
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
                val newIssue = repository.createIssue(title, labelNames)

                val currentList = _uiState.value.issues
                _uiState.value = _uiState.value.copy(
                    issues = listOf(newIssue) + currentList,
                    statusMessage = "Active Tasks: ${currentList.size + 1}"
                )
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

    fun loadClosedIssues() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val closed = repository.getClosedIssues()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    closedIssues = closed
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    statusMessage = "Failed to load archive: ${e.message}"
                )
            }
        }
    }
}