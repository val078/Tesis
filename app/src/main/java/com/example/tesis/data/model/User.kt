package com.example.tesis.data.model

import com.google.firebase.firestore.Exclude
import java.util.Date
import java.util.Calendar

data class User(
    val userId: String = "",
    var name: String = "",
    var email: String = "",
    var parentEmail: String? = null,
    var birthDate: Date? = null,
    val createdAt: Date = Date(),
    var role: String = "user",
    var active: Boolean = true,
    var avatar: String = "🦁", // ⭐ NUEVO: Emoji de avatar
    //var emailNotifications: EmailNotifications = EmailNotifications()
) {
    @get:Exclude
    val age: Int
        get() {
            if (birthDate == null) return 0
            val birthCalendar = Calendar.getInstance().apply { time = birthDate }
            val today = Calendar.getInstance()
            var years = today.get(Calendar.YEAR) - birthCalendar.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < birthCalendar.get(Calendar.DAY_OF_YEAR)) years--
            return years
        }

    @Exclude fun isChild(): Boolean = age < 18
    @Exclude fun needsParentEmail(): Boolean = isChild()
    @Exclude fun hasParentEmail(): Boolean = !parentEmail.isNullOrEmpty()
    @Exclude fun getDisplayName(): String = if (name.isNotEmpty()) name else email
    @Exclude fun getUserType(): String = when {
        role == "admin" -> "Administrador"
        isChild() -> "Niño/a"
        else -> "Adulto"
    }
}

data class AuthResult(
    val success: Boolean,
    val message: String,
    val user: User? = null
)