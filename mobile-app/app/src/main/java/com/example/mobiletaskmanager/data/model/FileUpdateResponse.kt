package com.example.mobiletaskmanager.data.model

import kotlinx.serialization.Serializable

@Serializable
data class FileUpdateResponse(
    val content: RepoContent? = null, // 更新されたファイルのメタデータ
    val commit: CommitInfo? = null    // コミット情報 (必要なら)
)

@Serializable
data class CommitInfo(
    val sha: String,
    val message: String
)