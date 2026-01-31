package com.example.mobiletaskmanager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.mobiletaskmanager.data.model.Label
import com.example.mobiletaskmanager.ui.theme.*
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SingleSelectLabelSection(
    title: String,
    labels: List<Label>,
    selectedLabels: List<Label>,
    onLabelSelected: (Label) -> Unit,
    prefixToRemove: String = ""
) {
    if (labels.isEmpty()) return

    Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
    Spacer(modifier = Modifier.height(4.dp))

    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEach { label ->
            val isSelected = selectedLabels.contains(label)
            val displayName = if (prefixToRemove.isNotEmpty() && label.name.startsWith(prefixToRemove)) {
                label.name.removePrefix(prefixToRemove)
            } else {
                label.name
            }

            FilterChip(
                selected = isSelected,
                onClick = { onLabelSelected(label) },
                label = { Text(displayName) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = label.color.toColor().copy(alpha = 0.6f),
                    selectedLabelColor = Color.White,
                    containerColor = SurfaceColor,
                    labelColor = TextPrimary
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = if (isSelected) Color.Transparent else DividerColor,
                    enabled = true, selected = isSelected
                )
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiSelectLabelSection(
    title: String,
    labels: List<Label>,
    selectedLabels: List<Label>,
    onLabelToggle: (Label) -> Unit,
    prefixToRemove: String = ""
) {
    if (labels.isEmpty()) return
    Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
    Spacer(modifier = Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEach { label ->
            val isSelected = selectedLabels.contains(label)
            val displayName = if (prefixToRemove.isNotEmpty() && label.name.startsWith(prefixToRemove)) label.name.removePrefix(prefixToRemove) else label.name

            FilterChip(
                selected = isSelected,
                onClick = { onLabelToggle(label) },
                label = { Text(displayName) },
                leadingIcon = if (isSelected) { { Icon(Icons.Default.Check, null, Modifier.size(16.dp)) } } else null,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = label.color.toColor().copy(alpha = 0.6f),
                    selectedLabelColor = Color.White,
                    containerColor = SurfaceColor,
                    labelColor = TextPrimary
                ),
                border = FilterChipDefaults.filterChipBorder(borderColor = if (isSelected) Color.Transparent else DividerColor, enabled = true, selected = isSelected)
            )
        }
    }
    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
fun AddTaskSheetContent(
    availableLabels: List<Label>,
    onCancel: () -> Unit,
    onSubmit: (String, Boolean, Set<DayOfWeek>, List<Label>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var isRoutine by remember { mutableStateOf(false) }
    var selectedDays by remember { mutableStateOf(emptySet<DayOfWeek>()) }
    var selectedLabels by remember { mutableStateOf(emptyList<Label>()) }

    // --- Label Groups ---
    val statusLabels = remember(availableLabels) { availableLabels.filter { it.name.startsWith("s:") } }
    val priorityLabels = remember(availableLabels) { availableLabels.filter { it.name.startsWith("p:") } }
    val timeLabels = remember(availableLabels) { availableLabels.filter { it.name.startsWith("t:") } }
    val contextLabels = remember(availableLabels) { availableLabels.filter { it.name.startsWith("c:") } }
    val otherLabels = remember(availableLabels) {
        availableLabels.filter {
            !it.name.startsWith("s:") && !it.name.startsWith("p:") && !it.name.startsWith("c:") && !it.name.startsWith("t:") &&
                    !it.name.startsWith("s:") && it.name != "mobile-entry" && it.name != "routine"
        }
    }

    fun selectSingle(label: Label, groupPrefix: String) {
        val others = selectedLabels.filter { !it.name.startsWith(groupPrefix) }
        selectedLabels = others + label
    }

    val dayScrollState = rememberScrollState()
    val contentScrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f)
            .padding(24.dp).navigationBarsPadding().verticalScroll(contentScrollState)
    ) {
        Text("New Task", style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = title, onValueChange = { title = it },
            label = { Text("Task Title") }, modifier = Modifier.fillMaxWidth(), singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryAccent, unfocusedBorderColor = TextSecondary,
                focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                focusedLabelColor = PrimaryAccent, unfocusedLabelColor = TextSecondary, cursorColor = PrimaryAccent
            )
        )
        Spacer(modifier = Modifier.height(16.dp))

        SingleSelectLabelSection("Status", statusLabels, selectedLabels, { selectSingle(it, "s:") }, "s:")
        SingleSelectLabelSection("Priority", priorityLabels, selectedLabels, { selectSingle(it, "p:") }, "p:")
        SingleSelectLabelSection("Time", timeLabels, selectedLabels, { selectSingle(it, "t:") }, "t:")
        SingleSelectLabelSection("Context", contextLabels, selectedLabels, { selectSingle(it, "c:") }, "c:")
        MultiSelectLabelSection("Other Labels", otherLabels, selectedLabels, { l ->
            selectedLabels = if (selectedLabels.contains(l)) selectedLabels - l else selectedLabels + l
        })

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = DividerColor)
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Repeat Task (Routine)", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
            Switch(
                checked = isRoutine, onCheckedChange = { isRoutine = it },
                colors = SwitchDefaults.colors(checkedThumbColor = SurfaceColor, checkedTrackColor = PrimaryAccent, uncheckedThumbColor = TextSecondary, uncheckedTrackColor = AppBackground)
            )
        }

        AnimatedVisibility(visible = isRoutine, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(dayScrollState), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DayOfWeek.values().forEach { day ->
                        val selected = selectedDays.contains(day)
                        FilterChip(
                            selected = selected, onClick = { selectedDays = if (selected) selectedDays - day else selectedDays + day },
                            label = { Text(day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = PrimaryAccent, selectedLabelColor = SurfaceColor, containerColor = SurfaceColor, labelColor = TextPrimary),
                            border = FilterChipDefaults.filterChipBorder(borderColor = if (selected) PrimaryAccent else DividerColor, selected = selected, enabled = true)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onCancel, colors = ButtonDefaults.textButtonColors(contentColor = TextSecondary)) { Text("Cancel") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = { onSubmit(title, isRoutine, selectedDays, selectedLabels) },
                enabled = title.isNotBlank() && (!isRoutine || selectedDays.isNotEmpty()),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryAccent, contentColor = SurfaceColor, disabledContainerColor = DividerColor, disabledContentColor = TextSecondary)
            ) {
                Text(if (isRoutine) "Add Routine" else "Add Task")
            }
        }
    }
}