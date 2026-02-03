package com.example.mobiletaskmanager.ui.screens.tasks

import com.example.mobiletaskmanager.data.model.Issue
import com.example.mobiletaskmanager.data.model.Label
import com.example.mobiletaskmanager.ui.components.SortOption
import com.example.mobiletaskmanager.ui.components.TaskFilterCriteria

data class TaskUiState(
    val filteredIssues: List<Issue> = emptyList(),
    val labels: List<Label> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val filterCriteria: TaskFilterCriteria = TaskFilterCriteria(),
    val sortOption: SortOption = SortOption.CREATED_DESC,
    val error: String? = null
)
