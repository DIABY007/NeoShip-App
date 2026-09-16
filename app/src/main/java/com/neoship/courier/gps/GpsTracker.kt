package com.neoship.courier.gps

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.Manifest
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Wrapper autour de FusedLocationProviderClient.
 * Fournit un Flow de positions GPS à l'intervalle configuré.
 *
 * Issues du skill android-background-task-specialist :
 * - Intervalle 10s (respect batterie, précision suffisante pour tracking coursier)
 * - Priorité HIGH_ACCURACY (nécessaire pour tracking en extérieur)
 * - Pas d'AlarmManager (WorkManager inadapté pour du temps réel)
 */
class GpsTracker(private val context: Context) {

    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    /**
     * Dernière position connue rapide (au démarrage du service).
     */
    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(): Location? {
        return try {
            fusedLocationClient.lastLocation.await()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Flow de positions en continu. Émet une position toutes les ~10 secondes.
     * Se stoppe quand le collecteur annule le coroutine scope.
     *
     * @param permissionGranted Flag indiquant que les permissions sont déjà acquises
     */
    @SuppressLint("MissingPermission")
    fun locationFlow(permissionGranted: Boolean): Flow<Location> = callbackFlow {
        if (!permissionGranted) {
            close(Exception("Permissions de localisation non accordées"))
            return@callbackFlow
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10_000L  // 10 secondes entre chaque mise à jour
        ).apply {
            setMinUpdateIntervalMillis(5_000L)  // Minimum 5s même si autre app demande
            setWaitForAccurateLocation(false)
        }.build()

        val cancellationTokenSource = CancellationTokenSource()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { location ->
                    trySend(location)
                }
            }

            override fun onLocationAvailability(availability: LocationAvailability) {
                if (!availability.isLocationAvailable) {
                    // La localisation est momentanément indisponible
                    // On continue d'écouter — elle peut revenir
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            callback,
            context.mainLooper
        ).addOnFailureListener { e ->
            close(e)
        }

        awaitClose {
            fusedLocationClient.removeLocationUpdates(callback)
            cancellationTokenSource.cancel()
        }
    }

    /**
     * Vérifie si les permissions de localisation sont accordées.
     */
    fun hasLocationPermissions(): Boolean {
        return context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    fun hasBackgroundLocationPermission(): Boolean {
        return context.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    }
}