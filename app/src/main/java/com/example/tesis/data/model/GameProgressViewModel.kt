// ui/components/GameProgressViewModel.kt
package com.example.tesis.data.model

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tesis.data.repository.GameRepository
import com.example.tesis.ui.components.GameResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GameProgressViewModel : ViewModel() {
    private val repository = GameRepository()

    companion object {
        private const val TAG = "GameProgressVM"
    }

    private val _allGameResults = MutableStateFlow<List<GameResult>>(emptyList())
    val allGameResults: StateFlow<List<GameResult>> = _allGameResults

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun saveGameResult(gameId: String, result: GameResult) {
        Log.d(TAG, "=== saveGameResult LLAMADO ===")
        Log.d(TAG, "GameId: $gameId")
        Log.d(TAG, "Result: $result")

        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                Log.d(TAG, "Iniciando coroutine en viewModelScope")
                Log.d(TAG, "Thread actual: ${Thread.currentThread().name}")

                withContext(Dispatchers.IO) {
                    Log.d(TAG, "Dentro de withContext(IO)")
                    Log.d(TAG, "Llamando a repository.saveGameResult...")
                    repository.saveGameResult(gameId, result)
                    Log.d(TAG, "repository.saveGameResult completado")
                }

                // ✅ Actualizar la lista local: añadir el nuevo resultado
                val updatedList = _allGameResults.value.toMutableList()
                updatedList.add(result)
                _allGameResults.value = updatedList

                Log.d(TAG, "✅ Estado local actualizado correctamente")
                Log.d(TAG, "✅ GUARDADO COMPLETO")
            } catch (e: Exception) {
                Log.e(TAG, "❌ ERROR CRÍTICO al guardar resultado", e)
                _error.value = "Error al guardar el resultado: ${e.message}"
            } finally {
                _isLoading.value = false
                Log.d(TAG, "=== saveGameResult FINALIZADO ===")
            }
        }
    }

    fun loadAllGameResults() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val results = repository.getAllGameResults()
                _allGameResults.value = results
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar resultados", e)
                _error.value = "Error al cargar resultados"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getUserGameResults(userId: String, gameId: String): List<GameResult> {
        // Retorna los resultados del usuario para ese juego específico
        return _allGameResults.value.filter {
            it.gameId == gameId
        }
    }
}