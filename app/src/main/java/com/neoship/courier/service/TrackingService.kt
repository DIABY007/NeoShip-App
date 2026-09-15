package com.neoship.courier.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.neoship.courier.data.repository.GpsRepository
import com.neoship.courier.gps.GpsTracker
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach

/**
 * Foreground Service de tracking GPS.
 *
 * Règles appliquées (android-background-task-specialist) :
 * - START_STICKY → redémarrage automatique si Android tue le service
 * - foregroundServiceType="location" (déclaré dans le Manifest)
 * - Notification persistante non-balayable
 * - Doze mode contourné via REQUEST_IGNORE_BATTERY_OPTIMIZATIONS (demandé au démarrage)
 * - Coroutine scope liée au cycle de vie du service (annulée dans onDestroy)
 *
 * Flux :
 *   GpsTracker.locationFlow → affichage log (debug) → GpsRepository.sendLocation
 */
class TrackingService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var gpsTracker: GpsTracker
    private val gpsRepository = GpsRepository()
    private var courierId: String = "unknown"

    override fun onCreate() {
        super.onCreate()
        gpsTracker = GpsTracker(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                courierId = intent.getStringExtra(EXTRA_COURIER_ID) ?: "unknown"
                startTracking()
            }
            ACTION_STOP -> {
                stopTracking()
            }
        }
        // START_STICKY : redémarre le service si tué par le système
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    // ──────────────────────────────────────────────────────────
    // Tracking
    // ──────────────────────────────────────────────────────────

    private fun startTracking() {
        val notification = NotificationHelper.buildNotification(this)
        startForeground(NotificationHelper.NOTIFICATION_ID, notification)

        // Lance la collecte des positions
        serviceScope.launch {
            gpsTracker.locationFlow(permissionGranted = true)
                .onEach { location ->
                    // Debug : log de la position
                    android.util.Log.d(
                        "TrackingService",
                        "📍 Position: ${location.latitude},${location.longitude} " +
                                "(±${location.accuracy}m)"
                    )

                    // Envoi au backend (silencieux en cas d'échec)
                    gpsRepository.sendLocation(location, courierId)
                }
                .catch { e ->
                    // Log l'erreur mais ne stoppe pas le service
                    android.util.Log.e("TrackingService", "Erreur de tracking", e)
                }
                .collect()
        }
    }

    private fun stopTracking() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        serviceScope.coroutineContext.cancelChildren()
        stopSelf()
    }

    companion object {
        const val ACTION_START = "com.neoship.courier.action.START_TRACKING"
        const val ACTION_STOP = "com.neoship.courier.action.STOP_TRACKING"
        const val EXTRA_COURIER_ID = "courier_id"
    }
}