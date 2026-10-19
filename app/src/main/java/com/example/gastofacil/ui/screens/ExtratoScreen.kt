package com.example.gastofacil.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gastofacil.ui.theme.*
import com.example.gastofacil.ui.util.CategoryIconBox
import com.example.gastofacil.ui.viewmodel.MainViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtratoScreen(
    viewModel: MainViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToLancamentos: () -> Unit,
    onNavigateToAjustes: () -> Unit
) {
    var selectedCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    val selectedYear = selectedCalendar[Calendar.YEAR]
    val selectedMonth = selectedCalendar[Calendar.MONTH]

    val transactionsFlow = remember(viewModel, selectedYear, selectedMonth) {
        viewModel.getTransactionsForMonth(selectedYear, selectedMonth)
    }
    val transactions by transactionsFlow.collectAsState(initial = emptyList())

    val totalIncomes = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalExpenses = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

    // Considera apenas saídas de caixa (PIX, DÉBITO, DINHEIRO) para calcular o saldo líquido da conta
    val totalCashExpenses = transactions.filter { it.type == "EXPENSE" && it.paymentMethod != "CREDIT" }.sumOf { it.amount }
    val balance = totalIncomes - totalCashExpenses

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale("pt", "BR")) }
    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale("pt", "BR")) }
    val monthTitleText = remember(selectedCalendar) {
        monthFormat.format(selectedCalendar.time).uppercase(Locale("pt", "BR"))
    }

    Scaffold(
        containerColor = SageBackground,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = TopBarHeaderGreen,
                    titleContentColor = Color.White
                ),
                title = { Text("Extrato do Saldo", fontWeight = FontWeight.Bold) }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SageCardBackground
            ) {
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
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
                    label = { Text("Ajustes") },
                    selected = false,
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = DarkForestGreen,
                        unselectedTextColor = DarkForestGreen
                    ),
                    onClick = onNavigateToAjustes
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SageCardBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = {
                            val newCal = (selectedCalendar.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                            selectedCalendar = newCal
                        }) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Mês Anterior", tint = DarkForestGreen)
                        }

                        Text(
                            text = monthTitleText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkForestGreen
                        )

                        IconButton(onClick = {
                            val newCal = (selectedCalendar.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                            selectedCalendar = newCal
                        }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Próximo Mês", tint = DarkForestGreen)
                        }
                    }

                    HorizontalDivider(color = DarkForestGreen.copy(alpha = 0.2f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Entradas Totais:")
                        Text(currencyFormat.format(totalIncomes), color = IncomeGreenText, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Saídas Totais:")
                        Text(currencyFormat.format(totalExpenses), color = ExpenseRedText, fontWeight = FontWeight.Bold)
                    }
                    HorizontalDivider(color = DarkForestGreen.copy(alpha = 0.2f))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Saldo Líquido:", fontWeight = FontWeight.Bold)
                        Text(
                            currencyFormat.format(balance),
                            fontWeight = FontWeight.Bold,
                            color = if (balance >= 0) IncomeGreenText else ExpenseRedText
                        )
                    }
                }
            }

            Text(text = "DETALHAMENTO CRONOLÓGICO", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(transactions, key = { it.id }) { transaction ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CategoryIconBox(
                                category = transaction.category,
                                type = transaction.type
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                val displayLocation = transaction.storeLocation?.ifBlank { null } ?: transaction.title
                                Text(
                                    text = displayLocation,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )

                                val dateStr = dateFormat.format(Date(transaction.date))
                                val infoLabel = if (transaction.type == "INCOME") "Entrada" else (transaction.bank ?: transaction.paymentMethod)

                                Text(
                                    text = "$dateStr | $infoLabel",
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = (if (transaction.type == "INCOME") "+ " else "- ") + currencyFormat.format(transaction.amount),
                                fontWeight = FontWeight.Bold,
                                color = if (transaction.type == "INCOME") IncomeGreenText else ExpenseRedText
                            )
                        }
                    }
                }
            }
        }
    }
}
