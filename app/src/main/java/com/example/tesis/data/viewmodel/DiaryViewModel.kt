package com.example.tesis.data.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tesis.data.model.DiaryEntry
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.tesis.data.model.FoodEntry
import com.google.firebase.Timestamp

class DiaryViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _foodEntries = MutableStateFlow<Map<String, List<FoodEntry>>>(emptyMap())
    val foodEntries: StateFlow<Map<String, List<FoodEntry>>> = _foodEntries

    private val _allEntries = MutableStateFlow<List<DiaryEntry>>(emptyList())
    val allEntries: StateFlow<List<DiaryEntry>> = _allEntries

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _entrySavedEvent = MutableStateFlow(false)
    val entrySavedEvent: StateFlow<Boolean> = _entrySavedEvent

    private var entriesListener: ListenerRegistration? = null

    init {
        startListeningToEntries()
    }

    fun resetEntrySavedEvent() {
        _entrySavedEvent.value = false
    }

    private fun startListeningToEntries() {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Log.e("DiaryViewModel", "❌ No hay usuario autenticado")
            _foodEntries.value = emptyMap()
            _allEntries.value = emptyList()
            return
        }

        Log.d("DiaryViewModel", "✅ Iniciando listener para userId: $userId")

        entriesListener = firestore.collection("diaryEntries")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("DiaryViewModel", "❌ Error escuchando entradas: ${error.message}")

                    if (error is FirebaseFirestoreException &&
                        error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        Log.d("DiaryViewModel", "🛑 Deteniendo listener por permisos denegados")
                        stopListening()
                    }
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    Log.d("DiaryViewModel", "📦 Documentos recibidos: ${snapshot.documents.size}")

                    val entriesMap = mutableMapOf<String, MutableList<FoodEntry>>()

                    snapshot.documents.forEachIndexed { index, doc ->
                        try {
                            val date = doc.getString("date") ?: ""

                            // ⭐ CAMBIO: Normalizar fecha para evitar problemas de espacios
                            val normalizedDate = date.trim()

                            Log.d("DiaryViewModel", "📅 Doc $index - date: '$normalizedDate'")
                            Log.d("DiaryViewModel", "   - moment: ${doc.getString("moment")}")
                            Log.d("DiaryViewModel", "   - sticker: ${doc.getString("sticker")}")

                            val entry = FoodEntry(
                                date = normalizedDate,
                                moment = doc.getString("moment") ?: "",
                                sticker = doc.getString("sticker") ?: "",
                                description = doc.getString("description") ?: "",
                                rating = doc.getString("rating") ?: ""
                            )

                            if (!entriesMap.containsKey(normalizedDate)) {
                                entriesMap[normalizedDate] = mutableListOf()
                            }
                            entriesMap[normalizedDate]?.add(entry)

                        } catch (e: Exception) {
                            Log.e("DiaryViewModel", "❌ Error parsing document: ${e.message}")
                            e.printStackTrace()
                        }
                    }

                    // ⭐ NUEVO: Ordenar entradas dentro de cada fecha por momento
                    val momentOrder = listOf("Desayuno", "Almuerzo", "Merienda", "Snack")
                    entriesMap.forEach { (date, entries) ->
                        entries.sortBy { entry ->
                            momentOrder.indexOf(entry.moment).takeIf { it != -1 } ?: Int.MAX_VALUE
                        }
                    }

                    _foodEntries.value = entriesMap

                    Log.d("DiaryViewModel", "✅ Fechas en memoria: ${entriesMap.keys.joinToString()}")
                    Log.d("DiaryViewModel", "✅ Total de fechas: ${entriesMap.size}")

                    // ⭐ NUEVO: Log de detalle para debugging
                    entriesMap.forEach { (date, entries) ->
                        Log.d("DiaryViewModel", "   '$date' → ${entries.size} entradas")
                    }
                }
            }
    }

    fun stopListening() {
        entriesListener?.remove()
        entriesListener = null
        _foodEntries.value = emptyMap()
        _allEntries.value = emptyList()
        _isLoading.value = false
        _entrySavedEvent.value = false
        Log.d("DiaryViewModel", "🛑 Listener detenido y datos limpiados")
    }

    fun restartListening() {
        Log.d("DiaryViewModel", "🔄 Reiniciando listener...")
        stopListening()
        startListeningToEntries()
    }

    fun loadAllEntries() {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Log.e("DiaryViewModel", "❌ No hay usuario autenticado")
            _allEntries.value = emptyList()
            return
        }

        _isLoading.value = true
        Log.d("DiaryViewModel", "📚 Cargando todas las entradas para historial...")

        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("diaryEntries")
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()

                val entries = snapshot.documents.mapNotNull { doc ->
                    try {
                        DiaryEntry(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            date = doc.getString("date")?.trim() ?: "", // ⭐ Normalizar
                            moment = doc.getString("moment") ?: "",
                            sticker = doc.getString("sticker") ?: "",
                            description = doc.getString("description") ?: "",
                            rating = doc.getString("rating") ?: "",
                            timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now()
                        )
                    } catch (e: Exception) {
                        Log.e("DiaryViewModel", "❌ Error parsing entry: ${e.message}")
                        null
                    }
                }

                _allEntries.value = entries
                Log.d("DiaryViewModel", "✅ Cargadas ${entries.size} entradas para historial")

            } catch (e: Exception) {
                Log.e("DiaryViewModel", "❌ Error cargando entradas: ${e.message}")
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveFoodEntry(entry: FoodEntry) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                // ⭐ CAMBIO: Normalizar fecha antes de guardar
                val normalizedDate = entry.date.trim()

                Log.d("DiaryViewModel", "💾 Guardando entrada con fecha: '$normalizedDate'")

                val diaryEntry = hashMapOf(
                    "userId" to userId,
                    "date" to normalizedDate,
                    "moment" to entry.moment,
                    "sticker" to entry.sticker,
                    "description" to entry.description,
                    "rating" to entry.rating,
                    "timestamp" to com.google.firebase.Timestamp.now()
                )

                val result = firestore.collection("diaryEntries")
                    .add(diaryEntry)
                    .await()

                Log.d("DiaryViewModel", "✅ Entrada guardada con ID: ${result.id}")

                // ⭐ NO necesitas loadAllEntries() porque el SnapshotListener se encarga
                // loadAllEntries()

                _entrySavedEvent.value = true

            } catch (e: Exception) {
                Log.e("DiaryViewModel", "❌ Error guardando entrada: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun updateFoodEntry(date: String, originalMoment: String, updatedEntry: FoodEntry) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                // ⭐ CAMBIO: Normalizar fecha
                val normalizedDate = date.trim()

                Log.d("DiaryViewModel", "✏️ Actualizando entrada: $normalizedDate - $originalMoment")

                val updates = hashMapOf<String, Any>(
                    "moment" to updatedEntry.moment,
                    "sticker" to updatedEntry.sticker,
                    "description" to updatedEntry.description,
                    "rating" to updatedEntry.rating,
                    "timestamp" to com.google.firebase.Timestamp.now() // ⭐ Actualizar timestamp
                )

                val documents = firestore.collection("diaryEntries")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("date", normalizedDate)
                    .whereEqualTo("moment", originalMoment)
                    .get()
                    .await()

                if (documents.documents.isNotEmpty()) {
                    documents.documents.first().reference.update(updates).await()
                    Log.d("DiaryViewModel", "✅ Entrada actualizada exitosamente")

                    // ⭐ NO necesitas loadAllEntries() porque el SnapshotListener se encarga
                    // loadAllEntries()
                } else {
                    Log.e("DiaryViewModel", "❌ No se encontró la entrada para actualizar")
                    Log.e("DiaryViewModel", "   Buscando: date='$normalizedDate', moment='$originalMoment'")
                }

            } catch (e: Exception) {
                Log.e("DiaryViewModel", "❌ Error actualizando entrada: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun deleteFoodEntry(date: String, moment: String) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            try {
                // ⭐ CAMBIO: Normalizar fecha
                val normalizedDate = date.trim()

                Log.d("DiaryViewModel", "🗑️ Eliminando entrada: $normalizedDate - $moment")

                val documents = firestore.collection("diaryEntries")
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("date", normalizedDate)
                    .whereEqualTo("moment", moment)
                    .get()
                    .await()

                if (documents.documents.isNotEmpty()) {
                    documents.documents.forEach { doc ->
                        doc.reference.delete().await()
                    }
                    Log.d("DiaryViewModel", "✅ Entrada eliminada exitosamente (${documents.size()} docs)")

                    // ⭐ NO necesitas loadAllEntries() porque el SnapshotListener se encarga
                    // loadAllEntries()
                } else {
                    Log.e("DiaryViewModel", "❌ No se encontró la entrada para eliminar")
                    Log.e("DiaryViewModel", "   Buscando: date='$normalizedDate', moment='$moment'")
                }

            } catch (e: Exception) {
                Log.e("DiaryViewModel", "❌ Error eliminando entrada: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun getFoodEntriesForDate(date: String): List<FoodEntry> {
        // ⭐ CAMBIO: Normalizar fecha antes de buscar
        val normalizedDate = date.trim()
        val entries = _foodEntries.value[normalizedDate] ?: emptyList()

        Log.d("DiaryViewModel", "🔍 Buscando entradas para: '$normalizedDate'")
        Log.d("DiaryViewModel", "   Encontradas: ${entries.size}")

        if (entries.isEmpty()) {
            Log.d("DiaryViewModel", "   Fechas disponibles:")
            _foodEntries.value.keys.forEach { key ->
                Log.d("DiaryViewModel", "      - '$key' (${_foodEntries.value[key]?.size} entradas)")
            }
        }

        return entries
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
        Log.d("DiaryViewModel", "🧹 ViewModel limpiado completamente")
    }
}