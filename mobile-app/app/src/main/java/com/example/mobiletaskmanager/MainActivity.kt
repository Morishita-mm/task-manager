package com.example.mobiletaskmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobiletaskmanager.data.api.GithubApiService
import com.example.mobiletaskmanager.data.repository.GithubRepository
import com.example.mobiletaskmanager.ui.MainViewModel
import com.example.mobiletaskmanager.ui.navigation.AppNavigation
import com.example.mobiletaskmanager.ui.theme.*
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // DI Setup (Hiltを使わない簡易DI)
        val contentType = "application/json".toMediaType()
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory(contentType))
            .build()
        val apiService = retrofit.create(GithubApiService::class.java)

        val repository = GithubRepository(
            api = apiService,
            token = BuildConfig.GITHUB_TOKEN,
            owner = BuildConfig.GITHUB_OWNER,
            repo = BuildConfig.GITHUB_REPO
        )

        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(repository) as T
            }
        }

        setContent {
            // テーマの適用
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = AppBackground,
                    surface = SurfaceColor,
                    primary = PrimaryAccent,
                    onBackground = TextPrimary,
                    onSurface = TextPrimary
                )
            ) {
                val viewModel: MainViewModel = viewModel(factory = viewModelFactory)
                val uiState by viewModel.uiState.collectAsState()

                // ナビゲーションの呼び出し
                AppNavigation(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }
}