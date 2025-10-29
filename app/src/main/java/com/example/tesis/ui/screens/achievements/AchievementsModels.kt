// ui/screens/achievements/AchievementsModels.kt
package com.example.tesis.ui.screens.achievements

import androidx.compose.ui.graphics.Color

data class AchievementData(
    val emoji: String,
    val name: String,
    val requirement: String,
    val unlocked: Boolean,
    val color: Color,
    val category: AchievementCategory
)

enum class AchievementCategory(val title: String, val emoji: String) {
    BEGINNER("Primeros Pasos", "🎮"),
    MASTERY("Maestría", "🏆"),
    STREAK("Racha", "🔥"),
    MRPOLLO("Mr. Pollo", "💖"),
    LEVEL("Nivel", "📈"),
    CONSISTENCY("Consistencia", "📅")
}

data class NextAchievementInfo(
    val achievement: AchievementData,
    val progress: Float,
    val progressText: String
)