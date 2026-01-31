package com.example.mobiletaskmanager.ui.components

import androidx.compose.ui.graphics.Color

fun String.toColor(): Color {
    return try {
        Color(android.graphics.Color.parseColor("#$this"))
    } catch (e: Exception) {
        Color.Gray
    }
}