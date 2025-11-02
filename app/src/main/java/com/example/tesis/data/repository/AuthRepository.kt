package com.example.tesis.data.repository

import android.util.Log
import com.example.tesis.data.model.AuthResult
import com.example.tesis.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

class AuthRepository {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "AuthRepository"
    }

    // ✅ Flow en tiempo real de autenticación CON SnapshotListener
    fun getAuthState(): Flow<User?> = callbackFlow {
        Log.d(TAG, "🔄 Iniciando Flow de autenticación")

        var firestoreListener: ListenerRegistration? = null

        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val firebaseUser = firebaseAuth.currentUser
            Log.d(TAG, "🎯 AuthStateListener - Usuario: ${firebaseUser?.email ?: "NULL"}")

            // ⭐ Remover listener anterior si existe
            firestoreListener?.remove()

            if (firebaseUser != null) {
                // ⭐ CAMBIO CRÍTICO: Usar addSnapshotListener en lugar de get()
                firestoreListener = firestore.collection("users")
                    .document(firebaseUser.uid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            Log.e(TAG, "❌ Error en SnapshotListener: ${error.message}")
                            trySend(null)
                            return@addSnapshotListener
                        }

                        if (snapshot != null && snapshot.exists()) {
                            val user = snapshot.toObject(User::class.java)
                            Log.d(TAG, "📡 Datos actualizados en tiempo real: ${user?.name}, avatar: ${user?.avatar}")
                            trySend(user)
                        } else {
                            Log.w(TAG, "⚠️ Usuario sin datos en Firestore")
                            trySend(null)
                        }
                    }
            } else {
                // No hay usuario autenticado
                Log.d(TAG, "🔴 No hay usuario - enviando NULL")
                trySend(null)
            }
        }

        auth.addAuthStateListener(authStateListener)
        Log.d(TAG, "📝 AuthStateListener registrado")

        awaitClose {
            Log.d(TAG, "🧹 Limpiando listeners")
            auth.removeAuthStateListener(authStateListener)
            firestoreListener?.remove()
        }
    }

    suspend fun getCurrentUser(): User? {
        return try {
            Log.d(TAG, "=== getCurrentUser INICIADO ===")

            val firebaseUser = auth.currentUser
            if (firebaseUser == null) {
                Log.w(TAG, "⚠️ No hay usuario autenticado")
                return null
            }

            val document = firestore.collection("users")
                .document(firebaseUser.uid)
                .get()
                .await()

            if (!document.exists()) {
                Log.e(TAG, "❌ Documento no existe en Firestore")
                return null
            }

            val user = document.toObject(User::class.java)
            Log.d(TAG, "✅ Usuario cargado: ${user?.email}")
            user

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al obtener usuario", e)
            null
        } finally {
            Log.d(TAG, "=== getCurrentUser FINALIZADO ===")
        }
    }

    suspend fun loginUser(emailOrUsername: String, password: String): AuthResult {
        return try {
            Log.d(TAG, "=== loginUser INICIADO ===")

            val firestore = FirebaseFirestore.getInstance()
            val configDoc = firestore.collection("config").document("app").get().await()
            val config = configDoc.data ?: emptyMap()
            val maintenanceMode = config["maintenanceMode"] as? Boolean ?: false
            val maintenanceMessage = config["maintenanceMessage"] as? String
                ?: "Estamos mejorando la app. Vuelve pronto 🚧"

            // Verificar mantenimiento antes de login
            if (maintenanceMode) {
                val adminEmail = "admin@gmail.com"
                val isEmail = emailOrUsername.contains("@")
                val emailToUse = if (isEmail) emailOrUsername else {
                    val querySnapshot = firestore.collection("users")
                        .whereEqualTo("name", emailOrUsername)
                        .limit(1)
                        .get()
                        .await()

                    if (querySnapshot.isEmpty) {
                        return AuthResult(
                            success = false,
                            message = "Usuario '$emailOrUsername' no encontrado"
                        )
                    }

                    querySnapshot.documents.first().getString("email") ?: ""
                }

                if (emailToUse.lowercase() != adminEmail) {
                    Log.w(TAG, "🚫 Bloqueado por mantenimiento: $emailToUse")
                    return AuthResult(
                        success = false,
                        message = maintenanceMessage
                    )
                }
            }

            // 🔹 Resto del login normal
            val isEmail = emailOrUsername.contains("@")
            val emailToUse: String

            if (isEmail) {
                emailToUse = emailOrUsername
            } else {
                val querySnapshot = firestore.collection("users")
                    .whereEqualTo("name", emailOrUsername)
                    .limit(1)
                    .get()
                    .await()

                if (querySnapshot.isEmpty) {
                    return AuthResult(
                        success = false,
                        message = "Usuario '$emailOrUsername' no encontrado"
                    )
                }

                emailToUse = querySnapshot.documents.first().getString("email") ?: ""
            }

            val authResult = auth.signInWithEmailAndPassword(emailToUse, password).await()
            if (authResult.user == null) {
                return AuthResult(success = false, message = "Error al iniciar sesión")
            }

            val user = getCurrentUser()
            if (user == null) {
                return AuthResult(success = true, message = "Login exitoso sin datos de perfil")
            }

            if (!user.active) {
                auth.signOut()
                return AuthResult(success = false, message = "Cuenta desactivada por el administrador")
            }

            AuthResult(success = true, message = "¡Bienvenido ${user.name}!", user = user)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error en login", e)
            val errorMessage = when {
                e.message?.contains("password") == true -> "Contraseña incorrecta"
                e.message?.contains("network") == true -> "Error de conexión"
                else -> "Error al iniciar sesión: ${e.message}"
            }
            AuthResult(success = false, message = errorMessage)
        }
    }

    suspend fun registerUser(
        name: String,
        email: String,
        password: String,
        parentEmail: String?,
        birthDate: Date?
    ): AuthResult {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: return AuthResult(
                success = false,
                message = "Error al crear usuario"
            )

            val user = User(
                userId = firebaseUser.uid,
                name = name,
                email = email,
                parentEmail = parentEmail,
                birthDate = birthDate,
                createdAt = Date()
            )

            firestore.collection("users")
                .document(firebaseUser.uid)
                .set(user)
                .await()

            AuthResult(success = true, message = "Registro exitoso", user = user)

        } catch (e: Exception) {
            AuthResult(success = false, message = e.message ?: "Error al registrar")
        }
    }

    fun logout() {
        Log.d(TAG, "🚪 Cerrando sesión")
        auth.signOut()
        Log.d(TAG, "✅ Sesión cerrada")
    }

    suspend fun changePassword(newPassword: String): Boolean {
        return try {
            val user = auth.currentUser
            user?.updatePassword(newPassword)?.await()
            Log.d(TAG, "✅ Contraseña actualizada correctamente")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error al cambiar contraseña", e)
            false
        }
    }
}