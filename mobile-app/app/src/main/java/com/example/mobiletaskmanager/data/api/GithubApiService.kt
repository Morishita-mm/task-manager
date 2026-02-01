package com.example.mobiletaskmanager.data.api

import com.example.mobiletaskmanager.data.model.*
import com.example.mobiletaskmanager.data.repository.UpdateIssueRequest
import retrofit2.http.*

interface GithubApiService {
    // ▼▼▼ 修正: stateパラメータを追加 (デフォルトは "open") ▼▼▼
    @GET("repos/{owner}/{repo}/issues")
    suspend fun getIssues(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Query("state") state: String = "open",
        @Query("labels") labels: String? = null,
        @Query("since") since: String? = null,
        @Query("per_page") perPage: Int = 100
    ): List<Issue>

    @POST("repos/{owner}/{repo}/issues")
    suspend fun createIssue(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Body body: CreateIssueRequest
    ): Issue

    @PATCH("repos/{owner}/{repo}/issues/{number}")
    suspend fun updateIssue(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("number") number: Int,
        @Body body: UpdateIssueRequest
    ): Issue

    @GET("repos/{owner}/{repo}/labels")
    suspend fun getLabels(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): List<Label>

    // ファイル操作関連
    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getFileContent(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String
    ): FileContentResponse

    @PUT("repos/{owner}/{repo}/contents/{path}")
    suspend fun updateFileContent(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String,
        @Body body: UpdateFileRequest
    )

    // ▼▼▼ 追加: ディレクトリ内のファイル一覧取得 ▼▼▼
    @GET("repos/{owner}/{repo}/contents/{path}")
    suspend fun getDirContents(
        @Header("Authorization") auth: String,
        @Path("owner") owner: String,
        @Path("repo") repo: String,
        @Path("path") path: String
    ): List<RepoContent>
}