package com.neoship.courier.ui.deliverydetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neoship.courier.data.repository.DeliveryRepository
import com.neoship.courier.model.Delivery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeliveryDetailUiState(
    val delivery: Delivery,
    val otpInput: String = "",
    val isValidating: Boolean = false,
    val validationResult: ValidationResult? = null,
    val hasError: Boolean = false,
    val shouldNavigateBack: Boolean = false
)

sealed class ValidationResult {
    data object Success : ValidationResult()
    data class Failure(val message: String) : ValidationResult()
}

class DeliveryDetailViewModel(
    private val delivery: Delivery,
    private val deliveryRepository: DeliveryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliveryDetailUiState(delivery = delivery))
    val uiState: StateFlow<DeliveryDetailUiState> = _uiState.asStateFlow()

    fun onOtpChanged(otp: String) {
        if (otp.length <= 4 && otp.all { it.isDigit() }) {
            _uiState.value = _uiState.value.copy(
                otpInput = otp,
                validationResult = null,
                hasError = false
            )

            // Auto-submit dès que le 4e chiffre est saisi
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

            val result = deliveryRepository.validateOtp(delivery.id, state.otpInput)
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
                        validationResult = ValidationResult.Failure(
                            error.message ?: "Code OTP incorrect"
                        ),
                        hasError = true
                    )
                }
            )
        }
    }

    fun onNavigateBack() {
        _uiState.value = _uiState.value.copy(shouldNavigateBack = true)
    }
}