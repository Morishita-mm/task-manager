@file:OptIn(InternalSerializationApi::class)

package com.example.mobiletaskmanager.data.api // パッケージ名はご自身環境に合わせて

import com.example.mobiletaskmanager.data.model.CreateIssueRequest
import com.example.mobiletaskmanager.data.model.UpdateFileRequest
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

// --- Data Models (データモデル) ---

// ▼ 1. ラベル用データクラスを追加
@Serializable
data class Label(
    val name: String,
    val color: String // "ff0000" のような16進数文字列（#なし）が返ってきます
)

// ▼ 2. Issueクラスを更新してラベルリストを持たせる
@Serializable
data class Issue(
    val number: Int,
    val title: String,
    val state: String,
    val labels: List<Label> = emptyList() // 追加 (デフォルト空リスト)
)

// Issueデータ
@Serializable
data class CreateIssueRequest(
    val title: String,
    val labels: List<String> = listOf("mobile-entry") // スマホから入れたことがわかるように
)

// Issue更新用リクエスト
@Serializable
data class UpdateIssueRequest(
    val state: String
)

// ファイル取得時のレスポンス
@Serializable
data class GithubFileResponse(
    val name: String,
    val path: String,
    val sha: String,      // ファイル更新時に必須
    val content: String? = null,  // Base64エンコードされた中身
    val encoding: String? = null // "base64" (nullの場合もあるのでnullable推奨)
)

// ファイル更新(PUT)時のリクエスト
@Serializable
data class UpdateFileRequest(
    val message: String, // コミットメッセージ
    val content: String, // Base64エンコードされた新ファイルの中身
    val sha: String      // 競合チェック用SHA
)

// ファイル更新時のレスポンス
@Serializable
data class UpdateFileResponse(
    val content: GithubFileResponse? = null
)

// --- API Interface (定義) ---

interface GithubApiService {

    // 1. Issue一覧取得
    @GET("repos/{owner}/{repo}/issues")
    suspend fun getIssues(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open"
    ): List<com.example.mobiletaskmanager.data.model.Issue>

    // 2. Issue更新 (Closeなど)
    @PATCH("repos/{owner}/{repo}/issues/{number}")
    suspend fun updateIssue(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
        @Body body: com.example.mobiletaskmanager.data.repository.UpdateIssueRequest
    ): Issue

    // 3. ファイル内容取得 (YAML読み込み用)
    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getFileContent(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String
    ): GithubFileResponse

    // 4. ファイル内容更新 (YAML追記用)
    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun updateFileContent(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Body body: UpdateFileRequest
    ): UpdateFileResponse

    @POST("repos/{owner}/{repo}/issues")
    suspend fun createIssue(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: CreateIssueRequest
    ): com.example.mobiletaskmanager.data.model.Issue

    // ▼ 3. ラベル一覧取得APIを追加 (GET /repos/.../labels)
    @GET("repos/{owner}/{repo}/labels")
    suspend fun getLabels(
        @Header("Authorization") token: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): List<com.example.mobiletaskmanager.data.model.Label>
}