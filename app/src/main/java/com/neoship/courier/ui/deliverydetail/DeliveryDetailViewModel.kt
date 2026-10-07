package com.neoship.courier.ui.deliverydetail

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neoship.courier.data.api.RetrofitClient
import com.neoship.courier.data.api.models.AppVersionResponse
import com.neoship.courier.data.repository.DeliveryRepository
import com.neoship.courier.model.Delivery
import com.neoship.courier.util.QrCodeGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeliveryDetailUiState(
    val delivery: Delivery,
    val otpInput: String = "",
    val otpRecipientName: String = "",
    val isValidating: Boolean = false,
    val isStarting: Boolean = false,
    val isFailing: Boolean = false,
    val validationResult: ValidationResult? = null,
    val hasError: Boolean = false,
    val shouldNavigateBack: Boolean = false,
    // Failure reason modal
    val showFailureDialog: Boolean = false,
    val selectedFailureReason: String = "",
    val isSubmittingFailure: Boolean = false,
    val failureError: String? = null,
    // Mobile Money QR
    val qrCodeBitmap: Bitmap? = null,
    val qrUssdCode: String = "",
    val beneficiaryNumber: String? = null
)

sealed class ValidationResult {
    data object Success : ValidationResult()
    data class Failure(val message: String) : ValidationResult()
}

/**
 * Motifs d'échec disponibles pour "Signaler un problème".
 */
val FAILURE_REASONS = listOf(
    "destinataire_absent",
    "destinataire_injoignable",
    "mauvaise_adresse",
    "colis_endommage",
    "refus_colis",
    "erreur_livraison"
)

val FAILURE_REASONS_LABEL: Map<String, String> = mapOf(
    "destinataire_absent" to "Destinataire absent",
    "destinataire_injoignable" to "Destinataire injoignable",
    "mauvaise_adresse" to "Mauvaise adresse",
    "colis_endommage" to "Colis endommagé",
    "refus_colis" to "Refus du colis",
    "erreur_livraison" to "Erreur de livraison"
)

class DeliveryDetailViewModel(
    private val delivery: Delivery,
    private val deliveryRepository: DeliveryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliveryDetailUiState(delivery = delivery))
    val uiState: StateFlow<DeliveryDetailUiState> = _uiState.asStateFlow()

    init {
        loadBeneficiary()
    }

    private fun loadBeneficiary() {
        viewModelScope.launch {
            try {
                val response = RetrofitClient.apiService.getAppVersion()
                if (response.isSuccessful) {
                    val number = response.body()?.beneficiaryNumber
                    if (!number.isNullOrBlank()) {
                        val price = (delivery.price?.toInt() ?: 0).coerceAtLeast(100)
                        val ussd = QrCodeGenerator.buildUssdUri(number, price)
                        val bitmap = QrCodeGenerator.generate(ussd, 512)
                        _uiState.value = _uiState.value.copy(
                            beneficiaryNumber = number,
                            qrUssdCode = ussd,
                            qrCodeBitmap = bitmap
                        )
                    }
                }
            } catch (_: Exception) { }
        }
    }

    // ── OTP ──

    fun onRecipientNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(otpRecipientName = name)
    }

    fun onOtpChanged(otp: String) {
        if (otp.length <= 4 && otp.all { it.isDigit() }) {
            _uiState.value = _uiState.value.copy(
                otpInput = otp,
                validationResult = null,
                hasError = false
            )
            if (otp.length == 4 && !_uiState.value.isValidating) {
                validateOtp()
            }
        }
    }

    fun validateOtp() {
        val state = _uiState.value
        if (state.otpInput.length != 4) {
            _uiState.value = state.copy(
                validationResult = ValidationResult.Failure("Le code doit faire 4 chiffres"),
                hasError = true
            )
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isValidating = true)
            val recipientName = state.otpRecipientName.ifBlank { null }
            val result = deliveryRepository.validateOtp(delivery.id, state.otpInput, recipientName)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(
                        isValidating = false,
                        validationResult = ValidationResult.Success,
                        hasError = false
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isValidating = false,
                        validationResult = ValidationResult.Failure(error.message ?: "Code OTP incorrect"),
                        hasError = true
                    )
                }
            )
        }
    }

    // ── Démarrer la course ──

    fun startDelivery() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isStarting = true)
            val result = deliveryRepository.startDelivery(delivery.id)
            result.fold(
                onSuccess = {
                    val updated = _uiState.value.delivery.copy(status = "in_progress")
                    _uiState.value = _uiState.value.copy(delivery = updated, isStarting = false)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isStarting = false,
                        validationResult = ValidationResult.Failure(error.message ?: "Impossible de démarrer"),
                        hasError = true
                    )
                }
            )
        }
    }

    // ── Signaler un problème ──

    /** Ouvre le dialogue de sélection du motif */
    fun openFailureDialog() {
        _uiState.value = _uiState.value.copy(
            showFailureDialog = true,
            selectedFailureReason = "",
            failureError = null
        )
    }

    /** Ferme le dialogue sans agir */
    fun closeFailureDialog() {
        _uiState.value = _uiState.value.copy(
            showFailureDialog = false,
            selectedFailureReason = "",
            failureError = null
        )
    }

    /** Sélectionne un motif */
    fun selectFailureReason(reason: String) {
        _uiState.value = _uiState.value.copy(selectedFailureReason = reason)
    }

    /** Confirme et envoie l'échec avec le motif sélectionné */
    fun confirmFailure() {
        val reason = _uiState.value.selectedFailureReason
        if (reason.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSubmittingFailure = true)
            val result = deliveryRepository.failDelivery(delivery.id, reason)
            result.fold(
                onSuccess = {
                    val updated = _uiState.value.delivery.copy(status = "failed")
                    _uiState.value = _uiState.value.copy(
                        delivery = updated,
                        isFailing = true,
                        showFailureDialog = false,
                        isSubmittingFailure = false
                    )
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isSubmittingFailure = false,
                        failureError = error.message ?: "Erreur lors du signalement"
                    )
                }
            )
        }
    }

    fun onNavigateBack() {
        _uiState.value = _uiState.value.copy(shouldNavigateBack = true)
    }
}