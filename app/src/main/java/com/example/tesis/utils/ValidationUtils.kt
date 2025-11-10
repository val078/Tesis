package com.example.tesis.utils

object ValidationUtils {


    fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return false

        val emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+"
        return email.matches(emailPattern.toRegex())
    }


    fun isValidPassword(password: String): Boolean {
        return password.length >= 6
    }


    fun isValidName(name: String): Boolean {
        return name.isNotBlank() && name.length >= 2
    }


    fun isValidAge(age: Int): Boolean {
        return age in 8..100
    }
}