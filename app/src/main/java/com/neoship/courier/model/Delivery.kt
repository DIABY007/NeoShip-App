package com.neoship.courier.model

import com.neoship.courier.data.api.models.DeliveryDto

/**
 * Modèle métier d'une course / livraison.
 * Construit à partir du DTO de l'API backend.
 */
data class Delivery(
    val id: String,
    val pickupAddress: String,
    val dropoffAddress: String,
    val status: String,
    val pickupLatitude: Double? = null,
    val pickupLongitude: Double? = null,
    val dropoffLatitude: Double? = null,
    val dropoffLongitude: Double? = null,
    val price: Double? = null,
    val distance: Double? = null,
    val courierId: String? = null
) {
    val isCompleted: Boolean get() = status == "delivered"

    val statusLabel: String get() = when (status) {
        "pending" -> "En attente"
        "assigned" -> "Assignée"
        "in_progress" -> "En cours"
        "delivered" -> "Livrée ✓"
        "failed" -> "Échouée"
        else -> status
    }
}

/** Convertit un DTO API en modèle métier */
fun DeliveryDto.toDelivery(): Delivery {
    return Delivery(
        id = id,
        pickupAddress = pickupAddress,
        dropoffAddress = dropoffAddress,
        status = status,
        pickupLatitude = pickupLat,
        pickupLongitude = pickupLng,
        dropoffLatitude = dropoffLat,
        dropoffLongitude = dropoffLng,
        price = price,
        distance = distance,
        courierId = courierId
    )
}