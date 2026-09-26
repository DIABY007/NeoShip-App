package com.neoship.courier.data.api.models

/**
 * Réponse de l'API pour GET /api/settings.
 */
data class AppSettingsResponse(
    val pricePerKm: Double = 0.0,
    val minPrice: Double = 0.0,
    val beneficiaryNumber: String? = null,
    val version: AppVersionResponse? = null
)