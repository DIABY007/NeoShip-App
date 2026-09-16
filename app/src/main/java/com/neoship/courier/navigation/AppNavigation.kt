package com.neoship.courier.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.neoship.courier.data.local.CompletedDeliveriesStorage
import com.neoship.courier.data.local.TokenManager
import com.neoship.courier.data.repository.AuthRepository
import com.neoship.courier.data.repository.DeliveryRepository
import com.neoship.courier.ui.deliveries.DeliveryListScreen
import com.neoship.courier.ui.deliveries.DeliveryListViewModel
import com.neoship.courier.ui.deliverydetail.DeliveryDetailScreen
import com.neoship.courier.ui.deliverydetail.DeliveryDetailViewModel
import com.neoship.courier.ui.deliverydetail.ValidationResult
import com.neoship.courier.ui.login.LoginScreen
import com.neoship.courier.ui.login.LoginViewModel
import com.neoship.courier.ui.permissions.PermissionsScreen
import com.neoship.courier.ui.permissions.PermissionsViewModel

object Routes {
    const val LOGIN = "login"
    const val PERMISSIONS = "permissions"
    const val DELIVERY_LIST = "delivery_list"
    const val DELIVERY_DETAIL = "delivery_detail/{deliveryId}"
    fun deliveryDetail(id: String) = "delivery_detail/$id"
}

/** Clé savedStateHandle pour remonter le résultat de validation */
private const val KEY_VALIDATED_DELIVERY = "validated_delivery"

@Composable
fun AppNavHost(
    navController: NavHostController,
    tokenManager: TokenManager,
    completedDeliveriesStorage: CompletedDeliveriesStorage
) {
    val authRepository = remember { AuthRepository(tokenManager) }
    val deliveryRepository = remember { DeliveryRepository(authRepository) }

    val startDest = if (!authRepository.isLoggedIn()) {
        Routes.LOGIN
    } else {
        Routes.PERMISSIONS
    }

    NavHost(
        navController = navController,
        startDestination = startDest
    ) {
        // === ÉCRAN DE CONNEXION ===
        composable(Routes.LOGIN) {
            val loginViewModel: LoginViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return LoginViewModel(authRepository) as T
                    }
                }
            )
            LoginScreen(
                viewModel = loginViewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.PERMISSIONS) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        // === TUNNEL DE PERMISSIONS ===
        composable(Routes.PERMISSIONS) {
            val permissionsViewModel: PermissionsViewModel = viewModel()
            PermissionsScreen(
                viewModel = permissionsViewModel,
                onPermissionsComplete = {
                    navController.navigate(Routes.DELIVERY_LIST) {
                        popUpTo(Routes.PERMISSIONS) { inclusive = true }
                    }
                }
            )
        }

        // === LISTE DES COURSES ===
        composable(Routes.DELIVERY_LIST) { backStackEntry ->
            val listViewModel: DeliveryListViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return DeliveryListViewModel(
                            deliveryRepository,
                            authRepository,
                            completedDeliveriesStorage
                        ) as T
                    }
                }
            )

            // Observer le résultat de validation en provenance du détail
            val savedStateHandle = backStackEntry.savedStateHandle
            LaunchedEffect(savedStateHandle) {
                val validatedId = savedStateHandle.get<String>(KEY_VALIDATED_DELIVERY)
                if (validatedId != null) {
                    listViewModel.markDeliveryCompleted(validatedId)
                    savedStateHandle.remove<String>(KEY_VALIDATED_DELIVERY)
                }
            }

            DeliveryListScreen(
                viewModel = listViewModel,
                onDeliveryClick = { delivery ->
                    navController.navigate(Routes.deliveryDetail(delivery.id))
                },
                onLogout = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // === DÉTAIL D'UNE COURSE ===
        composable(
            route = Routes.DELIVERY_DETAIL,
            arguments = listOf(
                navArgument("deliveryId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val deliveryId = backStackEntry.arguments?.getString("deliveryId") ?: return@composable

            // Charge la course depuis l'API (ou fallback mock)
            var delivery by remember { mutableStateOf<com.neoship.courier.model.Delivery?>(null) }
            var loading by remember { mutableStateOf(true) }

            LaunchedEffect(deliveryId) {
                val result = deliveryRepository.getDeliveries()
                delivery = result.getOrNull()?.find { it.id == deliveryId }
                loading = false
            }

            if (loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@composable
            }

            val found = delivery
            if (found == null) {
                navController.popBackStack()
                return@composable
            }

            val detailViewModel: DeliveryDetailViewModel = viewModel(
                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                    @Suppress("UNCHECKED_CAST")
                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                        return DeliveryDetailViewModel(found, deliveryRepository) as T
                    }
                }
            )

            // Observer la validation pour passer le résultat à la liste
            val detailState by detailViewModel.uiState.collectAsState()
            LaunchedEffect(detailState.validationResult) {
                if (detailState.validationResult is ValidationResult.Success) {
                    // Passer l'ID validé à la route précédente (liste)
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set(KEY_VALIDATED_DELIVERY, deliveryId)
                }
            }

            DeliveryDetailScreen(
                viewModel = detailViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}

