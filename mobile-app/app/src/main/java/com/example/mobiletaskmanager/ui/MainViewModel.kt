package com.example.mobiletaskmanager.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobiletaskmanager.data.model.Issue
import com.example.mobiletaskmanager.data.model.Label
import com.example.mobiletaskmanager.data.repository.GithubRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MainUiState(
    val issues: List<Issue> = emptyList(),
    val labels: List<Label> = emptyList(),
    val statusMessage: String = "Loading...",
    val isLoading: Boolean = false
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
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val labels = repository.getLabels()
                val issues = repository.getIssues()

                _uiState.value = _uiState.value.copy(
                    labels = labels,
                    issues = issues,
                    statusMessage = "Active Tasks: ${issues.size}",
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Error: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    fun closeIssue(issue: Issue) {
        viewModelScope.launch {
            try {
                val currentList = _uiState.value.issues
                _uiState.value = _uiState.value.copy(
                    issues = currentList.filter { it.number != issue.number }
                )
                repository.closeIssue(issue.number)
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Active Tasks: ${_uiState.value.issues.size}"
                )
            } catch (e: Exception) {
                loadData()
            }
        }
    }

    fun addOneOffTask(title: String, selectedLabels: List<Label>) {
        viewModelScope.launch {
            try {
                // UIローディング開始（任意）
                // _uiState.value = _uiState.value.copy(isLoading = true)

                val labelNames = selectedLabels.map { it.name } + "mobile-entry"

                // 1. 作成されたタスクを受け取る
                val newIssue = repository.createIssue(title, labelNames)

                // 2. 現在のリストの先頭に追加して画面更新
                val currentList = _uiState.value.issues
                _uiState.value = _uiState.value.copy(
                    issues = listOf(newIssue) + currentList,
                    statusMessage = "Active Tasks: ${currentList.size + 1}",
                    isLoading = false
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Error: ${e.message}",
                    isLoading = false
                )
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