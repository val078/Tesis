// Crea: viewmodel/UserSettingsViewModel.kt
package com.example.tesis.data.model

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.tesis.data.model.NotificationSettings
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

    private val _notificationSettings = MutableStateFlow(NotificationSettings())
    val notificationSettings: StateFlow<NotificationSettings> = _notificationSettings

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess

    init {
        loadSettings()
    }

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
                    val settings = doc.toObject(NotificationSettings::class.java)
                        ?: NotificationSettings()
                    _notificationSettings.value = settings
                    Log.d("UserSettingsVM", "✅ Configuración cargada: $settings")
                } else {
                    // Crear configuración por defecto
                    val defaultSettings = NotificationSettings()
                    _notificationSettings.value = defaultSettings
                    saveSettings(defaultSettings)
                    Log.d("UserSettingsVM", "✅ Configuración por defecto creada")
                }

            } catch (e: Exception) {
                Log.e("UserSettingsVM", "❌ Error cargando configuración", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveSettings(settings: NotificationSettings) {
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

                // ⭐ Reprogramar notificaciones
                NotificationScheduler.scheduleAllNotifications(context, updatedSettings)

                _saveSuccess.value = true
                Log.d("UserSettingsVM", "✅ Configuración guardada y notificaciones reprogramadas")

            } catch (e: Exception) {
                Log.e("UserSettingsVM", "❌ Error guardando configuración", e)
            }
        }
    }

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