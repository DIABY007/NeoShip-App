package com.neoship.courier.ui.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neoship.courier.data.repository.AuthRepository
import com.neoship.courier.data.repository.UpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    // Mise à jour
    val updateAvailable: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: String = "",
    val apkUrl: String = ""
)

class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val updateManager = UpdateManager()

    init {
        if (authRepository.isLoggedIn()) {
            _uiState.value = _uiState.value.copy(isLoggedIn = true)
        }
    }

    fun checkForUpdates(context: Context) {
        viewModelScope.launch {
            val result = updateManager.checkForUpdate()
            result.fold(
                onSuccess = { (available, url) ->
                    if (available) {
                        _uiState.value = _uiState.value.copy(
                            updateAvailable = true,
                            apkUrl = url
                        )
                    }
                },
                onFailure = { /* Serveur indispo, on ignore silencieusement */ }
            )
        }
    }

    fun startUpdate(context: Context) {
        val url = _uiState.value.apkUrl
        if (url.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDownloading = true, downloadProgress = "Téléchargement…")

            val downloadResult = updateManager.downloadApk(context, url)
            downloadResult.fold(
                onSuccess = { file ->
                    _uiState.value = _uiState.value.copy(
                        isDownloading = false,
                        downloadProgress = "Installation…"
                    )
                    updateManager.installApk(context, file)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isDownloading = false,
                        error = error.message
                    )
                }
            )
        }
    }

    fun onEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(email = email, error = null)
    }

    fun onPasswordChanged(password: String) {
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }

    fun login() {
        val state = _uiState.value
        if (state.email.isBlank()) {
            _uiState.value = state.copy(error = "Veuillez saisir votre email")
            return
        }
        if (state.password.isBlank()) {
            _uiState.value = state.copy(error = "Veuillez saisir votre mot de passe")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = authRepository.login(state.email, state.password)
            result.fold(
                onSuccess = {
                    _uiState.value = _uiState.value.copy(isLoading = false, isLoggedIn = true)
                },
                onFailure = { error ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Erreur inconnue"
                    )
                }
            )
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}