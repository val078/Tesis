// Crea: notifications/DiaryReminderWorker.kt
package com.example.tesis.notifications

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

class DiaryReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override suspend fun doWork(): Result {
        return try {
            val mealType = inputData.getString("meal_type") ?: return Result.failure()
            val userId = auth.currentUser?.uid ?: return Result.failure()

            Log.d("DiaryReminderWorker", "🔔 Verificando entrada de $mealType")

            // Verificar si ya escribió la entrada de hoy
            val hasEntry = checkTodayEntry(userId, mealType)

            if (!hasEntry) {
                Log.d("DiaryReminderWorker", "⚠️ No hay entrada de $mealType, enviando notificación")
                sendReminder(mealType)
            } else {
                Log.d("DiaryReminderWorker", "✅ Ya tiene entrada de $mealType")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e("DiaryReminderWorker", "❌ Error: ${e.message}", e)
            Result.failure()
        }
    }

    private suspend fun checkTodayEntry(userId: String, mealType: String): Boolean {
        return try {
            val today = SimpleDateFormat("EEEE d 'de' MMMM", Locale("es", "ES"))
                .format(Date())
                .replaceFirstChar { it.uppercase() }

            val snapshot = firestore.collection("diaryEntries")
                .whereEqualTo("userId", userId)
                .whereEqualTo("date", today)
                .whereEqualTo("moment", mealType)
                .get()
                .await()

            !snapshot.isEmpty
        } catch (e: Exception) {
            Log.e("DiaryReminderWorker", "Error verificando entrada: ${e.message}")
            false
        }
    }

    private fun sendReminder(mealType: String) {
        val (title, message, emoji, notificationId) = when (mealType) {
            "Desayuno" -> Tuple4(
                "¡Hora del desayuno!",
                "¿Ya desayunaste? Cuéntame qué comiste hoy",
                "☀️",
                NotificationHelper.BREAKFAST_NOTIFICATION_ID
            )
            "Almuerzo" -> Tuple4(
                "¡Hora del almuerzo!",
                "¿Qué delicioso almorzaste? Escríbelo en tu diario",
                "🌞",
                NotificationHelper.LUNCH_NOTIFICATION_ID
            )
            "Merienda" -> Tuple4(
                "¡Hora de la merienda!",
                "¿Ya cenaste? Cuéntame qué comiste en la noche",
                "🌙",
                NotificationHelper.SNACK_NOTIFICATION_ID
            )
            else -> return
        }

        NotificationHelper.showNotification(
            applicationContext,
            notificationId,
            title,
            message,
            emoji
        )
    }

    // Helper class para retornar múltiples valores
    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}