package com.example.tesis.utils

import com.example.tesis.utils.FirebaseErrorParser

object KidFriendlyMessages {
    fun getKidFriendlyErrorMessage(errorCode: String): String {

        if (FirebaseErrorParser.isCustomAppError(errorCode)) {
            return FirebaseErrorParser.getCustomErrorMessage(errorCode)
        }
        return when (errorCode) {
            // Errores de correo y contraseña
            "ERROR_INVALID_EMAIL" -> "¡Ups! Ese correo no parece correcto. Revisa que tenga @ y .com"
            "ERROR_WRONG_PASSWORD" -> "¡Contraseña incorrecta! Inténtalo de nuevo, tú puedes"
            "ERROR_USER_NOT_FOUND" -> "No encontramos esa cuenta. ¿Seguro que ya te registraste?"
            "ERROR_EMAIL_ALREADY_IN_USE" -> "¡Ese correo ya tiene una cuenta! Prueba con otro o inicia sesión"
            "ERROR_WEAK_PASSWORD" -> "La contraseña necesita ser más fuerte. Usa al menos 6 letras o números"
            "ERROR_USER_DISABLED" -> "Esta cuenta ha sido desactivada. Contacta al administrador"

            // Errores de credenciales
            "ERROR_INVALID_CREDENTIAL" -> "Las credenciales no son válidas. Revisa tu correo y contraseña"
            "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" -> "Ya existe una cuenta con ese correo. ¡Inicia sesión!"

            // Errores de operación
            "ERROR_OPERATION_NOT_ALLOWED" -> "Esta acción no está permitida ahora. ¡Lo sentimos!"

            // Errores de red
            "ERROR_NETWORK_REQUEST_FAILED" -> "No hay internet. Conéctate a WiFi y prueba de nuevo"
            "ERROR_TOO_MANY_REQUESTS" -> "¡Demasiados intentos! Descansa un poquito y prueba más tarde"

            // Errores de sesión
            "ERROR_USER_TOKEN_EXPIRED" -> "Tu sesión terminó. ¡Vuelve a iniciar sesión para seguir jugando!"
            "ERROR_INVALID_USER_TOKEN" -> "Tu token es inválido. ¡Inicia sesión de nuevo!"
            "ERROR_REQUIRES_RECENT_LOGIN" -> "Necesitas iniciar sesión nuevamente para esta acción"

            else -> "¡Ups! Algo no funcionó bien. ¡Vamos a intentarlo otra vez!"
        }
    }

    fun getValidationErrors(field: String): String {
        return when (field) {
            "passwords_dont_match" -> "Las contraseñas no son iguales. ¡Revísalas!"
            "parents_used" -> "¡Este correo parental ya está en uso por otra cuenta!"
            "username_not_found" -> "No encontramos ese nombre de usuario. ¿Seguro que está registrado?"
            "username_exists" -> "¡Este nombre de usuario ya existe! Elige otro diferente"
            else -> "¡Ups! Algo no está bien en un campo"
        }
    }

    fun isValidEmail(email: String): Boolean {
        return email.contains("@") && email.contains(".") && email.length >= 5
    }

    fun doPasswordsMatch(password: String, confirmPassword: String): Boolean {
        return password == confirmPassword
    }
}