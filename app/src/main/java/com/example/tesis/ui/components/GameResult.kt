package com.example.tesis.ui.components

import com.google.firebase.Timestamp

data class GameResult(
    val gameId: String = "", // 👈 Añade esto
    val score: Int = 0,
    val date: Timestamp = Timestamp.now(),
    val correctAnswers: Int = 0,
    val totalQuestions: Int = 0,
    val timeLeft: Int = 0,
    val streak: Int = 0,
    val extraData: Map<String, Any> = emptyMap()
) {
    val accuracy: Int
        get() = if (totalQuestions > 0) {
            ((correctAnswers.toDouble() / totalQuestions) * 100).toInt()
        } else {
            0
        }
}