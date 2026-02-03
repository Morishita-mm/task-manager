package com.example.mobiletaskmanager.ui.screens.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobiletaskmanager.data.model.Issue
import com.example.mobiletaskmanager.data.model.Label
import com.example.mobiletaskmanager.data.repository.GithubRepository
import com.example.mobiletaskmanager.ui.components.SortOption
import com.example.mobiletaskmanager.ui.components.TaskFilterCriteria
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TaskViewModel(private val repository: GithubRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState = _uiState.asStateFlow()

    private var _rawIssues: List<Issue> = emptyList()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                val labels = repository.getLabels()
                val issues = repository.getIssues()
                _rawIssues = issues.filter { it.labels.none { l -> l.name == "type:note" } }
                _uiState.update { it.copy(labels = labels) }
                applyFiltersAndSort()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun setFilter(criteria: TaskFilterCriteria) {
        _uiState.update { it.copy(filterCriteria = criteria) }
        applyFiltersAndSort()
    }

    fun setSort(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
        applyFiltersAndSort()
    }

    private fun applyFiltersAndSort() {
        val state = _uiState.value
        val result = filterAndSortTasks(_rawIssues, state.filterCriteria, state.sortOption)
        _uiState.update { it.copy(filteredIssues = result) }
    }

    fun closeIssue(issue: Issue) {
        viewModelScope.launch {
            try {
                _uiState.update { state ->
                    state.copy(filteredIssues = state.filteredIssues.filter { it.number != issue.number })
                }
                repository.closeIssue(issue.number)
                refresh()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
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
                _uiState.update { it.copy(error = e.message) }
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
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun addRoutineTask(title: String, schedule: String, selectedLabels: List<Label>) {
        viewModelScope.launch {
            try {
                val labelNames = selectedLabels.map { it.name } + "routine" + "mobile-added"
                repository.createRoutineTask(title, schedule, labelNames)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
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
}
