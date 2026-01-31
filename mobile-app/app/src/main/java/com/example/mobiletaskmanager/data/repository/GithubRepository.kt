package com.example.mobiletaskmanager.data.repository

import android.util.Base64
import com.example.mobiletaskmanager.data.api.GithubApiService
import com.example.mobiletaskmanager.data.model.*

class GithubRepository(
    private val api: GithubApiService,
    private val token: String,
    private val owner: String,
    private val repo: String
) {
    private val authHeader = "token $token"

    suspend fun getIssues(): List<Issue> {
        return api.getIssues(authHeader, owner, repo)
    }

    suspend fun getLabels(): List<Label> {
        return api.getLabels(authHeader, owner, repo)
    }

    suspend fun closeIssue(number: Int) {
        api.updateIssue(authHeader, owner, repo, number, UpdateIssueRequest("closed"))
    }

    // ▼▼▼ 修正箇所: 戻り値を Issue に変更し、return を追加 ▼▼▼
    suspend fun createIssue(title: String, labels: List<String>): Issue {
        return api.createIssue(
            authHeader, owner, repo,
            CreateIssueRequest(title, labels)
        )
    }
    // ▲▲▲ 修正ここまで ▲▲▲

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
}