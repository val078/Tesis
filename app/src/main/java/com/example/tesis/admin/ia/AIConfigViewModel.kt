package com.example.tesis.admin.ia

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tesis.admin.ia.AIConfig
import com.example.tesis.admin.ia.AIRecommendationLog
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AIConfigViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _config = MutableStateFlow(AIConfig())
    val config: StateFlow<AIConfig> = _config

    private val _logs = MutableStateFlow<List<AIRecommendationLog>>(emptyList())
    val logs: StateFlow<List<AIRecommendationLog>> = _logs

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess

    fun loadConfig() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d("AIConfigViewModel", "════════════════════════════════")
                Log.d("AIConfigViewModel", "🔍 INICIANDO CARGA DE CONFIGURACIÓN")
                Log.d("AIConfigViewModel", "👤 Usuario actual UID: ${auth.currentUser?.uid}")
                Log.d("AIConfigViewModel", "📧 Email: ${auth.currentUser?.email}")

                // Verificar rol de admin
                val userId = auth.currentUser?.uid
                if (userId != null) {
                    val userDoc = firestore.collection("users")
                        .document(userId)
                        .get()
                        .await()

                    val role = userDoc.getString("role")
                    Log.d("AIConfigViewModel", "🔐 Role del usuario: $role")

                    if (role != "admin") {
                        Log.e("AIConfigViewModel", "❌ Usuario NO es admin")
                    }
                }

                // Intentar cargar configuración
                Log.d("AIConfigViewModel", "📥 Obteniendo documento config/ai...")
                val doc = firestore.collection("config")
                    .document("ai")
                    .get()
                    .await()

                Log.d("AIConfigViewModel", "📄 Documento existe: ${doc.exists()}")
                Log.d("AIConfigViewModel", "📄 Datos del documento: ${doc.data}")

                if (doc.exists()) {
                    val config = doc.toObject(AIConfig::class.java)
                    if (config != null) {
                        _config.value = config
                        Log.d("AIConfigViewModel", "✅ Configuración parseada correctamente:")
                        Log.d("AIConfigViewModel", "   - enabled: ${config.enabled}")
                        Log.d("AIConfigViewModel", "   - maxResponseLength: ${config.maxResponseLength}")
                        Log.d("AIConfigViewModel", "   - systemPrompt: ${config.systemPrompt.take(50)}...")
                    } else {
                        Log.e("AIConfigViewModel", "❌ No se pudo parsear a AIConfig")
                        _config.value = AIConfig()
                    }
                } else {
                    Log.e("AIConfigViewModel", "❌ Documento config/ai NO existe")
                    _config.value = AIConfig()
                }

                Log.d("AIConfigViewModel", "════════════════════════════════")
            } catch (e: Exception) {
                Log.e("AIConfigViewModel", "❌ ERROR COMPLETO:")
                Log.e("AIConfigViewModel", "   Tipo: ${e.javaClass.simpleName}")
                Log.e("AIConfigViewModel", "   Mensaje: ${e.message}")
                e.printStackTrace()
                _config.value = AIConfig()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadLogs(limit: Int = 20) {
        viewModelScope.launch {
            try {
                Log.d("AIConfigViewModel", "📊 Cargando logs de IA...")

                val snapshot = firestore.collection("aiLogs")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(limit.toLong())
                    .get()
                    .await()

                Log.d("AIConfigViewModel", "📊 Logs encontrados: ${snapshot.size()}")

                _logs.value = snapshot.documents.mapNotNull { doc ->
                    try {
                        doc.toObject(AIRecommendationLog::class.java)?.copy(id = doc.id)
                    } catch (e: Exception) {
                        Log.e("AIConfigViewModel", "Error parseando log ${doc.id}: ${e.message}")
                        null
                    }
                }

                Log.d("AIConfigViewModel", "✅ ${_logs.value.size} logs cargados correctamente")
            } catch (e: Exception) {
                Log.e("AIConfigViewModel", "❌ Error cargando logs:")
                Log.e("AIConfigViewModel", "   Tipo: ${e.javaClass.simpleName}")
                Log.e("AIConfigViewModel", "   Mensaje: ${e.message}")
                e.printStackTrace()
                _logs.value = emptyList()
            }
        }
    }

    fun saveConfig(newConfig: AIConfig) {
        viewModelScope.launch {
            try {
                Log.d("AIConfigViewModel", "💾 GUARDANDO CONFIGURACIÓN...")

                val adminEmail = auth.currentUser?.email ?: "admin"

                val configToSave = newConfig.copy(
                    lastUpdated = Timestamp.now(),
                    updatedBy = adminEmail
                )

                Log.d("AIConfigViewModel", "📤 Datos a guardar:")
                Log.d("AIConfigViewModel", "   - enabled: ${configToSave.enabled}")
                Log.d("AIConfigViewModel", "   - maxResponseLength: ${configToSave.maxResponseLength}")
                Log.d("AIConfigViewModel", "   - systemPrompt: ${configToSave.systemPrompt.take(50)}...")
                Log.d("AIConfigViewModel", "   - updatedBy: ${configToSave.updatedBy}")

                firestore.collection("config")
                    .document("ai")
                    .set(configToSave)
                    .await()

                _config.value = configToSave
                _saveSuccess.value = true
                Log.d("AIConfigViewModel", "✅✅✅ CONFIGURACIÓN GUARDADA EXITOSAMENTE")
            } catch (e: Exception) {
                Log.e("AIConfigViewModel", "❌ ERROR AL GUARDAR:")
                Log.e("AIConfigViewModel", "   Tipo: ${e.javaClass.simpleName}")
                Log.e("AIConfigViewModel", "   Mensaje: ${e.message}")
                e.printStackTrace()
                _saveSuccess.value = false
            }
        }
    }

    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }
}