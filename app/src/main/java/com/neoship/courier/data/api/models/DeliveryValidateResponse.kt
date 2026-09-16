package com.neoship.courier.data.api.models

/**
 * Réponse de l'API pour POST /api/deliveries/validate.
 * 200 = { success: true, delivery: {...} }
 * 403 = { error: "Code OTP invalide" }
 */
data class DeliveryValidateResponse(
    val success: Boolean,
    val delivery: DeliveryValidateDto? = null,
    val error: String? = null
)

data class DeliveryValidateDto(
    val id: String,
    val status: String,
    val updatedAt: String
)