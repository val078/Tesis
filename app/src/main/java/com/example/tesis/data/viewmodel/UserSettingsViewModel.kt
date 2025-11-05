// Crea: viewmodel/UserSettingsViewModel.kt
package com.example.tesis.data.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tesis.notifications.NotificationSettings
import com.example.tesis.notifications.NotificationScheduler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class UserSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val context: Context = application.applicationContext

    // Estado de configuración
    private val _notificationSettings = MutableStateFlow(NotificationSettings())
    val notificationSettings: StateFlow<NotificationSettings> = _notificationSettings

    // Estado de carga
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    // Estado de éxito al guardar
    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess

    init {
        loadSettings()
    }

    // 🔹 Carga la configuración desde Firestore
    fun loadSettings() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                Log.d("UserSettingsVM", "🔄 Cargando configuración...")

                val userId = auth.currentUser?.uid ?: return@launch

                val doc = firestore.collection("users")
                    .document(userId)
                    .collection("settings")
                    .document("notifications")
                    .get()
                    .await()

                if (doc.exists()) {
                    // ✅ Configuración existente
                    val settings = doc.toObject(NotificationSettings::class.java)
                        ?: NotificationSettings()
                    _notificationSettings.value = settings
                    Log.d("UserSettingsVM", "✅ Configuración cargada: $settings")
                } else {
                    // 🆕 Crear configuración por defecto sin mostrar popup
                    val defaultSettings = NotificationSettings()
                    _notificationSettings.value = defaultSettings
                    saveSettings(defaultSettings, silent = true) // 🧩 guardado inicial silencioso
                    Log.d("UserSettingsVM", "✅ Configuración por defecto creada")
                }

            } catch (e: Exception) {
                Log.e("UserSettingsVM", "❌ Error cargando configuración", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    // 💾 Guarda la configuración en Firestore
    // Parámetro `silent` evita que se dispare el popup en guardados automáticos
    fun saveSettings(settings: NotificationSettings, silent: Boolean = false) {
        viewModelScope.launch {
            try {
                Log.d("UserSettingsVM", "💾 Guardando configuración...")

                val userId = auth.currentUser?.uid ?: return@launch

                val updatedSettings = settings.copy(
                    lastUpdated = System.currentTimeMillis()
                )

                firestore.collection("users")
                    .document(userId)
                    .collection("settings")
                    .document("notifications")
                    .set(updatedSettings)
                    .await()

                _notificationSettings.value = updatedSettings

                // 🔔 Reprogramar notificaciones locales
                NotificationScheduler.scheduleAllNotifications(context, updatedSettings)

                if (!silent) {
                    _saveSuccess.value = true
                }

                Log.d(
                    "UserSettingsVM",
                    "✅ Configuración guardada${if (silent) " (modo silencioso)" else ""}"
                )

            } catch (e: Exception) {
                Log.e("UserSettingsVM", "❌ Error guardando configuración", e)
            }
        }
    }

    // 🔄 Métodos para actualizar campos individuales
    fun updateBreakfastEnabled(enabled: Boolean) {
        _notificationSettings.value = _notificationSettings.value.copy(breakfastEnabled = enabled)
    }

    fun updateLunchEnabled(enabled: Boolean) {
        _notificationSettings.value = _notificationSettings.value.copy(lunchEnabled = enabled)
    }

    fun updateSnackEnabled(enabled: Boolean) {
        _notificationSettings.value = _notificationSettings.value.copy(snackEnabled = enabled)
    }

    fun updatePlayEnabled(enabled: Boolean) {
        _notificationSettings.value = _notificationSettings.value.copy(playEnabled = enabled)
    }

    fun updateBreakfastTime(time: String) {
        _notificationSettings.value = _notificationSettings.value.copy(breakfastTime = time)
    }

    fun updateLunchTime(time: String) {
        _notificationSettings.value = _notificationSettings.value.copy(lunchTime = time)
    }

    fun updateSnackTime(time: String) {
        _notificationSettings.value = _notificationSettings.value.copy(snackTime = time)
    }

    fun updatePlayTime(time: String) {
        _notificationSettings.value = _notificationSettings.value.copy(playTime = time)
    }

    fun toggleAllNotifications(enabled: Boolean) {
        _notificationSettings.value = _notificationSettings.value.copy(enabled = enabled)
    }

    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }
}