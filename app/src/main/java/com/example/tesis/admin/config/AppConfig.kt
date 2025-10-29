package com.example.tesis.admin.config

import com.google.firebase.Timestamp

data class AppConfig(
    val appVersion: String = "1.0.0",
    val maintenanceMode: Boolean = false,
    val maintenanceMessage: String = "Estamos mejorando la app. Vuelve pronto 🚀",
    val allowNewRegistrations: Boolean = true,
    val lastUpdated: Timestamp = Timestamp.now(),
    val updatedBy: String = ""
)