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

    fun loadIdeas() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
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

                _uiState.update { it.copy(ideas = ideasWithFeatures, isLoading = false) }

            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun selectIdea(idea: IdeaWithFeatures?) {
        _uiState.update { it.copy(selectedIdea = idea) }
    }

    fun addIdea(title: String, body: String) {
        viewModelScope.launch {
            try {
                repository.createIssue(title, listOf("type:idea"), body)
                loadIdeas()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun addFeature(parentIdea: Issue, title: String) {
        viewModelScope.launch {
            val body = "Parent: #${parentIdea.number}"
            try {
                repository.createIssue(title, listOf("type:feature"), body)
                loadIdeas()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
     fun closeIssueAndSubIssues(issue: Issue) {
        viewModelScope.launch {
            try {
                val ideaWithFeatures = _uiState.value.ideas.find { it.idea.number == issue.number }

                if (ideaWithFeatures != null) {
                    // このアイデアに紐づくすべてのFeatureをクローズ
                    ideaWithFeatures.features.forEach { feature ->
                        repository.closeIssue(feature.number)
                    }
                }

                // 親Issueをクローズ
                repository.closeIssue(issue.number)

                // リストを更新
                loadIdeas()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}
