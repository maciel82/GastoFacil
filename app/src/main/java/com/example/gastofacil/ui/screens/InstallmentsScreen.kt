package com.example.gastofacil.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gastofacil.ui.theme.*
import com.example.gastofacil.ui.viewmodel.InstallmentGroupSummary
import com.example.gastofacil.ui.viewmodel.MainViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallmentsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val installmentsList by viewModel.allInstallmentSummaries.collectAsState(initial = emptyList())
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }

    val totalRemainingDebt = installmentsList.sumOf { it.remainingAmount }
    val totalActivePurchases = installmentsList.size

    var installmentToDelete by remember { mutableStateOf<InstallmentGroupSummary?>(null) }

    Scaffold(
        containerColor = SageBackground,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = TopBarHeaderGreen,
                    titleContentColor = Color.White
                ),
                title = { Text("Painel de Compras Parceladas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Cartão de Resumo de Dívida Futura
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkForestGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CreditCard, contentDescription = null, tint = WarmGoldFab, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Total Pendente Futuro", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Text("$totalActivePurchases Compras", color = WarmGoldFab, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(currencyFormat.format(totalRemainingDebt), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Soma de todas as parcelas a vencer nos próximos meses", color = Color.LightGray, fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (installmentsList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nenhuma compra parcelada cadastrada.", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(installmentsList, key = { it.groupId }) { item ->
                        InstallmentCard(
                            summary = item,
                            currencyFormat = currencyFormat,
                            onDeleteClick = { installmentToDelete = item }
                        )
                    }
                }
            }
        }
    }

    if (installmentToDelete != null) {
        AlertDialog(
            onDismissRequest = { installmentToDelete = null },
            containerColor = SageBackground,
            title = { Text("Excluir Parcelamento?", fontWeight = FontWeight.Bold, color = DarkForestGreen) },
            text = {
                Text(
                    text = "Deseja excluir todas as parcelas pertencentes à compra '${installmentToDelete!!.title}'?",
                    fontSize = 14.sp,
                    color = Color.DarkGray
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val firstTx = installmentToDelete!!.installmentsList.firstOrNull()
                        if (firstTx != null) {
                            viewModel.deleteTransactionGroup(firstTx)
                        }
                        installmentToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Excluir Todas")
                }
            },
            dismissButton = {
                TextButton(onClick = { installmentToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun InstallmentCard(
    summary: InstallmentGroupSummary,
    currencyFormat: NumberFormat,
    onDeleteClick: () -> Unit
) {
    val ratio = if (summary.totalInstallments > 0) (summary.paidInstallmentsCount.toFloat() / summary.totalInstallments) else 0f

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(summary.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkForestGreen)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${summary.category}" + if (!summary.bank.isNullOrBlank()) " • ${summary.bank}" else "",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = ExpenseRedText, modifier = Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${summary.paidInstallmentsCount} de ${summary.totalInstallments} parcelas (${currencyFormat.format(summary.installmentAmount)}/mês)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkForestGreen
                )
                Text(
                    text = "${(ratio * 100).toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = IncomeGreenText
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            LinearProgressIndicator(
                progress = { ratio },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = BrandGastoGreen,
                trackColor = SageBackground
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Pago: ${currencyFormat.format(summary.paidAmount)}", fontSize = 12.sp, color = Color.Gray)
                Text("Falta: ${currencyFormat.format(summary.remainingAmount)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = ExpenseRedText)
            }
        }
    }
}
