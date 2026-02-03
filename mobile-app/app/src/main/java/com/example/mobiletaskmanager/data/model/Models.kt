@file:OptIn(kotlinx.serialization.InternalSerializationApi::class)

package com.example.mobiletaskmanager.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Issue(
    val number: Int,
    val title: String,
    val state: String,
    @SerialName("created_at")
    val createdAt: String,
    val body: String? = null,
    val labels: List<Label> = emptyList()
)

@Serializable
data class Label(
    val name: String,
    val color: String = "808080"
)

@Serializable
data class CreateIssueRequest(
    val title: String,
    val body: String? = null,
    val labels: List<String> = listOf("mobile-entry")
)

@Serializable
data class UpdateIssueRequest(
    val state: String
)

@Serializable
data class GithubFileResponse(
    val name: String,
    val path: String,
    val sha: String,
    val content: String? = null,
    val encoding: String? = null
)

@Serializable
data class UpdateFileRequest(
    val message: String,
    val content: String,
    val sha: String? = null
)

@Serializable
data class UpdateFileResponse(
    val content: GithubFileResponse? = null
)

@Serializable
data class CreateFileRequest(
    val message: String,
    val content: String
)