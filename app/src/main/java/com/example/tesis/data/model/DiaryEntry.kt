package com.example.tesis.data.model

import com.google.firebase.Timestamp

data class DiaryEntry(
    val id: String = "",
    val userId: String = "",
    val date: String = "", // "Lunes 28 de septiembre"
    val moment: String = "", // "Desayuno", "Almuerzo", etc.
    val sticker: String = "", // "🍎"
    val description: String = "",
    val rating: String = "", // "Bueno", "Malo", "Regular"
    val timestamp: Timestamp = Timestamp.now()
)