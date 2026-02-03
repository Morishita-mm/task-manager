package com.example.mobiletaskmanager.ui.screens.knowledge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobiletaskmanager.data.model.RepoContent
import com.example.mobiletaskmanager.data.repository.GithubRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class KnowledgeUiState(
    val knowledgeFiles: List<RepoContent> = emptyList(),
    val editingKnowledge: EditingKnowledge? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

data class EditingKnowledge(
    val name: String = "",
    val content: String = "",
    val sha: String? = null
)

class KnowledgeViewModel(private val repository: GithubRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(KnowledgeUiState())
    val uiState = _uiState.asStateFlow()

    fun loadKnowledgeFiles() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val files = repository.getKnowledgeFiles()
                val filtered = files
                    .filter { it.name.endsWith(".md") }
                    .sortedBy { it.name }
                _uiState.update { it.copy(knowledgeFiles = filtered) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun selectKnowledge(file: RepoContent) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val content = repository.getFileContent(file.path)
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        editingKnowledge = EditingKnowledge(
                            name = file.name,
                            content = content,
                            sha = file.sha
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun startCreateKnowledge() {
        _uiState.update { it.copy(editingKnowledge = EditingKnowledge(name = "", content = "", sha = null)) }
    }

    fun closeEditor() {
        _uiState.update { it.copy(editingKnowledge = null) }
    }

    fun saveKnowledge(name: String, content: String) {
        val currentEditing = _uiState.value.editingKnowledge ?: return
        val safeName = if (name.endsWith(".md")) name else "$name.md"

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.saveKnowledgeFile(safeName, content, currentEditing.sha)
                loadKnowledgeFiles()
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        editingKnowledge = null,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
