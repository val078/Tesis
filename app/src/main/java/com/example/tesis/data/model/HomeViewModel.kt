package com.example.tesis.data.model

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tesis.data.repository.AIRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel : ViewModel() {
    // ✅ CAMBIO: Usar singleton en vez de crear nueva instancia
    private val aiRepository = AIRepository.getInstance()

    private val _aiRecommendation = MutableStateFlow<AIState>(AIState.Idle)
    val aiRecommendation: StateFlow<AIState> = _aiRecommendation

    private var hasLoadedOnce = false

    fun loadAIRecommendation(userId: String, forceRefresh: Boolean = false) {
        if (hasLoadedOnce && !forceRefresh && _aiRecommendation.value is AIState.Success) {
            Log.d("HomeViewModel", "♻️ Ya hay recomendación en ViewModel, no se recarga")
            return
        }

        Log.d("HomeViewModel", "🔄 Cargando recomendación para userId: $userId")

        viewModelScope.launch {
            _aiRecommendation.value = AIState.Loading

            aiRepository.getAIRecommendation(userId, forceRefresh).fold(
                onSuccess = { recommendation ->
                    Log.d("HomeViewModel", "✅ Recomendación obtenida exitosamente")
                    _aiRecommendation.value = AIState.Success(recommendation)
                    hasLoadedOnce = true
                },
                onFailure = { error ->
                    Log.e("HomeViewModel", "❌ Error: ${error.message}")
                    _aiRecommendation.value = AIState.Error(
                        error.message ?: "Error al obtener recomendación"
                    )
                }
            )
        }
    }

    fun refreshRecommendation(userId: String) {
        Log.d("HomeViewModel", "🔄 Refrescando recomendación (forzado)...")
        aiRepository.invalidateCache()
        loadAIRecommendation(userId, forceRefresh = true)
    }
}

sealed class AIState {
    object Idle : AIState()
    object Loading : AIState()
    data class Success(val recommendation: String) : AIState()
    data class Error(val message: String) : AIState()
}