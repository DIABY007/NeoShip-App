package com.neoship.courier.data.api.models

/**
 * Payload pour POST /api/gps/update.
 * `deliveryId` est optionnel côté serveur.
 * Rate limit : 30 req/min.
 */
data class GpsUpdateRequest(
    val latitude: Double,
    val longitude: Double,
    val deliveryId: String? = null
)