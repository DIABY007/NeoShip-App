package com.neoship.courier.model

/**
 * Modèle métier d'une course / livraison.
 * Les données viennent de l'API backend (GET /api/deliveries).
 */
data class Delivery(
    val id: String,
    val pickupAddress: String,
    val dropoffAddress: String,
    val status: String,       // "assignée", "en_cours", "livrée"
    val otp: String,          // code à 4 chiffres (pour test uniquement — le vrai OTP est côté serveur)
    val pickupLatitude: Double? = null,
    val pickupLongitude: Double? = null,
    val dropoffLatitude: Double? = null,
    val dropoffLongitude: Double? = null
) {
    val isCompleted: Boolean get() = status == "livrée"

    val statusLabel: String get() = when (status) {
        "assignée" -> "Assignée"
        "en_cours" -> "En cours"
        "livrée" -> "Livrée ✓"
        else -> status
    }
}