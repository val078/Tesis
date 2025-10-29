package com.example.tesis.admin.config

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tesis.admin.config.AppConfig
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AppConfigViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _config = MutableStateFlow(AppConfig())
    val config: StateFlow<AppConfig> = _config

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess

    fun loadConfig() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d("AppConfigViewModel", "════════════════════════════════")
                Log.d("AppConfigViewModel", "⚙️ Cargando configuración de la app...")

                val doc = firestore.collection("config")
                    .document("app")
                    .get()
                    .await()

                Log.d("AppConfigViewModel", "📄 Documento existe: ${doc.exists()}")

                if (doc.exists()) {
                    val config = doc.toObject(AppConfig::class.java)
                    if (config != null) {
                        _config.value = config
                        Log.d("AppConfigViewModel", "✅ Configuración cargada:")
                        Log.d("AppConfigViewModel", "   - Version: ${config.appVersion}")
                        Log.d("AppConfigViewModel", "   - Mantenimiento: ${config.maintenanceMode}")
                        Log.d("AppConfigViewModel", "   - Registros: ${config.allowNewRegistrations}")
                    } else {
                        Log.e("AppConfigViewModel", "❌ No se pudo parsear AppConfig")
                        _config.value = AppConfig()
                    }
                } else {
                    Log.w("AppConfigViewModel", "⚠️ Documento no existe, usando valores por defecto")
                    _config.value = AppConfig()
                }

                Log.d("AppConfigViewModel", "════════════════════════════════")
            } catch (e: Exception) {
                Log.e("AppConfigViewModel", "❌ ERROR:", e)
                e.printStackTrace()
                _config.value = AppConfig()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveConfig(newConfig: AppConfig) {
        viewModelScope.launch {
            try {
                Log.d("AppConfigViewModel", "💾 Guardando configuración...")

                val adminEmail = auth.currentUser?.email ?: "admin"

                val configToSave = newConfig.copy(
                    lastUpdated = Timestamp.now(),
                    updatedBy = adminEmail
                )

                Log.d("AppConfigViewModel", "📤 Datos a guardar:")
                Log.d("AppConfigViewModel", "   - appVersion: ${configToSave.appVersion}")
                Log.d("AppConfigViewModel", "   - maintenanceMode: ${configToSave.maintenanceMode}")
                Log.d("AppConfigViewModel", "   - allowNewRegistrations: ${configToSave.allowNewRegistrations}")

                firestore.collection("config")
                    .document("app")
                    .set(configToSave)
                    .await()

                _config.value = configToSave
                _saveSuccess.value = true
                Log.d("AppConfigViewModel", "✅ Configuración guardada exitosamente")
            } catch (e: Exception) {
                Log.e("AppConfigViewModel", "❌ ERROR al guardar:", e)
                e.printStackTrace()
                _saveSuccess.value = false
            }
        }
    }

    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }
}