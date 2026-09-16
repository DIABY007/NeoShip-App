package com.neoship.courier.model

import com.neoship.courier.data.api.models.DeliveryDto
import java.util.Locale

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
    val courierId: String? = null,
    val clientName: String? = null,
    val clientPhone: String? = null
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

    /** ID court : 6 premiers caractères de l'UUID, en majuscules */
    val shortId: String get() = "#${id.take(6).uppercase(Locale.ROOT)}"

    /** Vrai si le client a au moins un nom */
    val hasClientInfo: Boolean get() = !clientName.isNullOrBlank()
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
        courierId = courierId,
        clientName = clientName,
        clientPhone = clientPhone
    )
}