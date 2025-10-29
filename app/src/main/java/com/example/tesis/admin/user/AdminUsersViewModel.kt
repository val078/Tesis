package com.example.tesis.admin.user

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tesis.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AdminUsersViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private val _users = MutableStateFlow<List<User>>(emptyList())
    val users: StateFlow<List<User>> = _users

    fun loadUsers() {
        viewModelScope.launch {
            try {
                firestore.collection("users")
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            Log.e("AdminUsersViewModel", "❌ Error escuchando usuarios", e)
                            return@addSnapshotListener
                        }
                        if (snapshot != null) {
                            _users.value = snapshot.documents.mapNotNull { it.toObject(User::class.java) }
                            Log.d("AdminUsersViewModel", "✅ ${_users.value.size} usuarios cargados (realtime)")
                        }
                    }
            } catch (e: Exception) {
                Log.e("AdminUsersViewModel", "❌ Error cargando usuarios", e)
            }
        }
    }

    fun toggleUserStatus(userId: String, newStatus: Boolean) {
        viewModelScope.launch {
            try {
                Log.d("AdminUsersViewModel", "🔄 Cambiando usuario $userId a active=$newStatus")

                // 🔹 CAMBIO AQUÍ: antes decía "isActive"
                firestore.collection("users")
                    .document(userId)
                    .update("active", newStatus)
                    .await()

                Log.d("AdminUsersViewModel", "✅ Firestore actualizado")

                _users.value = _users.value.map { user ->
                    if (user.userId == userId) {
                        user.copy(active = newStatus)
                    } else {
                        user
                    }
                }

                Log.d("AdminUsersViewModel", "✅ Estado local actualizado")
            } catch (e: Exception) {
                Log.e("AdminUsersViewModel", "❌ Error: ${e.message}", e)
                loadUsers()
            }
        }
    }

    // ✅ NUEVA: Función para actualizar usuario
    fun updateUser(user: User) {
        viewModelScope.launch {
            try {
                Log.d("AdminUsersViewModel", "📝 Actualizando usuario ${user.userId}")

                // Actualizar en Firestore
                firestore.collection("users")
                    .document(user.userId)
                    .set(user)
                    .await()

                Log.d("AdminUsersViewModel", "✅ Usuario actualizado en Firestore")

                // Actualizar localmente
                _users.value = _users.value.map {
                    if (it.userId == user.userId) user else it
                }

                Log.d("AdminUsersViewModel", "✅ Lista local actualizada")
            } catch (e: Exception) {
                Log.e("AdminUsersViewModel", "❌ Error actualizando usuario", e)
            }
        }
    }

    // ⚠️ EJECUTAR UNA SOLA VEZ para activar todos los usuarios
    fun activateAllUsers() {
        viewModelScope.launch {
            try {
                Log.d("AdminUsersViewModel", "🔄 Activando todos los usuarios...")

                val snapshot = firestore.collection("users").get().await()

                snapshot.documents.forEach { doc ->
                    doc.reference.update("isActive", true).await()
                    Log.d("AdminUsersViewModel", "✅ Usuario ${doc.id} activado")
                }

                Log.d("AdminUsersViewModel", "✅ Todos los usuarios activados")
                loadUsers()

            } catch (e: Exception) {
                Log.e("AdminUsersViewModel", "❌ Error activando usuarios", e)
            }
        }
    }

    // ⚠️ EJECUTAR UNA SOLA VEZ
    fun cleanAndStandardizeUsers() {
        viewModelScope.launch {
            try {
                Log.d("AdminUsersViewModel", "🧹 INICIANDO LIMPIEZA COMPLETA")

                val snapshot = firestore.collection("users").get().await()

                snapshot.documents.forEach { doc ->
                    Log.d("AdminUsersViewModel", "🔧 Limpiando usuario: ${doc.id}")

                    // Obtener datos actuales
                    val currentData = doc.data ?: return@forEach

                    // Crear mapa limpio con SOLO los campos necesarios
                    val cleanData = hashMapOf<String, Any?>(
                        "userId" to (currentData["userId"] ?: doc.id),
                        "name" to (currentData["name"] ?: ""),
                        "email" to (currentData["email"] ?: ""),
                        "parentEmail" to currentData["parentEmail"],
                        "birthDate" to currentData["birthDate"],
                        "createdAt" to (currentData["createdAt"] ?: com.google.firebase.Timestamp.now()),
                        "role" to (currentData["role"] ?: "user"),
                        "active" to (currentData["active"] ?: currentData["isActive"] ?: true) // ⭐ Unificar
                    )

                    // Reemplazar documento completo con datos limpios
                    doc.reference.set(cleanData).await()

                    Log.d("AdminUsersViewModel", "   ✅ Usuario limpio: ${cleanData["email"]}")
                }

                Log.d("AdminUsersViewModel", "✅ LIMPIEZA COMPLETADA")

                // Recargar usuarios
                loadUsers()

            } catch (e: Exception) {
                Log.e("AdminUsersViewModel", "❌ Error en limpieza: ${e.message}", e)
            }
        }
    }
}

