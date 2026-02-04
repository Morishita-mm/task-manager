package com.example.mobiletaskmanager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mobiletaskmanager.data.api.GithubApiService
import com.example.mobiletaskmanager.data.repository.GithubRepository
import com.example.mobiletaskmanager.ui.navigation.AppNavigation
import com.example.mobiletaskmanager.ui.screens.archive.ArchiveViewModel
import com.example.mobiletaskmanager.ui.screens.ideas.IdeaViewModel
import com.example.mobiletaskmanager.ui.screens.knowledge.KnowledgeViewModel
import com.example.mobiletaskmanager.ui.screens.notes.NoteViewModel
import com.example.mobiletaskmanager.ui.screens.reports.ReportViewModel
import com.example.mobiletaskmanager.ui.screens.tasks.TaskViewModel
import com.example.mobiletaskmanager.ui.theme.*
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit

class MainActivity : ComponentActivity() {

    private val json = Json { ignoreUnknownKeys = true }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                return when {
                    modelClass.isAssignableFrom(TaskViewModel::class.java) -> TaskViewModel(repository) as T
                    modelClass.isAssignableFrom(NoteViewModel::class.java) -> NoteViewModel(repository) as T
                    modelClass.isAssignableFrom(IdeaViewModel::class.java) -> IdeaViewModel(repository) as T
                    modelClass.isAssignableFrom(ReportViewModel::class.java) -> ReportViewModel(repository) as T
                    modelClass.isAssignableFrom(KnowledgeViewModel::class.java) -> KnowledgeViewModel(repository) as T
                    modelClass.isAssignableFrom(ArchiveViewModel::class.java) -> ArchiveViewModel(repository) as T
                    else -> throw IllegalArgumentException("Unknown ViewModel class")
                }
            }
        }

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = AppBackground,
                    surface = SurfaceColor,
                    primary = PrimaryAccent,
                    onBackground = TextPrimary,
                    onSurface = TextPrimary
                )
            ) {
                val taskViewModel: TaskViewModel = viewModel(factory = viewModelFactory)
                val noteViewModel: NoteViewModel = viewModel(factory = viewModelFactory)
                val ideaViewModel: IdeaViewModel = viewModel(factory = viewModelFactory)
                val reportViewModel: ReportViewModel = viewModel(factory = viewModelFactory)
                val knowledgeViewModel: KnowledgeViewModel = viewModel(factory = viewModelFactory)
                val archiveViewModel: ArchiveViewModel = viewModel(factory = viewModelFactory)

                AppNavigation(
                    taskViewModel = taskViewModel,
                    noteViewModel = noteViewModel,
                    ideaViewModel = ideaViewModel,
                    reportViewModel = reportViewModel,
                    knowledgeViewModel = knowledgeViewModel,
                    archiveViewModel = archiveViewModel
                )
            }
        }
    }
}