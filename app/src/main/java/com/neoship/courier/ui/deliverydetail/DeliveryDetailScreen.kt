package com.neoship.courier.ui.deliverydetail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeliveryDetailScreen(
    viewModel: DeliveryDetailViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    // Navigation auto-retour après succès + délai
    LaunchedEffect(state.validationResult) {
        if (state.validationResult is ValidationResult.Success) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        } else if (state.validationResult is ValidationResult.Failure) {
            // L'erreur est déjà visible visuellement (champ rouge + message)
            // Le haptic LongPress sert de renforcement sans API instable
        }
    }

    // Retour à la liste si demandé
    LaunchedEffect(state.shouldNavigateBack) {
        if (state.shouldNavigateBack) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        state.delivery.shortId,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // === 1. ADRESSES (TIMELINE) ===
            AddressTimelineCard(
                pickupLabel = "DÉPART",
                pickupAddress = state.delivery.pickupAddress,
                dropoffLabel = "DESTINATION",
                dropoffAddress = state.delivery.dropoffAddress
            )

            // === 2. CLIENT (si disponible) ===
            if (state.delivery.hasClientInfo) {
                ClientInfoCard(
                    clientName = state.delivery.clientName.orEmpty(),
                    clientPhone = state.delivery.clientPhone,
                    context = context,
                    snackbarHostState = snackbarHostState,
                    scope = scope
                )
            }

            // === 3. BOUTON NAVIGUER — ACTION PRINCIPALE ===
            Button(
                onClick = {
                    val lat = state.delivery.dropoffLatitude
                    val lng = state.delivery.dropoffLongitude

                    if (lat != null && lng != null) {
                        try {
                            // 1. Essayer Google Maps par son URI scheme dédié
                            val gmmUri = Uri.parse("google.navigation:q=$lat,$lng")
                            val gmmIntent = Intent(Intent.ACTION_VIEW, gmmUri)
                            context.startActivity(gmmIntent)
                        } catch (e1: Exception) {
                            try {
                                // 2. Fallback : ouvrir dans Google Maps Web via navigateur
                                val webUri = Uri.parse(
                                    "https://www.google.com/maps/dir/?api=1&destination=$lat,$lng&travelmode=driving"
                                )
                                context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
                            } catch (e2: Exception) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = "Aucune app de navigation disponible",
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                        }
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Coordonnées indisponibles pour cette course",
                                duration = SnackbarDuration.Short
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    Icons.Default.Directions,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Naviguer vers la destination",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // === 4. LIGNE DE SÉPARATION VISUELLE ===
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // === 5. SECTION OTP ===
            OtpSection(
                otpInput = state.otpInput,
                onOtpChanged = viewModel::onOtpChanged,
                isValidating = state.isValidating,
                hasError = state.hasError,
                validationResult = state.validationResult,
                onValidate = viewModel::validateOtp
            )

            // Bouton retour liste (après succès OTP)
            if (state.validationResult is ValidationResult.Success) {
                Button(
                    onClick = { onBack() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Retour à la liste")
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ────────────────────────────────────────────────────────────
// ADRESSES AVEC TIMELINE
// ────────────────────────────────────────────────────────────

@Composable
private fun AddressTimelineCard(
    pickupLabel: String,
    pickupAddress: String,
    dropoffLabel: String,
    dropoffAddress: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Timeline : ● → ligne → 🔴
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(20.dp)
            ) {
                Icon(
                    Icons.Default.Circle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(10.dp)
                )
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(28.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Colonne textes
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column {
                    Text(
                        text = pickupLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = pickupAddress,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Column {
                    Text(
                        text = dropoffLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = dropoffAddress,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────
// SECTION OTP
// ────────────────────────────────────────────────────────────

@Composable
private fun OtpSection(
    otpInput: String,
    onOtpChanged: (String) -> Unit,
    isValidating: Boolean,
    hasError: Boolean,
    validationResult: ValidationResult?,
    onValidate: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                "Validation de la livraison",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                "Saisissez le code à 4 chiffres remis au destinataire",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Champ OTP
            OutlinedTextField(
                value = otpInput,
                onValueChange = onOtpChanged,
                label = { Text("Code OTP") },
                placeholder = { Text("• • • •") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isValidating && validationResult !is ValidationResult.Success,
                isError = hasError,
                supportingText = if (hasError) {
                    {
                        Text(
                            (validationResult as? ValidationResult.Failure)?.message
                                ?: "Erreur de validation"
                        )
                    }
                } else null
            )

            // Résultat succès
            if (validationResult is ValidationResult.Success) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Livraison validée ✓",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                Text(
                    "Cette course va être retirée de votre liste",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                }

            // Bouton de validation
            Button(
                onClick = onValidate,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = otpInput.length == 4
                        && !isValidating
                        && validationResult !is ValidationResult.Success,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary
                )
            ) {
                if (isValidating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = MaterialTheme.colorScheme.onTertiary,
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Validation…")
                } else {
                    Icon(
                        Icons.Default.Verified,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Valider la livraison")
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────
// CLIENT INFO
// ────────────────────────────────────────────────────────────

@Composable
private fun ClientInfoCard(
    clientName: String,
    clientPhone: String?,
    context: android.content.Context,
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Client",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Nom
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Badge,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    clientName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }

            // Téléphone + actions
            if (!clientPhone.isNullOrBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        clientPhone,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    // Appel
                    IconButton(
                        onClick = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$clientPhone")))
                            } catch (_: Exception) {
                                scope.launch { snackbarHostState.showSnackbar("Impossible d'ouvrir le téléphone", duration = SnackbarDuration.Short) }
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.PhoneInTalk, contentDescription = "Appeler", tint = MaterialTheme.colorScheme.primary)
                    }
                    // WhatsApp
                    IconButton(
                        onClick = {
                            try {
                                val msg = "Bonjour%20${clientName.take(20).replace(" ", "%20")}%2C%20je%20suis%20votre%20coursier%20NeoShip"
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$clientPhone?text=$msg")))
                            } catch (_: Exception) {
                                scope.launch { snackbarHostState.showSnackbar("WhatsApp non disponible", duration = SnackbarDuration.Short) }
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.Forum, contentDescription = "WhatsApp", tint = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }
    }
}
