package com.neoship.courier

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.neoship.courier.navigation.AppNavHost
import com.neoship.courier.ui.theme.NeoShipTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as NeoShipApp

        setContent {
            NeoShipTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()

                    AppNavHost(
                        navController = navController,
                        tokenManager = app.tokenManager,
                        completedDeliveriesStorage = app.completedDeliveriesStorage
                    )
                }
            }
        }
    }
}