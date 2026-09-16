package com.neoship.courier.data.api.models

/**
 * Réponse de l'API pour POST /api/auth/login.
 * Correspond au contrat V1.0 du backend.
 */
data class LoginResponse(
    val user: UserInfo,
    val token: String
)

data class UserInfo(
    val id: String,
    val email: String,
    val name: String,
    val role: String,
    val phone: String
)