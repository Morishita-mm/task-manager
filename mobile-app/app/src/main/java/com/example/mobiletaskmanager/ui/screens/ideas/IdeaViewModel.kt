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
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.createIssue(title, listOf("type:idea"), body)
                loadIdeas(isRefresh = true)
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
                loadIdeas(isRefresh = true)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun closeIssueAndSubIssues(issue: Issue) {
        viewModelScope.launch {
            // 1. まず詳細画面でローディングを表示（まだ一覧には戻らない）
            _uiState.update { it.copy(isLoading = true) }

            try {
                val ideaWithFeatures = _uiState.value.ideas.find { it.idea.number == issue.number }

                // 2. 関連する子Issue（Feature）をすべてクローズ
                ideaWithFeatures?.features?.forEach { feature ->
                    repository.closeIssue(feature.number)
                }

                // 3. 大元（Idea）のIssueをクローズ
                repository.closeIssue(issue.number)

                // 4. すべてのクローズ処理が完了したタイミングで、詳細画面を閉じて一覧に戻る
                _uiState.update { it.copy(selectedIdea = null) }

                // 5. 最新のリスト状態に更新する
                loadIdeas(isRefresh = true)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}