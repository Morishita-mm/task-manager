package com.example.mobiletaskmanager.ui.screens.ideas

import com.example.mobiletaskmanager.data.model.Issue

data class IdeaUiState(
    val ideas: List<IdeaWithFeatures> = emptyList(),
    val selectedIdea: IdeaWithFeatures? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

data class IdeaWithFeatures(
    val idea: Issue,
    val features: List<Issue>
)
