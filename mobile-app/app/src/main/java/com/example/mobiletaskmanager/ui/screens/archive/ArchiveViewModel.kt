package com.example.mobiletaskmanager.ui.screens.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobiletaskmanager.data.model.Issue
import com.example.mobiletaskmanager.data.repository.GithubRepository
import com.example.mobiletaskmanager.ui.components.SortOption
import com.example.mobiletaskmanager.ui.components.TaskFilterCriteria
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

data class ArchiveUiState(
    val filteredIssues: List<Issue> = emptyList(),
    val isLoading: Boolean = false,
    val filterCriteria: TaskFilterCriteria = TaskFilterCriteria(),
    val sortOption: SortOption = SortOption.CREATED_DESC,
    val error: String? = null
)

class ArchiveViewModel(private val repository: GithubRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ArchiveUiState())
    val uiState = _uiState.asStateFlow()

    private var _rawIssues: List<Issue> = emptyList()

    fun loadInitialData() {
        loadClosedIssues()
    }

    private fun loadClosedIssues() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val oneWeekAgo = Instant.now().minus(7, ChronoUnit.DAYS).toString()
                val closed = repository.getClosedIssues(since = oneWeekAgo)
                _rawIssues = closed.filter { it.labels.none { l -> l.name == "type:note" || l.name == "type:idea" || l.name == "type:feature" }}
                applyFiltersAndSort()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
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
