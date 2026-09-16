package com.neoship.courier.data.repository

import com.neoship.courier.data.api.RetrofitClient
import com.neoship.courier.data.api.models.DeliveryValidateRequest
import com.neoship.courier.model.Delivery
import com.neoship.courier.model.toDelivery

/**
 * Repository des courses.
 * Appelle l'API réelle GET /api/deliveries et POST /api/deliveries/validate.
 * Le fallback mock est conservé pour le développement sans backend.
 */
class DeliveryRepository(
    private val authRepository: AuthRepository
) {
    private val api = RetrofitClient.apiService

    /**
     * Récupère les courses assignées au coursier connecté.
     * Le token Bearer est injecté automatiquement par l'interceptor RetrofitClient.
     */
    suspend fun getDeliveries(): Result<List<Delivery>> {
        return try {
            val response = api.getDeliveries()
            if (response.isSuccessful) {
                val body = response.body()
                    ?: return Result.failure(Exception("Réponse vide"))

                val deliveries = body.deliveries.map { it.toDelivery() }
                Result.success(deliveries)
            } else {
                val errorBody = response.errorBody()?.string()
                android.util.Log.w("DeliveryRepo", "API error ${response.code()}: $errorBody")
                Result.failure(Exception("Erreur serveur (${response.code()})"))
            }
        } catch (e: Exception) {
            android.util.Log.w("DeliveryRepo", "API call failed, mock fallback", e)
            Result.success(getMockDeliveries())
        }
    }

    /**
     * Valide le code OTP.
     * Le token Bearer est injecté automatiquement par l'interceptor RetrofitClient.
     */
    suspend fun validateOtp(deliveryId: String, otp: String): Result<Unit> {
        return try {
            val response = api.validateDelivery(
                DeliveryValidateRequest(deliveryId, otp)
            )

            when (response.code()) {
                200 -> {
                    if (response.body()?.success == true) {
                        Result.success(Unit)
                    } else {
                        Result.failure(Exception("Erreur de validation"))
                    }
                }
                403 -> {
                    val err = response.errorBody()?.string() ?: "Code OTP invalide"
                    Result.failure(Exception(err))
                }
                409 -> Result.failure(Exception("Cette livraison est déjà livrée"))
                else -> Result.failure(Exception("Erreur serveur (${response.code()})"))
            }
        } catch (e: Exception) {
            mockValidateOtp(deliveryId, otp)
        }
    }

    // ── Fallback mock ──

    private val mockDeliveries = listOf(
        Delivery(
            id = "mock-del-001",
            pickupAddress = "15 Rue de la Paix, 75001 Paris",
            dropoffAddress = "42 Avenue des Champs-Élysées, 75008 Paris",
            status = "assigned",
            pickupLatitude = 48.8690,
            pickupLongitude = 2.3310,
            dropoffLatitude = 48.8712,
            dropoffLongitude = 2.3069
        ),
        Delivery(
            id = "mock-del-002",
            pickupAddress = "8 Boulevard Saint-Michel, 75005 Paris",
            dropoffAddress = "27 Rue du Faubourg Saint-Honoré, 75008 Paris",
            status = "assigned",
            pickupLatitude = 48.8505,
            pickupLongitude = 2.3440,
            dropoffLatitude = 48.8720,
            dropoffLongitude = 2.3190
        )
    )

    private fun getMockDeliveries(): List<Delivery> = mockDeliveries

    private fun mockValidateOtp(deliveryId: String, otp: String): Result<Unit> {
        val delivery = mockDeliveries.find { it.id == deliveryId && otp == "1234" }
        return if (delivery != null) Result.success(Unit)
        else Result.failure(Exception("Code OTP incorrect"))
    }
}