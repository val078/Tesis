// utils/FirebaseErrorParser.kt
package com.example.tesis.utils

object FirebaseErrorParser {
    fun parseFirebaseError(errorMessage: String): String {
        return when {
            // Errores de correo
            errorMessage.contains("The email address is badly formatted") ->
                "ERROR_INVALID_EMAIL"
            errorMessage.contains("The email address is already in use") ->
                "ERROR_EMAIL_ALREADY_IN_USE"

            // Errores de contraseña
            errorMessage.contains("The password is invalid") ->
                "ERROR_WRONG_PASSWORD"
            errorMessage.contains("Password should be at least 6 characters") ->
                "ERROR_WEAK_PASSWORD"

            // Errores de usuario
            errorMessage.contains("There is no user record") ->
                "ERROR_USER_NOT_FOUND"
            errorMessage.contains("The user account has been disabled") ->
                "ERROR_USER_DISABLED"

            // Errores de credenciales
            errorMessage.contains("invalid credential") ->
                "ERROR_INVALID_CREDENTIAL"
            errorMessage.contains("Operation not allowed") ->
                "ERROR_OPERATION_NOT_ALLOWED"

            // Errores de token
            errorMessage.contains("token expired") ->
                "ERROR_USER_TOKEN_EXPIRED"
            errorMessage.contains("requires recent login") ->
                "ERROR_REQUIRES_RECENT_LOGIN"

            // Errores de red
            errorMessage.contains("A network error") ->
                "ERROR_NETWORK_REQUEST_FAILED"
            errorMessage.contains("Too many requests") ->
                "ERROR_TOO_MANY_REQUESTS"

            // Errores personalizados de nuestra app
            errorMessage.contains("nombre de usuario ya existe") ->
                "USERNAME_EXISTS"
            errorMessage.contains("No encontramos ese nombre de usuario") ->
                "USERNAME_NOT_FOUND"
            errorMessage.contains("correo parental ya está en uso") ->
                "PARENT_EMAIL_IN_USE"

            else -> {
                // ✅ Manejo de errores genéricos
                when {
                    errorMessage.contains("already exists") -> "ERROR_EMAIL_ALREADY_IN_USE"
                    errorMessage.contains("not found") -> "ERROR_USER_NOT_FOUND"
                    errorMessage.contains("invalid") -> "ERROR_INVALID_CREDENTIAL"
                    errorMessage.contains("network") -> "ERROR_NETWORK_REQUEST_FAILED"
                    errorMessage.contains("disabled") -> "ERROR_USER_DISABLED"
                    else -> "ERROR_UNKNOWN" // ✅ Error desconocido
                }
            }
        }
    }

    // ✅ Nueva función para verificar si es un error de nuestra app
    fun isCustomAppError(errorCode: String): Boolean {
        return when (errorCode) {
            "USERNAME_EXISTS",
            "USERNAME_NOT_FOUND",
            "PARENT_EMAIL_IN_USE" -> true
            else -> false
        }
    }

    // ✅ Nueva función para obtener mensajes personalizados
    fun getCustomErrorMessage(errorCode: String): String {
        return when (errorCode) {
            "USERNAME_EXISTS" -> "¡Este nombre de usuario ya existe! Elige otro diferente"
            "USERNAME_NOT_FOUND" -> "No encontramos ese nombre de usuario. ¿Seguro que está bien escrito?"
            "PARENT_EMAIL_IN_USE" -> "¡Este correo parental ya está en uso por otra cuenta!"
            else -> "¡Ups! Algo no funcionó bien. ¡Vamos a intentarlo otra vez!"
        }
    }
}