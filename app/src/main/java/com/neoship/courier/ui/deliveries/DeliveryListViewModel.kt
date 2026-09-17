package com.neoship.courier.ui.deliveries

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neoship.courier.data.local.CompletedDeliveriesStorage
import com.neoship.courier.data.repository.AuthRepository
import com.neoship.courier.data.repository.DeliveryRepository
import com.neoship.courier.data.repository.UpdateManager
import com.neoship.courier.model.Delivery
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeliveryListUiState(
    val deliveries: List<Delivery> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    // Mise à jour
    val updateAvailable: Boolean = false,
    val isDownloading: Boolean = false,
    val downloadProgress: String = "",
    val apkUrl: String = ""
)

class DeliveryListViewModel(
    private val deliveryRepository: DeliveryRepository,
    private val authRepository: AuthRepository,
    private val completedStorage: CompletedDeliveriesStorage
) : ViewModel() {

    private val _uiState = MutableStateFlow(DeliveryListUiState())
    val uiState: StateFlow<DeliveryListUiState> = _uiState.asStateFlow()

    private val updateManager = UpdateManager()

    init {
        loadDeliveries()
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            updateManager.checkForUpdate().fold(
                onSuccess = { (available, url) ->
                    if (available) _uiState.value = _uiState.value.copy(updateAvailable = true, apkUrl = url)
                },
                onFailure = { /* silence */ }
            )
        }
    }

    fun startUpdate(context: Context) {
        val url = _uiState.value.apkUrl
        if (url.isBlank()) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDownloading = true, downloadProgress = "Téléchargement…")
            updateManager.downloadApk(context, url).fold(
                onSuccess = { file ->
                    _uiState.value = _uiState.value.copy(isDownloading = false)
                    updateManager.installApk(context, file)
                },
                onFailure = { error -> _uiState.value = _uiState.value.copy(isDownloading = false, error = error.message) }
            )
        }
    }

    fun loadDeliveries() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val allDeliveries = deliveryRepository.getDeliveries()
                    .getOrDefault(emptyList()) // Fallback mock si API down
                val completedIds = completedStorage.getCompletedIds()
                val filtered = allDeliveries.filter { it.id !in completedIds }

                _uiState.value = _uiState.value.copy(
                    deliveries = filtered,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Impossible de charger les courses : ${e.localizedMessage}"
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)
            try {
                val allDeliveries = deliveryRepository.getDeliveries()
                    .getOrDefault(emptyList())
                val completedIds = completedStorage.getCompletedIds()
                val filtered = allDeliveries.filter { it.id !in completedIds }

                _uiState.value = _uiState.value.copy(
                    deliveries = filtered,
                    isRefreshing = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = "Impossible de rafraîchir : ${e.localizedMessage}"
                )
            }
        }
    }

    fun markDeliveryCompleted(id: String) {
        completedStorage.addCompletedId(id)
        // Re-filtre la liste immédiatement
        val updated = _uiState.value.deliveries.filter { it.id != id }
        _uiState.value = _uiState.value.copy(deliveries = updated)
    }

    fun logout() {
        completedStorage.clear()
        authRepository.logout()
    }

    /** ID du coursier extrait depuis la réponse login */
    fun getCourierId(): String = authRepository.getUserId() ?: "unknown"
}