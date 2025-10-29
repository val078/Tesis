package com.example.tesis.data.repository

import android.util.Log
import com.example.tesis.data.api.GeminiClient
import com.example.tesis.admin.ia.AIConfig
import com.example.tesis.admin.ia.AIRecommendationLog
import com.google.ai.client.generativeai.type.ServerException
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AIRepository private constructor() {
    private val firestore = FirebaseFirestore.getInstance()
    private val geminiModel = GeminiClient.model
    private val mutex = Mutex()

    private var cachedRecommendation: String? = null
    private var lastEntriesContent: String = ""
    private var lastUserId: String = ""
    private var isLoading: Boolean = false
    private var cachedConfig: AIConfig? = null

    suspend fun getAIRecommendation(userId: String, forceRefresh: Boolean = false): Result<String> {
        return mutex.withLock {
            try {
                Log.d("AIRepository", "🤖 Iniciando obtención de recomendación para userId: $userId")

                if (isLoading) {
                    Log.d("AIRepository", "⏳ Ya hay un request en proceso")
                    return@withLock Result.success(
                        cachedRecommendation ?: "Cargando recomendación..."
                    )
                }

                val config = getAIConfig()
                if (!config.enabled) {
                    Log.d("AIRepository", "⚠️ IA deshabilitada por configuración")
                    return@withLock Result.success(
                        "La IA está temporalmente deshabilitada. ¡Vuelve pronto! 🤖"
                    )
                }

                val diaryEntries = getTodayDiaryEntries(userId)
                Log.d("AIRepository", "📝 Entradas encontradas: ${diaryEntries.size}")

                diaryEntries.forEachIndexed { index, entry ->
                    Log.d("AIRepository", "   [$index]: '$entry'")
                }

                val currentContent = generateContentHash(diaryEntries)
                val hasChanges = currentContent != lastEntriesContent || userId != lastUserId

                Log.d("AIRepository", "🔍 Verificando caché:")
                Log.d("AIRepository", "   - forceRefresh: $forceRefresh")
                Log.d("AIRepository", "   - hasChanges: $hasChanges")
                Log.d("AIRepository", "   - cachedRecommendation: ${cachedRecommendation != null}")
                Log.d("AIRepository", "   - lastUserId: '$lastUserId'")
                Log.d("AIRepository", "   - currentUserId: '$userId'")
                Log.d("AIRepository", "   - lastEntriesContent: '$lastEntriesContent'")
                Log.d("AIRepository", "   - currentContent: '$currentContent'")

                // ⭐ NUEVO: Si no hay caché en memoria, intentar cargar desde Firestore
                if (cachedRecommendation == null && lastEntriesContent.isEmpty()) {
                    Log.d("AIRepository", "🔄 Caché vacío, intentando cargar última recomendación...")

                    val lastRecommendation = getLastSavedRecommendation(userId)

                    if (lastRecommendation != null) {
                        Log.d("AIRepository", "📜 Última recomendación encontrada en Firestore")

                        // ⭐ Restaurar caché desde Firestore
                        cachedRecommendation = lastRecommendation
                        lastEntriesContent = currentContent
                        lastUserId = userId

                        // ⭐ Recalcular hasChanges ahora que tenemos caché
                        val stillHasChanges = currentContent != lastEntriesContent || userId != lastUserId

                        if (!stillHasChanges) {
                            Log.d("AIRepository", "✅ Usando última recomendación guardada (sin cambios)")
                            return@withLock Result.success(lastRecommendation)
                        }
                    }
                }

                if (!forceRefresh && !hasChanges && cachedRecommendation != null) {
                    Log.d("AIRepository", "♻️ USANDO CACHÉ en memoria")
                    return@withLock Result.success(cachedRecommendation!!)
                }

                if (diaryEntries.isEmpty()) {
                    Log.d("AIRepository", "⚠️ No hay entradas del diario hoy")

                    val lastRecommendation = getLastSavedRecommendation(userId)

                    if (lastRecommendation != null) {
                        Log.d("AIRepository", "📜 Usando última recomendación guardada")
                        cachedRecommendation = lastRecommendation
                        lastEntriesContent = currentContent
                        lastUserId = userId
                        return@withLock Result.success(lastRecommendation)
                    } else {
                        val emptyMessage = "¡Hola! 👋 Aún no has escrito nada en tu diario hoy. " +
                                "Cuéntame qué has comido para poder darte recomendaciones saludables. 😊"
                        cachedRecommendation = emptyMessage
                        lastEntriesContent = currentContent
                        lastUserId = userId
                        return@withLock Result.success(emptyMessage)
                    }
                }

                isLoading = true
                Log.d("AIRepository", "🔄 Generando nueva recomendación")

                val prompt = createPrompt(diaryEntries, config.systemPrompt)
                Log.d("AIRepository", "📤 HACIENDO REQUEST A GEMINI")

                val aiResponse = generateContentWithRetry(prompt, maxRetries = 3)

                val limitedResponse = if (aiResponse.length > config.maxResponseLength) {
                    val trimmed = aiResponse.take(config.maxResponseLength)
                    val lastSpace = trimmed.lastIndexOf(' ')

                    val safeTrimmed = if (lastSpace > config.maxResponseLength - 50) {
                        trimmed.substring(0, lastSpace)
                    } else {
                        trimmed
                    }

                    cleanBrokenEmojis(safeTrimmed) + "..."
                } else {
                    aiResponse
                }

                Log.d("AIRepository", "✅ Respuesta recibida: ${limitedResponse.take(50)}...")
                Log.d("AIRepository", "   Longitud: ${limitedResponse.length} caracteres")

                saveLastRecommendation(userId, limitedResponse)
                saveRecommendationLog(userId, diaryEntries.joinToString("\n"), limitedResponse, prompt)

                cachedRecommendation = limitedResponse
                lastEntriesContent = currentContent
                lastUserId = userId
                isLoading = false

                Result.success(limitedResponse)

            } catch (e: Exception) {
                isLoading = false
                Log.e("AIRepository", "❌ Error: ${e.message}", e)

                val errorMessage = when {
                    e is ServerException && e.message?.contains("503") == true -> {
                        "🤖 El servidor de IA está muy ocupado en este momento. " +
                                "Por favor, intenta de nuevo en unos minutos. 😊"
                    }
                    e is ServerException && e.message?.contains("overloaded") == true -> {
                        "🤖 La IA está procesando muchas solicitudes. " +
                                "Intenta nuevamente en un momento. ⏳"
                    }
                    e.message?.contains("network") == true -> {
                        "📡 No hay conexión a internet. Revisa tu conexión. 📶"
                    }
                    else -> {
                        "😅 Hubo un problema al generar la recomendación. " +
                                "Intenta de nuevo más tarde."
                    }
                }

                val lastRecommendation = getLastSavedRecommendation(userId)
                if (lastRecommendation != null) {
                    Log.d("AIRepository", "📜 Usando última recomendación por error")
                    cachedRecommendation = lastRecommendation
                    return@withLock Result.success(lastRecommendation)
                }

                Result.failure(Exception(errorMessage))
            }
        }
    }

    // ⭐ FUNCIÓN PRIVADA CON REINTENTOS
    private suspend fun generateContentWithRetry(
        prompt: String,
        maxRetries: Int = 3
    ): String {
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                Log.d("AIRepository", "🔄 Intento ${attempt + 1} de $maxRetries")

                val response = geminiModel.generateContent(prompt)
                val text = response.text

                if (text != null && text.isNotBlank()) {
                    Log.d("AIRepository", "✅ Respuesta exitosa en intento ${attempt + 1}")
                    return text
                }

            } catch (e: ServerException) {
                lastException = e
                Log.e("AIRepository", "❌ Intento ${attempt + 1} falló: ${e.message}")

                if (e.message?.contains("503") == true ||
                    e.message?.contains("overloaded") == true) {

                    if (attempt < maxRetries - 1) {
                        val delayMs = (attempt + 1) * 4000L
                        Log.d("AIRepository", "⏳ Esperando ${delayMs}ms antes de reintentar...")
                        delay(delayMs)
                    }
                } else {
                    throw e
                }
            } catch (e: Exception) {
                lastException = e
                Log.e("AIRepository", "❌ Error inesperado: ${e.message}")
                throw e
            }
        }

        throw lastException ?: Exception("No se pudo generar recomendación después de $maxRetries intentos")
    }

    // ⭐ GENERAR HASH DEL CONTENIDO
    private fun generateContentHash(entries: List<String>): String {
        if (entries.isEmpty()) {
            return "empty"
        }

        // Normalizar agresivamente para evitar diferencias mínimas
        val normalized = entries
            .map { entry ->
                entry
                    .trim()  // Quitar espacios al inicio/fin
                    .replace(Regex("\\s+"), " ")  // Espacios múltiples → 1 espacio
                    .lowercase()  // Todo en minúsculas
            }
            .sorted()  // Ordenar alfabéticamente
            .joinToString("|")

        val hash = normalized.hashCode().toString()

        // ⭐ Log para debugging
        Log.d("AIRepository", "🔑 Hash generado:")
        Log.d("AIRepository", "   Entries: ${entries.size}")
        Log.d("AIRepository", "   Normalized: '$normalized'")
        Log.d("AIRepository", "   Hash: '$hash'")

        return hash
    }

    private suspend fun saveLastRecommendation(userId: String, recommendation: String) {
        try {
            val data = hashMapOf(
                "userId" to userId,
                "lastRecommendation" to recommendation,
                "timestamp" to Timestamp.now()
            )

            firestore.collection("users")
                .document(userId)
                .collection("aiData")
                .document("lastRecommendation")
                .set(data)
                .await()

            Log.d("AIRepository", "💾 Última recomendación guardada")
        } catch (e: Exception) {
            Log.e("AIRepository", "❌ Error guardando última recomendación: ${e.message}")
        }
    }

    private suspend fun getLastSavedRecommendation(userId: String): String? {
        return try {
            val doc = firestore.collection("users")
                .document(userId)
                .collection("aiData")
                .document("lastRecommendation")
                .get()
                .await()

            val recommendation = doc.getString("lastRecommendation")
            val timestamp = doc.getTimestamp("timestamp")

            if (recommendation != null && timestamp != null) {
                val daysSinceRecommendation = (System.currentTimeMillis() - timestamp.toDate().time) / (1000 * 60 * 60 * 24)

                if (daysSinceRecommendation <= 7) {
                    Log.d("AIRepository", "📜 Recomendación encontrada (${daysSinceRecommendation} días de antigüedad)")
                    recommendation
                } else {
                    Log.d("AIRepository", "⏰ Recomendación muy antigua (${daysSinceRecommendation} días)")
                    null
                }
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("AIRepository", "❌ Error obteniendo última recomendación: ${e.message}")
            null
        }
    }

    private suspend fun getAIConfig(): AIConfig {
        return try {
            if (cachedConfig != null) {
                return cachedConfig!!
            }

            val doc = firestore.collection("config")
                .document("ai")
                .get()
                .await()

            val config = doc.toObject(AIConfig::class.java) ?: AIConfig()
            cachedConfig = config
            config
        } catch (e: Exception) {
            Log.e("AIRepository", "Error obteniendo config: ${e.message}")
            AIConfig()
        }
    }

    private suspend fun saveRecommendationLog(
        userId: String,
        userInput: String,
        aiResponse: String,
        promptUsed: String
    ) {
        try {
            val userDoc = firestore.collection("users").document(userId).get().await()
            val userName = userDoc.getString("name") ?: "Usuario"

            val log = AIRecommendationLog(
                userId = userId,
                userName = userName,
                userInput = userInput,
                aiResponse = aiResponse,
                timestamp = Timestamp.now(),
                promptUsed = promptUsed
            )

            firestore.collection("aiLogs")
                .add(log)
                .await()

            Log.d("AIRepository", "✅ Log guardado correctamente")
        } catch (e: Exception) {
            Log.e("AIRepository", "Error guardando log: ${e.message}")
        }
    }

    fun invalidateCache() {
        Log.d("AIRepository", "🗑️ Limpiando caché")
        cachedRecommendation = null
        lastEntriesContent = ""
        lastUserId = ""
        cachedConfig = null
    }

    private suspend fun getTodayDiaryEntries(userId: String): List<String> {
        return try {
            val today = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "ES"))
                .format(Date())
                .replaceFirstChar { it.uppercase() }

            val snapshot = firestore.collection("diaryEntries")
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", today)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val moment = doc.getString("moment") ?: ""
                val description = doc.getString("description") ?: ""
                val sticker = doc.getString("sticker") ?: ""
                val rating = doc.getString("rating") ?: ""

                if (description.isNotBlank()) {
                    "$sticker $moment: $description (le pareció: $rating)"
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e("AIRepository", "Error obteniendo entradas: ${e.message}")
            emptyList()
        }
    }

    private fun createPrompt(diaryEntries: List<String>, systemPrompt: String): String {
        val entriesText = diaryEntries.joinToString("\n")

        return """
$systemPrompt

Un niño/adolescente registró lo siguiente que comió hoy:

$entriesText
        """.trimIndent()
    }

    companion object {
        @Volatile
        private var INSTANCE: AIRepository? = null

        fun getInstance(): AIRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AIRepository().also { INSTANCE = it }
            }
        }
    }

    private fun cleanBrokenEmojis(text: String): String {
        if (text.isEmpty()) return text

        // Verificar si el último carácter es un surrogate sin pareja
        val lastChar = text.last()

        return if (Character.isHighSurrogate(lastChar) || Character.isLowSurrogate(lastChar)) {
            // Si hay un surrogate sin pareja, remover el último carácter
            text.dropLast(1)
        } else {
            text
        }
    }
}