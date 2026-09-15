package com.neoship.courier.data.repository

import com.neoship.courier.data.api.RetrofitClient
import com.neoship.courier.data.api.models.LoginRequest
import com.neoship.courier.data.local.JwtDecoder
import com.neoship.courier.data.local.TokenManager

/**
 * Repository d'authentification.
 * Interface unique pour le login — les ViewModels ne touchent jamais Retrofit directement.
 */
class AuthRepository(
    private val tokenManager: TokenManager
) {
    private val api = RetrofitClient.apiService

    /**
     * Tente la connexion auprès du backend.
     * @return Result contenant le token si succès, l'erreur sinon.
     */
    suspend fun login(email: String, password: String): Result<String> {
        return try {
            val response = api.login(LoginRequest(email, password))
            if (response.isSuccessful) {
                val token = response.body()?.token
                    ?: return Result.failure(Exception("Réponse API invalide : token manquant"))
                tokenManager.saveToken(token)
                Result.success(token)
            } else {
                val message = when (response.code()) {
                    401 -> "Email ou mot de passe incorrect"
                    403 -> "Compte désactivé"
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

    /**
     * Décode le token JWT pour extraire l'identifiant du coursier.
     * Utilise l'ordre de champs : sub, courierId, courier_id, id, userId, user_id.
     * @return l'ID du coursier, ou une valeur par défaut "unknown" si non disponible
     */
    fun getCourierId(): String {
        val token = tokenManager.getToken() ?: return "unknown"
        return JwtDecoder.getCourierId(token) ?: "unknown"
    }
}