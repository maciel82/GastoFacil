package com.example.gastofacil.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gastofacil.ui.components.NotificationsDialog
import com.example.gastofacil.ui.theme.*
import com.example.gastofacil.ui.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuScreen(
    viewModel: MainViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToLancamentos: () -> Unit,
    onNavigateToExtrato: () -> Unit,
    onNavigateToAjustes: () -> Unit,
    onNavigateToAnalytics: () -> Unit
) {
    val unreadNotificationsCount by viewModel.unreadNotificationsCount.collectAsState(initial = 0)
    var showNotificationsDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = SageBackground,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.White
                ),
                title = { Text("Menu Principal", fontWeight = FontWeight.Bold) }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = SageCardBackground) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Início") },
                    label = { Text("Início") },
                    selected = false,
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = DarkForestGreen,
                        unselectedTextColor = DarkForestGreen
                    ),
                    onClick = onNavigateToHome
                )
                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Histórico") },
                    label = { Text("Histórico") },
                    selected = false,
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = DarkForestGreen,
                        unselectedTextColor = DarkForestGreen
                    ),
                    onClick = onNavigateToLancamentos
                )
                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "Extrato") },
                    label = { Text("Extrato") },
                    selected = false,
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = DarkForestGreen,
                        unselectedTextColor = DarkForestGreen
                    ),
                    onClick = onNavigateToExtrato
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Menu, contentDescription = "Menu") },
                    label = { Text("Menu") },
                    selected = true,
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = WarmGoldFab,
                        selectedIconColor = DarkForestGreen,
                        selectedTextColor = DarkForestGreen,
                        unselectedIconColor = DarkForestGreen,
                        unselectedTextColor = DarkForestGreen
                    ),
                    onClick = {}
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Seção: Avisos e Central de Alertas
            Text(
                text = "AVISOS & ALERTAS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                MenuActionItem(
                    icon = Icons.Default.Notifications,
                    title = "Central de Alertas",
                    subtitle = if (unreadNotificationsCount > 0) "Histórico de avisos ($unreadNotificationsCount novos)" else "Histórico de limites e avisos",
                    badgeCount = unreadNotificationsCount,
                    onClick = { showNotificationsDialog = true }
                )
            }

            // Seção: Análises e Relatórios
            Text(
                text = "ANÁLISES E RELATÓRIOS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                MenuActionItem(
                    icon = Icons.Default.PieChart,
                    title = "Analytics & Gráficos",
                    subtitle = "Gastos por categoria e banco",
                    onClick = onNavigateToAnalytics
                )
            }

            // Seção: Configurações
            Text(
                text = "CONFIGURAÇÕES E DADOS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    MenuActionItem(
                        icon = Icons.Default.Settings,
                        title = "Ajustes & Preferências",
                        subtitle = "Modo escuro, categorias, limite de gastos e backup",
                        onClick = onNavigateToAjustes
                    )
                }
            }

            // Seção: Sobre
            Text(
                text = "SOBRE O APLICATIVO",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "GastoFácil v1.0.0",
                        fontWeight = FontWeight.Bold,
                        color = DarkForestGreen
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Gestão financeira pessoal com interface Verde Jade",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        if (showNotificationsDialog) {
            NotificationsDialog(
                viewModel = viewModel,
                onDismiss = { showNotificationsDialog = false }
            )
        }
    }
}

@Composable
fun MenuActionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    badgeCount: Int = 0,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = DarkForestGreen)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    if (badgeCount > 0) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Badge(containerColor = ExpenseRedText) {
                            Text(text = badgeCount.toString(), color = Color.White, fontSize = 10.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, fontSize = 12.sp, color = Color.Gray)
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
        }
    }
}
