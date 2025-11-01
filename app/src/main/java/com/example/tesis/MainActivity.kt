package com.example.tesis

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.tesis.data.model.NotificationSettings
import com.example.tesis.notifications.NotificationHelper
import com.example.tesis.notifications.NotificationScheduler
import com.example.tesis.ui.navigation.AppNavigation
import com.example.tesis.ui.theme.TesisTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// 👇 Importa esto (accompanist)
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

class MainActivity : ComponentActivity() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // ⭐ Launcher para solicitar permisos
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("MainActivity", "✅ Permiso de notificaciones concedido")
            setupNotifications()
        } else {
            Log.d("MainActivity", "❌ Permiso de notificaciones denegado")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ⭐ PASO 1: Crear canal de notificaciones
        NotificationHelper.createNotificationChannel(this)
        Log.d("MainActivity", "📱 Canal de notificaciones creado")

        // ⭐ PASO 2: Solicitar permisos de notificación (Android 13+)
        checkAndRequestNotificationPermission()

        setContent {
            TesisTheme {
                // 👇 PASO 1: Control del color de las barras del sistema
                val systemUiController = rememberSystemUiController()
                val useDarkIcons = true // true si tu fondo es claro

                SideEffect {
                    // Barra superior (status bar)
                    systemUiController.setStatusBarColor(
                        color = Color(0xFFF1A7A7), // tu color personalizado
                        darkIcons = useDarkIcons
                    )
                }

                // 👇 Tu UI principal
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    // ⭐ PASO 2: Verificar y solicitar permisos
    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    Log.d("MainActivity", "✅ Ya tiene permiso de notificaciones")
                    setupNotifications()
                }
                else -> {
                    Log.d("MainActivity", "❓ Solicitando permiso de notificaciones")
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            // Android 12 o inferior no necesita solicitar permiso
            setupNotifications()
        }
    }

    // ⭐ PASO 3: Programar notificaciones
    private fun setupNotifications() {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Log.d("MainActivity", "⚠️ Usuario no autenticado, no se programan notificaciones")
            return
        }

        // Cargar configuración y programar notificaciones en background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("MainActivity", "🔄 Cargando configuración de notificaciones...")

                val doc = firestore.collection("users")
                    .document(userId)
                    .collection("settings")
                    .document("notifications")
                    .get()
                    .await()

                val settings = if (doc.exists()) {
                    doc.toObject(NotificationSettings::class.java) ?: NotificationSettings()
                } else {
                    NotificationSettings()
                }

                // ⭐ Programar notificaciones
                NotificationScheduler.scheduleAllNotifications(this@MainActivity, settings)
                Log.d("MainActivity", "✅ Notificaciones programadas correctamente")

            } catch (e: Exception) {
                Log.e("MainActivity", "❌ Error configurando notificaciones", e)
            }
        }
    }
}
