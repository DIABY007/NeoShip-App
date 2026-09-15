package com.neoship.courier.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.neoship.courier.R

/**
 * Gestion du canal de notification et construction de la notification persistante
 * pour le Foreground Service de tracking GPS.
 *
 * Règles appliquées (android-background-task-specialist) :
 * - Canal créé au démarrage de l'app (dans Application.onCreate)
 * - Notification NON balayable (Ongoing = true)
 * - Priorité LOW pour ne pas déranger le coursier
 */
object NotificationHelper {

    private const val CHANNEL_ID = "neoship_tracking"
    private const val CHANNEL_NAME = "Tracking GPS"
    private const val CHANNEL_DESC = "Notification de suivi de position"
    const val NOTIFICATION_ID = 1001

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW // Ne pas émettre de son
        ).apply {
            description = CHANNEL_DESC
            setShowBadge(false)
        }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    fun buildNotification(context: Context): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.tracking_notification_title))
            .setContentText(context.getString(R.string.tracking_notification_body))
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true)  // Non balayable
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
}