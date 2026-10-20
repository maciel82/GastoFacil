package com.example.gastofacil

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.gastofacil.ui.components.AddTransactionDialog
import com.example.gastofacil.ui.screens.AjustesScreen
import com.example.gastofacil.ui.screens.ExtratoScreen
import com.example.gastofacil.ui.screens.HomeScreen
import com.example.gastofacil.ui.screens.MonthDetailsScreen
import com.example.gastofacil.ui.theme.GastoFacilTheme
import com.example.gastofacil.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
        setContent {
            GastoFacilTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    var showAddDialog by remember { mutableStateOf(false) }

                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            HomeScreen(
                                viewModel = viewModel,
                                onNavigateToExtrato = { navController.navigate("extrato") },
                                onNavigateToLancamentos = { navController.navigate("details") },
                                onNavigateToDetails = { navController.navigate("details") },
                                onOpenAddDialog = { showAddDialog = true },
                                onNavigateToAjustes = { navController.navigate("ajustes") }
                            )
                        }
                        composable("extrato") {
                            ExtratoScreen(
                                viewModel = viewModel,
                                onNavigateToHome = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
                                onNavigateToLancamentos = { navController.navigate("details") },
                                onNavigateToAjustes = { navController.navigate("ajustes") }
                            )
                        }
                        composable("details") {
                            MonthDetailsScreen(
                                viewModel = viewModel,
                                onNavigateToHome = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
                                onNavigateToExtrato = { navController.navigate("extrato") },
                                onNavigateToAjustes = { navController.navigate("ajustes") }
                            )
                        }
                        composable("ajustes") {
                            AjustesScreen(
                                viewModel = viewModel,
                                onNavigateToHome = { navController.navigate("home") { popUpTo("home") { inclusive = true } } },
                                onNavigateToLancamentos = { navController.navigate("details") },
                                onNavigateToExtrato = { navController.navigate("extrato") }
                            )
                        }
                    }

                    if (showAddDialog) {
                        AddTransactionDialog(
                            viewModel = viewModel,
                            onDismiss = { showAddDialog = false }
                        )
                    }
                }
            }
        }
    }
}