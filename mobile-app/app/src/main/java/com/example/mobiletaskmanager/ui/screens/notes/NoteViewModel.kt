package com.example.mobiletaskmanager.ui.screens.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobiletaskmanager.data.model.Issue
import com.example.mobiletaskmanager.data.repository.GithubRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.temporal.ChronoUnit

class NoteViewModel(private val repository: GithubRepository) : ViewModel() {
    private val _notes = MutableStateFlow<List<Issue>>(emptyList())
    val notes = _notes.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing = _isRefreshing.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val threeDaysAgo = Instant.now().minus(3, ChronoUnit.DAYS).toString()
                val result = repository.getNotes(since = threeDaysAgo)
                _notes.value = result.sortedByDescending { it.createdAt }
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun addNote(content: String) {
        viewModelScope.launch {
            repository.createIssue(content, listOf("type:note", "mobile-entry"))
            refresh()
        }
    }
}
