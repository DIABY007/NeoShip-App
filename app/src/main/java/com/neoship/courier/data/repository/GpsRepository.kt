package com.neoship.courier.data.repository

import android.location.Location
import com.neoship.courier.data.api.RetrofitClient
import com.neoship.courier.data.api.models.GpsUpdateRequest

/**
 * Repository d'envoi des positions GPS.
 * POST /api/gps/update avec les coordonnées.
 * Rate limit : 30 req/min côté serveur — notre intervalle de 10s (6 req/min) est sûr.
 */
class GpsRepository(
    private val authRepository: AuthRepository
) {
    private val api = RetrofitClient.apiService

    suspend fun sendLocation(location: Location): Boolean {
        return try {
            val token = authRepository.getToken()
                ?: return false

            val request = GpsUpdateRequest(
                latitude = location.latitude,
                longitude = location.longitude,
                deliveryId = null // Optionnel — associé à une course plus tard
            )
            val response = api.updateGps("Bearer $token", request)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}