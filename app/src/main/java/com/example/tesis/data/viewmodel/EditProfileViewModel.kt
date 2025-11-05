// En EditProfileViewModel.kt
package com.example.tesis.data.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tesis.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EditProfileViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _saveSuccess = MutableStateFlow(false)
    val saveSuccess: StateFlow<Boolean> = _saveSuccess

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        loadUserData()
    }

    fun loadUserData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val userId = auth.currentUser?.uid ?: return@launch

                Log.d("EditProfileVM", "🔄 Cargando datos del usuario...")

                val doc = firestore.collection("users")
                    .document(userId)
                    .get()
                    .await()

                val user = doc.toObject(User::class.java)
                _currentUser.value = user

                Log.d("EditProfileVM", "✅ Datos cargados: ${user?.name}")

            } catch (e: Exception) {
                Log.e("EditProfileVM", "❌ Error cargando datos", e)
                _errorMessage.value = "Error al cargar datos: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateAvatar(newAvatar: String) {
        _currentUser.value = _currentUser.value?.copy(avatar = newAvatar)
    }

    fun updateName(newName: String) {
        _currentUser.value = _currentUser.value?.copy(name = newName)
    }

    fun saveProfile(onSuccess: () -> Unit = {}) { // ⭐ Agregar callback
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val userId = auth.currentUser?.uid ?: return@launch
                val user = _currentUser.value ?: return@launch

                Log.d("EditProfileVM", "💾 Guardando perfil...")

                firestore.collection("users")
                    .document(userId)
                    .update(
                        mapOf(
                            "name" to user.name,
                            "avatar" to user.avatar
                        )
                    )
                    .await()

                _saveSuccess.value = true
                Log.d("EditProfileVM", "✅ Perfil guardado: ${user.avatar}")

                onSuccess() // ⭐ Llamar callback

            } catch (e: Exception) {
                Log.e("EditProfileVM", "❌ Error guardando perfil", e)
                _errorMessage.value = "Error al guardar: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resetSaveSuccess() {
        _saveSuccess.value = false
    }

    fun clearError() {
        _errorMessage.value = null
    }
}