package com.example.mobiletaskmanager.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobiletaskmanager.data.model.Issue
import com.example.mobiletaskmanager.data.model.Label
import com.example.mobiletaskmanager.ui.theme.*

@Composable
fun TaskRow(
    issue: Issue,
    statusLabels: List<Label>,
    onClose: (Issue) -> Unit,
    onStatusChange: (Label) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SurfaceColor)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = issue.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (issue.labels.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    issue.labels.forEach { label ->
                        if (label.name == "mobile-entry") return@forEach
                        val bgColor = label.color.toColor()
                        Surface(
                            color = bgColor,
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp)) {
                                Text(
                                    text = label.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Menu
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = TextSecondary)
            }
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                containerColor = SurfaceColor
            ) {
                statusLabels.forEach { label ->
                    val displayName = label.name.removePrefix("s:")
                    DropdownMenuItem(
                        text = { Text("Move to $displayName", color = TextPrimary) },
                        onClick = {
                            onStatusChange(label)
                            showMenu = false
                        }
                    )
                }
            }
        }

        // Done Button
        IconButton(
            onClick = { onClose(issue) },
            modifier = Modifier
                .size(36.dp)
                .background(PrimaryAccent.copy(alpha = 0.2f), CircleShape)
        ) {
            Icon(Icons.Default.Check, contentDescription = "Done", tint = PrimaryAccent, modifier = Modifier.size(20.dp))
        }
    }
}