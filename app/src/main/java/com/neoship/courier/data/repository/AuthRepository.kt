package com.neoship.courier.data.repository

import com.neoship.courier.data.api.RetrofitClient
import com.neoship.courier.data.api.models.LoginRequest
import com.neoship.courier.data.local.TokenManager

/**
 * Repository d'authentification.
 * Conforme au contrat API V1.0 :
 *   POST /api/auth/login → { user: { id, email, name, role, phone }, token }
 */
class AuthRepository(
    private val tokenManager: TokenManager
) {
    private val api = RetrofitClient.apiService

    suspend fun login(email: String, password: String): Result<String> {
        return try {
            val response = api.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                val body = response.body()
                    ?: return Result.failure(Exception("Réponse vide du serveur"))

                val token = body.token
                val userId = body.user.id

                tokenManager.saveToken(token)
                tokenManager.saveUserId(userId)

                Result.success(token)
            } else {
                val message = when (response.code()) {
                    400 -> "Email ou mot de passe invalide"
                    401 -> "Email ou mot de passe incorrect"
                    429 -> "Trop de tentatives. Réessayez dans une minute."
                    else -> "Erreur serveur (${response.code()})"
                }
                Result.failure(Exception(message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Impossible de contacter le serveur : ${e.localizedMessage}"))
        }
    }

    fun isLoggedIn(): Boolean = tokenManager.hasToken()

    fun logout() {
        tokenManager.clearToken()
    }

    fun getToken(): String? = tokenManager.getToken()

    fun getUserId(): String? = tokenManager.getUserId()

    fun getCourierName(): String? = null  // À récupérer plus tard si besoin
}