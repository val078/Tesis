package com.example.tesis.utils

object ScoreCalculator {

    fun calculateScore(
        correctAnswers: Int,
        totalQuestions: Int,
        timeBonus: Int = 0
    ): Int {
        if (totalQuestions == 0) return 0

        val baseScore = (correctAnswers * 100) / totalQuestions
        return baseScore + timeBonus
    }


    fun calculateTimeBonus(timeInSeconds: Int, maxTime: Int): Int {
        if (timeInSeconds >= maxTime) return 0

        val timeLeft = maxTime - timeInSeconds
        return (timeLeft * 0.5).toInt() // 0.5 puntos por segundo ahorrado
    }
}