package com.example.mobiletaskmanager.ui.screens.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.mobiletaskmanager.data.model.Issue
import com.example.mobiletaskmanager.ui.theme.*
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteScreen(
    notes: List<Issue>,
    isRefreshing: Boolean,
    onAddNote: (String) -> Unit,
    onRefresh: () -> Unit
) {
    var text by remember { mutableStateOf("") }
    var isPosting by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current

    LaunchedEffect(Unit) {
        if (notes.isEmpty() && !isRefreshing) {
            onRefresh()
        }
    }

    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) {
            isPosting = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        Surface(color = SurfaceColor, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Microblog / Notes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(16.dp)
            )
        }
        HorizontalDivider(color = DividerColor)

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.weight(1f)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = {
                    items(notes, key = { it.number }) { note ->
                        NoteItem(note)
                    }
                }
            )
        }

        Surface(
            color = SurfaceColor,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    placeholder = { Text("What's on your mind?") },
                    modifier = Modifier.weight(1f),
                    maxLines = 4,
                    enabled = !isPosting, 
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (text.isNotBlank() && !isPosting) {
                            isPosting = true
                            onAddNote(text)
                            text = ""
                            focusManager.clearFocus()
                        }
                    })
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPosting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = PrimaryAccent,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = {
                                if (text.isNotBlank()) {
                                    isPosting = true
                                    onAddNote(text)
                                    text = ""
                                    focusManager.clearFocus()
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (text.isNotBlank()) PrimaryAccent else Color.Gray.copy(alpha = 0.5f),
                                    androidx.compose.foundation.shape.CircleShape
                                ),
                            enabled = text.isNotBlank()
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Post", tint = SurfaceColor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NoteItem(note: Issue) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = note.title,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatTime(note.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
    }
}

fun formatTime(isoString: String): String {
    return try {
        val instant = java.time.Instant.parse(isoString)
        val zonedDateTime = instant.atZone(ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("MM/dd HH:mm")
        zonedDateTime.format(formatter)
    } catch (e: Exception) {
        isoString
    }
}
