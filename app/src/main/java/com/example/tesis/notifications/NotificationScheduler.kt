// Crea: notifications/NotificationScheduler.kt
package com.example.tesis.notifications

import android.content.Context
import android.util.Log
import androidx.work.*
import java.util.Calendar
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    private const val BREAKFAST_WORK_TAG = "breakfast_reminder"
    private const val LUNCH_WORK_TAG = "lunch_reminder"
    private const val SNACK_WORK_TAG = "snack_reminder"
    private const val PLAY_WORK_TAG = "play_reminder"

    fun scheduleAllNotifications(context: Context, settings: NotificationSettings) {
        Log.d("NotificationScheduler", "📅 Programando notificaciones...")

        // Cancelar todas las notificaciones existentes
        WorkManager.getInstance(context).cancelAllWorkByTag(BREAKFAST_WORK_TAG)
        WorkManager.getInstance(context).cancelAllWorkByTag(LUNCH_WORK_TAG)
        WorkManager.getInstance(context).cancelAllWorkByTag(SNACK_WORK_TAG)
        WorkManager.getInstance(context).cancelAllWorkByTag(PLAY_WORK_TAG)

        if (!settings.enabled) {
            Log.d("NotificationScheduler", "⚠️ Notificaciones deshabilitadas")
            return
        }

        // Programar cada notificación
        if (settings.breakfastEnabled) {
            scheduleMealReminder(context, "Desayuno", settings.breakfastTime, BREAKFAST_WORK_TAG)
        }

        if (settings.lunchEnabled) {
            scheduleMealReminder(context, "Almuerzo", settings.lunchTime, LUNCH_WORK_TAG)
        }

        if (settings.snackEnabled) {
            scheduleMealReminder(context, "Merienda", settings.snackTime, SNACK_WORK_TAG)
        }

        if (settings.playEnabled) {
            schedulePlayReminder(context, settings.playTime)
        }

        Log.d("NotificationScheduler", "✅ Notificaciones programadas")
    }

    private fun scheduleMealReminder(
        context: Context,
        mealType: String,
        time: String, // "HH:mm"
        tag: String
    ) {
        val (hour, minute) = time.split(":").map { it.toInt() }
        val delay = calculateDelay(hour, minute)

        Log.d("NotificationScheduler", "⏰ $mealType a las $time (delay: ${delay}ms)")

        val data = workDataOf("meal_type" to mealType)

        val workRequest = PeriodicWorkRequestBuilder<DiaryReminderWorker>(
            24, TimeUnit.HOURS // Repetir cada 24 horas
        )
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag(tag)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            tag,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun schedulePlayReminder(context: Context, time: String) {
        val (hour, minute) = time.split(":").map { it.toInt() }
        val delay = calculateDelay(hour, minute)

        Log.d("NotificationScheduler", "⏰ Jugar a las $time (delay: ${delay}ms)")

        val workRequest = PeriodicWorkRequestBuilder<PlayReminderWorker>(
            24, TimeUnit.HOURS
        )
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .addTag(PLAY_WORK_TAG)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PLAY_WORK_TAG,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun calculateDelay(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Si la hora ya pasó hoy, programar para mañana
        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        return target.timeInMillis - now.timeInMillis
    }

    fun cancelAllNotifications(context: Context) {
        Log.d("NotificationScheduler", "🗑️ Cancelando todas las notificaciones")
        WorkManager.getInstance(context).cancelAllWork()
    }
}