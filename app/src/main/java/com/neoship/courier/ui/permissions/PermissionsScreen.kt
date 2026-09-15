package com.neoship.courier.ui.permissions

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Écran de tunnel de permissions.
 * Guide le coursier étape par étape pour accorder toutes les permissions nécessaires
 * au tracking GPS en arrière-plan.
 *
 * Issues du skill touch-psychology :
 * - Actions primaires dans la thumb zone (boutons en bas)
 * - Feedback immédiat via les icônes d'état
 * - Explications claires (pas de jargon technique)
 *
 * Issues du skill platform-android :
 * - Utilisation des API Activity Result Contracts
 * - Respect du flow permission Android (fine → background → notification → batterie)
 */
@Composable
fun PermissionsScreen(
    viewModel: PermissionsViewModel,
    onPermissionsComplete: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Vérification initiale
    LaunchedEffect(Unit) {
        viewModel.checkInitialState(context)
    }

    // Redirection si déjà toutes accordées
    LaunchedEffect(state.allGranted) {
        if (state.allGranted) {
            onPermissionsComplete()
        }
    }

    // ── Launchers de permissions ──
    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onLocationGranted()
    }

    val backgroundLocationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onBackgroundLocationGranted()
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onNotificationGranted()
        else viewModel.onNotificationGranted() // On continue même si refusé (notification moins critique)
    }

    // Déclenchement automatique de la permission en cours
    LaunchedEffect(state.currentStep) {
        when (state.currentStep) {
            PermissionStep.LOCATION -> {
                if (!state.locationGranted) {
                    locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
            PermissionStep.BACKGROUND_LOCATION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && !state.backgroundLocationGranted) {
                    backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            }
            PermissionStep.NOTIFICATION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !state.notificationGranted) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            PermissionStep.COMPLETED -> {
                onPermissionsComplete()
            }
            else -> { /* BATTERY_OPTIMIZATION : attend action utilisateur */ }
        }
    }

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Contenu variable selon l'étape
                    PermissionStepContent(
                        step = state.currentStep,
                        onSkipBattery = viewModel::onBatteryOptimizationSkipped,
                        onOpenBatterySettings = { viewModel.openBatterySettings(context) },
                        onBatteryDone = viewModel::onBatteryOptimizationDisabled
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionStepContent(
    step: PermissionStep,
    onSkipBattery: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onBatteryDone: () -> Unit
) {
    val (icon, title, description) = when (step) {
        PermissionStep.LOCATION -> Triple(
            Icons.Default.MyLocation,
            "Localisation",
            "NeoShip a besoin de votre position pour tracker vos courses en temps réel."
        )
        PermissionStep.BACKGROUND_LOCATION -> Triple(
            Icons.Default.LocationOff,
            "Localisation en arrière-plan",
            "Permettez le tracking même lorsque l'application est réduite ou l'écran verrouillé."
        )
        PermissionStep.NOTIFICATION -> Triple(
            Icons.Default.Notifications,
            "Notifications",
            "Une notification persistante indique que votre position est enregistrée."
        )
        PermissionStep.BATTERY_OPTIMIZATION -> Triple(
            Icons.Default.BatteryFull,
            "Optimisation batterie",
            "Désactivez l'optimisation batterie pour éviter que le tracking ne soit interrompu en veille."
        )
        PermissionStep.COMPLETED -> Triple(
            Icons.Default.CheckCircle,
            "Prêt !",
            "Toutes les permissions sont configurées. Bonne route !"
        )
    }

    // Icône
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(64.dp),
        tint = MaterialTheme.colorScheme.primary
    )

    // Titre
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )

    // Description
    Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 8.dp)
    )

    Spacer(Modifier.height(8.dp))

    // Boutons selon l'étape
    when (step) {
        PermissionStep.BATTERY_OPTIMIZATION -> {
            Button(
                onClick = onOpenBatterySettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Ouvrir les paramètres")
            }

            OutlinedButton(
                onClick = onSkipBattery,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Passer (recommandé)", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        PermissionStep.COMPLETED -> {
            Button(
                onClick = onBatteryDone,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Commencer")
            }
        }

        else -> {
            // Les permissions runtime sont demandées automatiquement via LaunchedEffect
            // On attend — l'UI reste affichée
            if (step == PermissionStep.LOCATION || step == PermissionStep.BACKGROUND_LOCATION) {
                Text(
                    "Une demande de permission va apparaître…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}