package com.example.mobiletaskmanager.data.model

import android.annotation.SuppressLint
import kotlinx.serialization.Serializable

@SuppressLint("UnsafeOptInUsageError")
@Serializable
data class FileContentResponse(
    val name: String,
    val path: String,
    val sha: String,
    val size: Int,
    val url: String,
    val type: String,
    val content: String? = null, // Base64エンコードされた中身
    val encoding: String? = null // "base64" など
)