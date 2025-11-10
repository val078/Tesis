package com.example.tesis.utils

import org.junit.Test
import org.junit.Assert.*

class ScoreCalculatorTest {

    @Test
    fun allCorrectAnswersShouldGive100Points() {
        val correctAnswers = 10
        val totalQuestions = 10
        val timeBonus = 0

        val score = ScoreCalculator.calculateScore(correctAnswers, totalQuestions, timeBonus)

        assertEquals(100, score)
    }

    @Test
    fun halfCorrectAnswersShouldGive50Points() {
        val correctAnswers = 5
        val totalQuestions = 10
        val timeBonus = 0

        val score = ScoreCalculator.calculateScore(correctAnswers, totalQuestions, timeBonus)

        assertEquals(50, score)
    }

    @Test
    fun timeBonusShouldAddExtraPoints() {
        val correctAnswers = 10
        val totalQuestions = 10
        val timeBonus = 20

        val score = ScoreCalculator.calculateScore(correctAnswers, totalQuestions, timeBonus)

        assertEquals(120, score)
    }

    @Test
    fun zeroQuestionsAnsweredShouldGiveZeroPoints() {
        val correctAnswers = 0
        val totalQuestions = 10
        val timeBonus = 0

        val score = ScoreCalculator.calculateScore(correctAnswers, totalQuestions, timeBonus)

        assertEquals(0, score)
    }

    @Test
    fun zeroTotalQuestionsShouldGiveZeroPoints() {
        val correctAnswers = 5
        val totalQuestions = 0
        val timeBonus = 0

        val score = ScoreCalculator.calculateScore(correctAnswers, totalQuestions, timeBonus)

        assertEquals(0, score)
    }

    @Test
    fun fastCompletionShouldGiveTimeBonus() {
        val timeInSeconds = 30
        val maxTime = 60

        val bonus = ScoreCalculator.calculateTimeBonus(timeInSeconds, maxTime)

        assertTrue(bonus > 0)
    }

    @Test
    fun slowCompletionShouldGiveZeroBonus() {
        val timeInSeconds = 60
        val maxTime = 60

        val bonus = ScoreCalculator.calculateTimeBonus(timeInSeconds, maxTime)

        assertEquals(0, bonus)
    }

    @Test
    fun overtimeShouldGiveZeroBonus() {
        val timeInSeconds = 70
        val maxTime = 60

        val bonus = ScoreCalculator.calculateTimeBonus(timeInSeconds, maxTime)

        assertEquals(0, bonus)
    }
}