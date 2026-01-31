package com.example.mobiletaskmanager.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RepoContent(
    val name: String,
    val path: String,
    val type: String, // "file" or "dir"
    val sha: String
)