package com.example.tesis.admin.edit

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tesis.data.model.DiaryEntry
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminFoodHistoryViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private val _entries = MutableStateFlow<List<DiaryEntry>>(emptyList())
    val entries: StateFlow<List<DiaryEntry>> = _entries

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    fun loadEntries(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                Log.d("AdminFoodHistory", "🔍 Cargando entradas para userId: $userId")

                val snapshot = firestore.collection("diaryEntries")
                    .whereEqualTo("userId", userId)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .get()
                    .await()

                Log.d("AdminFoodHistory", "📦 Documentos encontrados: ${snapshot.documents.size}")

                // ✅ CORRECCIÓN: Mapear manualmente para incluir el ID
                val entriesList = snapshot.documents.mapNotNull { doc ->
                    try {
                        DiaryEntry(
                            id = doc.id, // ⭐ IMPORTANTE: Asignar el ID del documento
                            userId = doc.getString("userId") ?: "",
                            date = doc.getString("date") ?: "",
                            moment = doc.getString("moment") ?: "",
                            sticker = doc.getString("sticker") ?: "",
                            description = doc.getString("description") ?: "",
                            rating = doc.getString("rating") ?: "",
                            timestamp = doc.getTimestamp("timestamp")
                                ?: Timestamp.now()
                        )
                    } catch (e: Exception) {
                        Log.e("AdminFoodHistory", "❌ Error parseando entrada: ${e.message}")
                        null
                    }
                }

                _entries.value = entriesList
                Log.d("AdminFoodHistory", "✅ Entradas cargadas: ${entriesList.size}")

                // 🔍 Debug: Mostrar las primeras 3 entradas
                entriesList.take(3).forEachIndexed { index, entry ->
                    Log.d("AdminFoodHistory", "  $index: ${entry.moment} - ${entry.description.take(30)}...")
                }

            } catch (e: Exception) {
                Log.e("AdminFoodHistory", "❌ Error cargando entradas: ${e.message}")
                e.printStackTrace()
                _entries.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deleteEntry(entryId: String) {
        viewModelScope.launch {
            try {
                Log.d("AdminFoodHistory", "🗑️ Eliminando entrada con ID: $entryId")

                firestore.collection("diaryEntries")
                    .document(entryId)
                    .delete()
                    .await()

                Log.d("AdminFoodHistory", "✅ Entrada eliminada exitosamente")

                // Refrescar la lista sin necesidad de buscar el userId
                val currentUserId = _entries.value.firstOrNull()?.userId
                if (currentUserId != null) {
                    loadEntries(currentUserId)
                }
            } catch (e: Exception) {
                Log.e("AdminFoodHistory", "❌ Error eliminando entrada: ${e.message}")
                e.printStackTrace()
            }
        }
    }
}