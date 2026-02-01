package com.example.mobiletaskmanager.data.repository

import android.annotation.SuppressLint
import android.util.Base64
import com.example.mobiletaskmanager.data.api.GithubApiService
import com.example.mobiletaskmanager.data.model.*
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class UpdateIssueRequest(
    val state: String? = null,
    val labels: List<String>? = null
)

class GithubRepository(
    private val api: GithubApiService,
    private val token: String,
    private val owner: String,
    private val repo: String
) {
    private val authHeader = "token $token"

    suspend fun getIssues(): List<Issue> {
        return api.getIssues(authHeader, owner, repo, state = "open")
    }

    suspend fun getNotes(since: String): List<Issue> {
        return api.getIssues(
            authHeader, owner, repo,
            state = "all",
            labels = "type:note",
            since = since
        )
    }

    suspend fun getClosedIssues(since: String? = null): List<Issue> {
        return api.getIssues(authHeader, owner, repo, state = "closed", since = since)
    }

    suspend fun getLabels(): List<Label> {
        return api.getLabels(authHeader, owner, repo)
    }

    suspend fun closeIssue(number: Int) {
        api.updateIssue(authHeader, owner, repo, number, UpdateIssueRequest(state = "closed"))
    }

    suspend fun updateIssueLabels(number: Int, labels: List<String>) {
        api.updateIssue(authHeader, owner, repo, number, UpdateIssueRequest(labels = labels))
    }

    suspend fun createIssue(title: String, labels: List<String>): Issue {
        return api.createIssue(
            authHeader, owner, repo,
            CreateIssueRequest(title, labels)
        )
    }

    suspend fun createRoutineTask(title: String, schedule: String, labelNames: List<String>) {
        val path = "config/routines.yaml"

        val currentFile = api.getFileContent(authHeader, owner, repo, path)
        val rawContent = currentFile.content ?: ""
        val cleanContent = rawContent.replace("\n", "")

        val currentYaml = if (cleanContent.isNotEmpty()) {
            String(Base64.decode(cleanContent, Base64.DEFAULT), Charsets.UTF_8)
        } else ""

        val labelsJson = labelNames.joinToString(", ", "[", "]") { "\"$it\"" }

        val newBlock = """
            |
            |  - title: "$title"
            |    schedule: "$schedule"
            |    labels: $labelsJson
        """.trimMargin()

        val newYaml = currentYaml + newBlock
        val newContentBase64 = Base64.encodeToString(newYaml.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

        api.updateFileContent(
            authHeader, owner, repo, path,
            UpdateFileRequest(
                message = "Add routine: $title",
                content = newContentBase64,
                sha = currentFile.sha
            )
        )
    }
    suspend fun getReportFiles(): List<RepoContent> {
        return api.getDirContents(authHeader, owner, repo, "reports")
    }

    suspend fun getFileContent(path: String): String {
        val response = api.getFileContent(authHeader, owner, repo, path)
        val raw = response.content ?: return ""
        val clean = raw.replace("\n", "")
        return String(Base64.decode(clean, Base64.DEFAULT), Charsets.UTF_8)
    }
}