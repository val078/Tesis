package com.example.tesis.data.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tesis.data.model.AuthResult
import com.example.tesis.data.model.User
import com.example.tesis.data.repository.AuthRepository
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

class AuthViewModel : ViewModel() {
    private val authRepository = AuthRepository()
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance() // ⭐ Faltaba esta línea

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _authState = MutableStateFlow<AuthResult?>(null)
    val authState: StateFlow<AuthResult?> = _authState

    val currentUser = authRepository.getAuthState()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // ⭐ NUEVO: Verificar modo mantenimiento
    suspend fun checkMaintenanceMode(): Boolean {
        return try {
            val doc = firestore.collection("config")
                .document("app")
                .get()
                .await()

            val maintenanceMode = doc.getBoolean("maintenanceMode") ?: false
            Log.d("AuthViewModel", "🔧 Modo mantenimiento: $maintenanceMode")
            maintenanceMode
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error verificando mantenimiento: ${e.message}")
            false
        }
    }

    // ⭐ NUEVO: Verificar si permite registros
    suspend fun checkRegistrationsAllowed(): Boolean {
        return try {
            val doc = firestore.collection("config")
                .document("app")
                .get()
                .await()

            val allowed = doc.getBoolean("allowNewRegistrations") ?: true
            Log.d("AuthViewModel", "📝 Registros permitidos: $allowed")
            allowed
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error verificando registros: ${e.message}")
            true
        }
    }

    // ⭐ NUEVO: Obtener mensaje de mantenimiento
    suspend fun getMaintenanceMessage(): String {
        return try {
            val doc = firestore.collection("config")
                .document("app")
                .get()
                .await()

            doc.getString("maintenanceMessage")
                ?: "Estamos mejorando la app. Vuelve pronto 🚀"
        } catch (e: Exception) {
            "Estamos mejorando la app. Vuelve pronto 🚀"
        }
    }

    fun logout() {
        viewModelScope.launch {
            Log.d("AuthViewModel", "🚪 Ejecutando logout...")
            authRepository.logout()
            _authState.value = null
        }
    }

    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            _isLoading.value = true
            Log.d("AuthViewModel", "📧 Iniciando login con: $email")

            val result = authRepository.loginUser(email, password)
            _authState.value = result

            if (!result.success) {
                Log.d("AuthViewModel", "❌ Login fallido: ${result.message}")
            } else {
                Log.d("AuthViewModel", "✅ Login exitoso")
            }

            _isLoading.value = false
        }
    }

    fun registerUser(
        name: String,
        email: String,
        password: String,
        parentEmail: String?,
        birthDate: Date?
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _authState.value = authRepository.registerUser(name, email, password, parentEmail, birthDate)
            _isLoading.value = false
        }
    }

    // ⭐ NUEVO: Recargar usuario actual
    fun reloadCurrentUser() {
        viewModelScope.launch {
            try {
                val userId = auth.currentUser?.uid ?: return@launch

                Log.d("AuthViewModel", "🔄 Recargando usuario...")

                val doc = firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()

                val user = doc.toObject(User::class.java)

                // ⚠️ PROBLEMA: currentUser es un Flow del AuthRepository
                // No podemos modificarlo directamente aquí
                // La solución está abajo ↓

                Log.d("AuthViewModel", "✅ Usuario recargado: ${user?.name}, avatar: ${user?.avatar}")
            } catch (e: Exception) {
                Log.e("AuthViewModel", "❌ Error recargando usuario", e)
            }
        }
    }

    suspend fun sendPasswordResetEmail(email: String) {
        try {
            auth.sendPasswordResetEmail(email).await()
        } catch (e: Exception) {
            throw Exception("No se pudo enviar el correo de recuperación. Verifica que el correo esté registrado.")
        }
    }

    fun changePasswordWithReauth(
        currentPassword: String,
        newPassword: String,
        callback: (Boolean, String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val user = auth.currentUser
                if (user == null || user.email == null) {
                    callback(false, "Usuario no autenticado")
                    return@launch
                }

                val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)

                user.reauthenticate(credential).await()
                Log.d("AuthViewModel", "Reautenticación exitosa")

                // Paso 2: Cambiar la contraseña
                user.updatePassword(newPassword).await()
                Log.d("AuthViewModel", "Contraseña actualizada correctamente")

                callback(true, null)

            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error al cambiar contraseña", e)

                val errorMessage = when (e) {
                    is com.google.firebase.auth.FirebaseAuthInvalidCredentialsException ->
                        "La contraseña actual es incorrecta"
                    is com.google.firebase.auth.FirebaseAuthWeakPasswordException ->
                        "La nueva contraseña es demasiado débil"
                    else -> "Error: ${e.localizedMessage}"
                }

                callback(false, errorMessage)
            }
        }
    }
}