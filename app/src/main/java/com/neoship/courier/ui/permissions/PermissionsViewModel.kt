package com.neoship.courier.ui.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Étape du tunnel de permissions.
 * Chaque étape doit être complétée avant de passer à la suivante.
 */
enum class PermissionStep {
    /** Permission de localisation (fine) */
    LOCATION,
    /** Permission de localisation en arrière-plan (Android 10+) */
    BACKGROUND_LOCATION,
    /** Permission de notification (Android 13+) */
    NOTIFICATION,
    /** Optimisation batterie (Doze mode) */
    BATTERY_OPTIMIZATION,
    /** Toutes les permissions accordées */
    COMPLETED
}

data class PermissionsUiState(
    val currentStep: PermissionStep = PermissionStep.LOCATION,
    val locationGranted: Boolean = false,
    val backgroundLocationGranted: Boolean = false,
    val notificationGranted: Boolean = (Build.VERSION.SDK_INT < 33),
    val batteryOptimizationDisabled: Boolean = false,
    val allGranted: Boolean = false
)

class PermissionsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionsUiState())
    val uiState: StateFlow<PermissionsUiState> = _uiState.asStateFlow()

    fun checkInitialState(context: Context) {
        val fineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val backgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        val batteryOff = isBatteryOptimizationDisabled(context)

        val nextStep = when {
            !fineLocation -> PermissionStep.LOCATION
            !backgroundLocation -> PermissionStep.BACKGROUND_LOCATION
            !notification -> PermissionStep.NOTIFICATION
            !batteryOff -> PermissionStep.BATTERY_OPTIMIZATION
            else -> PermissionStep.COMPLETED
        }

        _uiState.value = _uiState.value.copy(
            locationGranted = fineLocation,
            backgroundLocationGranted = backgroundLocation,
            notificationGranted = notification,
            batteryOptimizationDisabled = batteryOff,
            currentStep = nextStep,
            allGranted = (nextStep == PermissionStep.COMPLETED)
        )
    }

    fun onLocationGranted() {
        _uiState.value = _uiState.value.copy(
            locationGranted = true,
            currentStep = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                PermissionStep.BACKGROUND_LOCATION
            else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                PermissionStep.NOTIFICATION
            else
                PermissionStep.BATTERY_OPTIMIZATION
        )
    }

    fun onBackgroundLocationGranted() {
        _uiState.value = _uiState.value.copy(
            backgroundLocationGranted = true,
            currentStep = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                PermissionStep.NOTIFICATION
            else
                PermissionStep.BATTERY_OPTIMIZATION
        )
    }

    fun onNotificationGranted() {
        _uiState.value = _uiState.value.copy(
            notificationGranted = true,
            currentStep = PermissionStep.BATTERY_OPTIMIZATION
        )
    }

    fun onBatteryOptimizationSkipped() {
        // L'utilisateur peut choisir de ne pas désactiver l'optimisation
        // Le tracking fonctionnera moins bien en veille mais ne cassera pas
        completeAll()
    }

    fun onBatteryOptimizationDisabled() {
        completeAll()
    }

    private fun completeAll() {
        _uiState.value = _uiState.value.copy(
            batteryOptimizationDisabled = true,
            currentStep = PermissionStep.COMPLETED,
            allGranted = true
        )
    }

    fun isBatteryOptimizationDisabled(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun openBatterySettings(context: Context) {
        val intent = Intent(
            android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}")
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}