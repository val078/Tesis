package com.example.tesis.utils

import org.junit.Test
import org.junit.Assert.*

class ValidationUtilsTest {

    @Test
    fun validEmailShouldReturnTrue() {
        val email = "test@example.com"
        val result = ValidationUtils.isValidEmail(email)
        assertTrue(result)
    }

    @Test
    fun emailWithoutAtShouldReturnFalse() {
        val email = "testexample.com"
        val result = ValidationUtils.isValidEmail(email)
        assertFalse(result)
    }

    @Test
    fun emptyEmailShouldReturnFalse() {
        val email = ""
        val result = ValidationUtils.isValidEmail(email)
        assertFalse(result)
    }

    @Test
    fun emailWithSpacesShouldReturnFalse() {
        val email = "test @example.com"
        val result = ValidationUtils.isValidEmail(email)
        assertFalse(result)
    }

    @Test
    fun passwordWithLessThan6CharactersShouldReturnFalse() {
        val password = "12345"
        val result = ValidationUtils.isValidPassword(password)
        assertFalse(result)
    }

    @Test
    fun passwordWith6OrMoreCharactersShouldReturnTrue() {
        val password = "123456"
        val result = ValidationUtils.isValidPassword(password)
        assertTrue(result)
    }

    @Test
    fun emptyPasswordShouldReturnFalse() {
        val password = ""
        val result = ValidationUtils.isValidPassword(password)
        assertFalse(result)
    }

    @Test
    fun validNameShouldReturnTrue() {
        val name = "Valentina"
        val result = ValidationUtils.isValidName(name)
        assertTrue(result)
    }

    @Test
    fun emptyNameShouldReturnFalse() {
        val name = ""
        val result = ValidationUtils.isValidName(name)
        assertFalse(result)
    }

    @Test
    fun singleCharacterNameShouldReturnFalse() {
        val name = "A"
        val result = ValidationUtils.isValidName(name)
        assertFalse(result)
    }

    @Test
    fun validAgeShouldReturnTrue() {
        val age = 10
        val result = ValidationUtils.isValidAge(age)
        assertTrue(result)
    }

    @Test
    fun ageBelowMinimumShouldReturnFalse() {
        val age = 5
        val result = ValidationUtils.isValidAge(age)
        assertFalse(result)
    }

    @Test
    fun ageAboveMaximumShouldReturnFalse() {
        val age = 150
        val result = ValidationUtils.isValidAge(age)
        assertFalse(result)
    }
}