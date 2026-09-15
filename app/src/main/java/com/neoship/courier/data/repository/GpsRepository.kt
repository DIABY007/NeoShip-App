package com.neoship.courier.data.repository

import android.location.Location
import com.neoship.courier.data.api.RetrofitClient
import com.neoship.courier.data.api.models.GpsUpdateRequest

/**
 * Repository d'envoi des positions GPS.
 * Poste les coordonnées vers /api/gps/update.
 * Les erreurs réseau sont silencieuses (le tracking ne doit pas s'arrêter si une requête échoue).
 */
class GpsRepository {

    private val api = RetrofitClient.apiService

    /**
     * Envoie une position au backend.
     * @return true si l'envoi a réussi, false sinon
     */
    suspend fun sendLocation(
        location: Location,
        courierId: String
    ): Boolean {
        return try {
            val request = GpsUpdateRequest(
                latitude = location.latitude,
                longitude = location.longitude,
                timestamp = System.currentTimeMillis(),
                courierId = courierId
            )
            val response = api.updateGps(request)
            response.isSuccessful
        } catch (e: Exception) {
            // Échec réseau — on ne coupe pas le tracking pour autant
            false
        }
    }
}