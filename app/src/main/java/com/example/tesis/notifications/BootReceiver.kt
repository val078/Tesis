// Crea: notifications/BootReceiver.kt
package com.example.tesis.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.tesis.data.model.NotificationSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootReceiver", "📱 Dispositivo reiniciado, reprogramando notificaciones")

            // Cargar configuración y reprogramar
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // TODO: Cargar settings desde SharedPreferences o Firestore
                    val settings = NotificationSettings() // Default
                    NotificationScheduler.scheduleAllNotifications(context, settings)
                } catch (e: Exception) {
                    Log.e("BootReceiver", "Error reprogramando notificaciones", e)
                }
            }
        }
    }
}