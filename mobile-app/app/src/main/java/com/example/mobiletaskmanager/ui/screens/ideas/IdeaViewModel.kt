package com.example.mobiletaskmanager.ui.screens.ideas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobiletaskmanager.data.model.Issue
import com.example.mobiletaskmanager.data.repository.GithubRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class IdeaViewModel(private val repository: GithubRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(IdeaUiState())
    val uiState = _uiState.asStateFlow()

    private val parentRegex = """Parent: #(\d+)""".toRegex()

    init {
        loadIdeas()
    }

    // 引数 isRefresh を追加して、初回ロードとリフレッシュを使い分ける
    fun loadIdeas(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = true) }
            } else {
                _uiState.update { it.copy(isLoading = true) }
            }

            try {
                val allIssues = repository.getIssues()

                val ideas = allIssues.filter { it.labels.any { l -> l.name == "type:idea" } }
                val features = allIssues.filter { it.labels.any { l -> l.name == "type:feature" } }

                val ideasWithFeatures = ideas.map { idea ->
                    val relatedFeatures = features.filter { feature ->
                        parentRegex.find(feature.body ?: "")?.let { matchResult ->
                            matchResult.destructured.component1().toInt() == idea.number
                        } ?: false
                    }
                    IdeaWithFeatures(idea, relatedFeatures)
                }

                _uiState.update {
                    it.copy(
                        ideas = ideasWithFeatures,
                        isLoading = false,
                        isRefreshing = false,
                        error = null
                    )
                }

                // 詳細表示中の場合は、選択中のデータも更新する
                val currentSelected = _uiState.value.selectedIdea
                if (currentSelected != null) {
                    val updatedSelected = ideasWithFeatures.find { it.idea.number == currentSelected.idea.number }
                    _uiState.update { it.copy(selectedIdea = updatedSelected) }
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false, isRefreshing = false) }
            }
        }
    }

    fun selectIdea(idea: IdeaWithFeatures?) {
        _uiState.update { it.copy(selectedIdea = idea) }
    }

    fun addIdea(title: String, body: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) } // 追加中もロード表示
            try {
                repository.createIssue(title, listOf("type:idea"), body)
                loadIdeas(isRefresh = true) // 追加後に最新化
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun addFeature(parentIdea: Issue, title: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val body = "Parent: #${parentIdea.number}"
            try {
                repository.createIssue(title, listOf("type:feature"), body)
                loadIdeas(isRefresh = true) // 追加後に最新化
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun closeIssueAndSubIssues(issue: Issue) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val ideaWithFeatures = _uiState.value.ideas.find { it.idea.number == issue.number }

                if (ideaWithFeatures != null) {
                    ideaWithFeatures.features.forEach { feature ->
                        repository.closeIssue(feature.number)
                    }
                }
                repository.closeIssue(issue.number)

                // クローズ後は詳細画面を閉じる
                _uiState.update { it.copy(selectedIdea = null) }
                loadIdeas(isRefresh = true)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}