package com.neoship.courier.data.api.models

/**
 * Réponse de l'API pour GET /api/deliveries.
 */
data class DeliveriesResponse(
    val deliveries: List<DeliveryDto>,
    val count: Int,
    val limit: Int,
    val offset: Int
)

/**
 * Modèle d'une course côté API.
 * Les noms de champs suivent exactement le contrat backend.
 */
data class DeliveryDto(
    val id: String,
    val pickupAddress: String,
    val dropoffAddress: String,
    val pickupLat: Double?,
    val pickupLng: Double?,
    val dropoffLat: Double?,
    val dropoffLng: Double?,
    val status: String,
    val price: Double?,
    val distance: Double?,
    val courierId: String?,
    val createdAt: String,
    val updatedAt: String
)