package com.example.tesis.notifications

data class NotificationSettings(
    val enabled: Boolean = true,
    val breakfastEnabled: Boolean = true,
    val lunchEnabled: Boolean = true,
    val snackEnabled: Boolean = true,
    val playEnabled: Boolean = true,

    val breakfastTime: String = "09:00", // HH:mm
    val lunchTime: String = "13:00",
    val snackTime: String = "19:00",
    val playTime: String = "16:00",

    val lastUpdated: Long = System.currentTimeMillis()
)