package com.example.mobiletaskmanager.ui.screens.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobiletaskmanager.data.model.RepoContent
import com.example.mobiletaskmanager.data.repository.GithubRepository
import com.example.mobiletaskmanager.ui.components.SortOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReportUiState(
    val filteredReports: List<RepoContent> = emptyList(),
    val selectedReportContent: String? = null,
    val isLoading: Boolean = false,
    val filterQuery: String = "",
    val sortOption: SortOption = SortOption.NAME_DESC,
    val error: String? = null
)

class ReportViewModel(private val repository: GithubRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState = _uiState.asStateFlow()

    private var _rawReports: List<RepoContent> = emptyList()

    fun loadReports() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val reports = repository.getReportFiles()
                _rawReports = reports
                applyFiltersAndSort()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun selectReport(path: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, selectedReportContent = null) }
            try {
                val content = repository.getFileContent(path)
                _uiState.update { it.copy(isLoading = false, selectedReportContent = content) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun clearSelectedReport() {
        _uiState.update { it.copy(selectedReportContent = null) }
    }

    fun setFilter(query: String) {
        _uiState.update { it.copy(filterQuery = query) }
        applyFiltersAndSort()
    }

    fun setSort(option: SortOption) {
        _uiState.update { it.copy(sortOption = option) }
        applyFiltersAndSort()
    }

    private fun applyFiltersAndSort() {
        val state = _uiState.value
        val result = filterAndSortReports(_rawReports, state.filterQuery, state.sortOption)
        _uiState.update { it.copy(filteredReports = result) }
    }

    private fun filterAndSortReports(reports: List<RepoContent>, query: String, sort: SortOption): List<RepoContent> {
        var result = reports.filter { it.type == "file" && it.name.endsWith(".md") }

        if (query.isNotEmpty()) {
            result = result.filter { it.name.contains(query, ignoreCase = true) }
        }

        result = when (sort) {
            SortOption.NAME_ASC, SortOption.CREATED_ASC -> result.sortedBy { it.name }
            SortOption.NAME_DESC, SortOption.CREATED_DESC, SortOption.PRIORITY_DESC, SortOption.PRIORITY_ASC -> result.sortedByDescending { it.name }
            else -> result.sortedByDescending { it.name } 
        }
        return result
    }
}
