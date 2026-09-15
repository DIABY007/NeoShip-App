package com.neoship.courier.data.repository

import com.neoship.courier.data.api.RetrofitClient
import com.neoship.courier.data.api.models.DeliveryValidateRequest
import com.neoship.courier.model.Delivery

/**
 * Repository pour les courses et la validation OTP.
 * Les endpoints /deliveries et /gps seront câblés quand le backend sera prêt.
 * Pour l'instant, les données de test permettent d'avancer l'UI.
 */
class DeliveryRepository {

    private val api = RetrofitClient.apiService

    /**
     * Retourne des courses fictives pour le développement.
     * À remplacer par un appel GET /api/deliveries quand le backend sera disponible.
     */
    fun getMockDeliveries(): List<Delivery> {
        return listOf(
            Delivery(
                id = "DEL-001",
                pickupAddress = "15 Rue de la Paix, 75001 Paris",
                dropoffAddress = "42 Avenue des Champs-Élysées, 75008 Paris",
                status = "assignée",
                otp = "1234",
                pickupLatitude = 48.8690,
                pickupLongitude = 2.3310,
                dropoffLatitude = 48.8712,
                dropoffLongitude = 2.3069
            ),
            Delivery(
                id = "DEL-002",
                pickupAddress = "8 Boulevard Saint-Michel, 75005 Paris",
                dropoffAddress = "27 Rue du Faubourg Saint-Honoré, 75008 Paris",
                status = "assignée",
                otp = "5678",
                pickupLatitude = 48.8505,
                pickupLongitude = 2.3440,
                dropoffLatitude = 48.8720,
                dropoffLongitude = 2.3190
            ),
            Delivery(
                id = "DEL-003",
                pickupAddress = "3 Place de la Bastille, 75011 Paris",
                dropoffAddress = "18 Rue de Rivoli, 75001 Paris",
                status = "en_cours",
                otp = "9012",
                pickupLatitude = 48.8530,
                pickupLongitude = 2.3690,
                dropoffLatitude = 48.8610,
                dropoffLongitude = 2.3410
            )
        )
    }

    /**
     * Valide le code OTP auprès du backend.
     * En mode dev (API indisponible), compare avec les OTP mockés.
     * @return Result.success si la validation passe, Result.failure sinon.
     */
    suspend fun validateOtp(deliveryId: String, otp: String): Result<Unit> {
        // Phase dev : essai API, fallback mock
        return try {
            val response = api.validateDelivery(DeliveryValidateRequest(deliveryId, otp))
            if (response.isSuccessful && response.body()?.success == true) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Code OTP incorrect"))
            }
        } catch (e: Exception) {
            // API indisponible → fallback mock pour le développement
            mockValidateOtp(deliveryId, otp)
        }
    }

    /**
     * Validation OTP mockée (sans backend).
     * Compare avec le code stocké dans les données fictives.
     */
    private fun mockValidateOtp(deliveryId: String, otp: String): Result<Unit> {
        val delivery = getMockDeliveries().find { it.id == deliveryId }
        return if (delivery != null && delivery.otp == otp) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("Code OTP incorrect"))
        }
    }
}