package com.neoship.courier

import android.app.Application
import com.neoship.courier.data.local.CompletedDeliveriesStorage
import com.neoship.courier.data.local.TokenManager
import com.neoship.courier.service.NotificationHelper

class NeoShipApp : Application() {

    lateinit var tokenManager: TokenManager
        private set

    lateinit var completedDeliveriesStorage: CompletedDeliveriesStorage
        private set

    override fun onCreate() {
        super.onCreate()
        tokenManager = TokenManager(this)
        completedDeliveriesStorage = CompletedDeliveriesStorage(this)
        // Canal de notification pour le Foreground Service GPS (Android 8+)
        NotificationHelper.createChannel(this)
    }
}