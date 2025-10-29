// Crea: notifications/PlayReminderWorker.kt
package com.example.tesis.notifications

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*

class PlayReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override suspend fun doWork(): Result {
        return try {
            val userId = auth.currentUser?.uid ?: return Result.failure()

            Log.d("PlayReminderWorker", "🎮 Verificando si jugó hoy")

            // Verificar si jugó hoy
            val hasPlayedToday = checkTodayPlay(userId)

            if (!hasPlayedToday) {
                Log.d("PlayReminderWorker", "⚠️ No ha jugado hoy, enviando notificación")
                sendPlayReminder()
            } else {
                Log.d("PlayReminderWorker", "✅ Ya jugó hoy")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("PlayReminderWorker", "❌ Error: ${e.message}", e)
            Result.failure()
        }
    }

    private suspend fun checkTodayPlay(userId: String): Boolean {
        return try {
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.time

            val snapshot = firestore.collection("gameProgress")
                .whereEqualTo("userId", userId)
                .whereGreaterThan("timestamp", todayStart)
                .limit(1)
                .get()
                .await()

            !snapshot.isEmpty
        } catch (e: Exception) {
            Log.e("PlayReminderWorker", "Error verificando juegos: ${e.message}")
            false
        }
    }

    private fun sendPlayReminder() {
        NotificationHelper.showNotification(
            applicationContext,
            NotificationHelper.PLAY_NOTIFICATION_ID,
            "¡Hora de jugar!",
            "¿Qué tal un juego educativo? ¡Mantén a Sr Pollo feliz! 🎮",
            "🎮"
        )
    }
}