package com.example.mobiletaskmanager.ui.screens.ideas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobiletaskmanager.data.model.Issue
import com.example.mobiletaskmanager.data.repository.GithubRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class IdeaViewModel(private val repository: GithubRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(IdeaUiState())
    val uiState = _uiState.asStateFlow()

    private val parentRegex = """Parent: #(\d+)""".toRegex()

    init {
        loadIdeas()
    }

    fun loadIdeas(isRefresh: Boolean = false) {
        viewModelScope.launch {
            if (isRefresh) _uiState.update { it.copy(isRefreshing = true) }
            else _uiState.update { it.copy(isLoading = true) }

            try {
                val allIssues = repository.getIssues(state = "all")

                val allIdeas = allIssues.filter { it.labels.any { l -> l.name == "type:idea" } }
                val allFeatures = allIssues.filter { it.labels.any { l -> l.name == "type:feature" } }

                val ideasWithFeatures = allIdeas.map { idea ->
                    val relatedFeatures = allFeatures.filter { feature ->
                        parentRegex.find(feature.body ?: "")?.let {
                            it.destructured.component1().toInt() == idea.number
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

                _uiState.value.selectedIdea?.let { current ->
                    ideasWithFeatures.find { it.idea.number == current.idea.number }?.let { updated ->
                        _uiState.update { it.copy(selectedIdea = updated) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false, isRefreshing = false) }
            }
        }
    }

    fun toggleShowClosed() {
        _uiState.update { it.copy(showClosed = !it.showClosed) }
    }

    fun selectIdea(idea: IdeaWithFeatures?) { _uiState.update { it.copy(selectedIdea = idea) } }

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
            try {
                repository.createIssue(title, listOf("type:feature"), "Parent: #${parentIdea.number}")
                loadIdeas(isRefresh = true)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun closeIssueAndSubIssues(issue: Issue) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                _uiState.value.ideas.find { it.idea.number == issue.number }?.features?.forEach {
                    if (it.state == "open") repository.closeIssue(it.number)
                }
                repository.closeIssue(issue.number)
                _uiState.update { it.copy(selectedIdea = null) }
                loadIdeas(isRefresh = true)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun closeFeature(feature: Issue) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.closeIssue(feature.number)
                loadIdeas(isRefresh = true)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }

    fun exportIdeaToMarkdown(ideaWithFeatures: IdeaWithFeatures) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val idea = ideaWithFeatures.idea
                val features = ideaWithFeatures.features

                val displayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

                val proposedDate = ZonedDateTime.parse(idea.createdAt).format(displayFormatter)
                val lastUpdatedDate =
                    (listOf(idea.createdAt) + features.map { it.createdAt }).maxOfOrNull {
                        ZonedDateTime.parse(it)
                    }
                        ?.format(displayFormatter) ?: proposedDate

                val markdown = buildString {
                    appendLine("# ${idea.title}")
                    appendLine()
                    appendLine("## 📅 Timeline")
                    appendLine("- **Proposed Date:** $proposedDate")
                    appendLine("- **Last Updated:** $lastUpdatedDate")
                    appendLine()
                    appendLine("## 📝 Description")
                    appendLine(idea.body ?: "No description provided.")
                    appendLine()
                    appendLine("## 💡 Features")
                    if (features.isEmpty()) {
                        appendLine("- No features defined yet.")
                    } else {
                        features.forEach { feature ->
                            val prefix = if (feature.state == "closed") "~~" else ""
                            val suffix = if (feature.state == "closed") "~~ (Closed)" else ""
                            appendLine("- $prefix${feature.title}$suffix")
                        }
                    }
                    appendLine()
                    appendLine("---")
                    appendLine("Generated by TaskManager App on ${LocalDate.now()}")
                }

                val fileName = idea.title.lowercase().replace(Regex("[^a-z0-9]"), "-").take(30) + ".md"
                repository.saveFile("ideas/$fileName", markdown, "docs: Export Idea '${idea.title}'")

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message, isLoading = false) }
            }
        }
    }
}