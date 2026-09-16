package com.neoship.courier.data.repository

import android.location.Location
import com.neoship.courier.data.api.RetrofitClient
import com.neoship.courier.data.api.models.GpsUpdateRequest

/**
 * Repository d'envoi des positions GPS.
 * POST /api/gps/update avec les coordonnées.
 * Le token JWT est récupéré automatiquement via le tokenProvider de RetrofitClient.
 */
class GpsRepository {
    private val api = RetrofitClient.apiService

    suspend fun sendLocation(location: Location): Boolean {
        return try {
            val request = GpsUpdateRequest(
                latitude = location.latitude,
                longitude = location.longitude,
                deliveryId = null
            )
            val response = api.updateGps("Bearer ${RetrofitClient.tokenProvider?.invoke() ?: ""}", request)
            response.isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}