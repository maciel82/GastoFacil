package com.example.gastofacil.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gastofacil.data.local.RecurringBillEntity
import com.example.gastofacil.ui.theme.*
import com.example.gastofacil.ui.viewmodel.MainViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringBillsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val bills by viewModel.allRecurringBills.collectAsState(initial = emptyList())
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }

    var showAddDialog by remember { mutableStateOf(false) }

    val totalBillsAmount = bills.sumOf { it.amount }
    val totalPaid = bills.filter { it.isPaidThisMonth }.sumOf { it.amount }
    val totalPending = totalBillsAmount - totalPaid

    Scaffold(
        containerColor = SageBackground,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = TopBarHeaderGreen,
                    titleContentColor = Color.White
                ),
                title = { Text("Contas Fixas & Vencimentos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = WarmGoldFab,
                contentColor = WarmGoldOnFab
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nova Conta Fixa")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Cartão de Resumo de Contas
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
                            Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = WarmGoldFab, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Total do Mês", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Text(currencyFormat.format(totalBillsAmount), color = WarmGoldFab, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Pago este Mês", color = Color.LightGray, fontSize = 11.sp)
                            Text(currencyFormat.format(totalPaid), color = IncomeGreenText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Pendente", color = Color.LightGray, fontSize = 11.sp)
                            Text(currencyFormat.format(totalPending), color = WarmGoldFab, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (bills.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nenhuma conta fixa cadastrada. Toque no '+' para adicionar!", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(bills, key = { it.id }) { bill ->
                        RecurringBillCard(
                            bill = bill,
                            currencyFormat = currencyFormat,
                            onTogglePaid = { viewModel.toggleBillPaidStatus(bill) },
                            onDeleteClick = { viewModel.deleteRecurringBill(bill) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        RecurringBillFormDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, amount, dueDay, category, bank ->
                viewModel.addRecurringBill(title, amount, dueDay, category, bank)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun RecurringBillCard(
    bill: RecurringBillEntity,
    currencyFormat: NumberFormat,
    onTogglePaid: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = if (bill.isPaidThisMonth) SageCardBackground else Color.White),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onTogglePaid, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (bill.isPaidThisMonth) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (bill.isPaidThisMonth) "Pago" else "Pendente",
                    tint = if (bill.isPaidThisMonth) IncomeGreenText else WarmGoldFab,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(bill.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = DarkForestGreen)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Vence dia ${bill.dueDay} | ${bill.category}" + if (!bill.bank.isNullOrBlank()) " • ${bill.bank}" else "",
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(currencyFormat.format(bill.amount), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = DarkForestGreen)
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (bill.isPaidThisMonth) IncomeGreenText.copy(alpha = 0.15f) else ExpenseRedText.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (bill.isPaidThisMonth) "PAGO" else "PENDENTE",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (bill.isPaidThisMonth) IncomeGreenText else ExpenseRedText,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = Color.LightGray, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun RecurringBillFormDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, dueDay: Int, category: String, bank: String?) -> Unit
) {
    var titleText by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var dueDayText by remember { mutableStateOf("10") }
    var categoryText by remember { mutableStateOf("Contas de casa") }
    var bankText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SageBackground,
        title = { Text("Nova Conta Fixa", fontWeight = FontWeight.Bold, color = DarkForestGreen) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("Descrição (ex: Aluguel, Luz)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Valor Estimado (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = dueDayText,
                    onValueChange = { dueDayText = it },
                    label = { Text("Dia do Vencimento (1 a 31)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = categoryText,
                    onValueChange = { categoryText = it },
                    label = { Text("Categoria") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = bankText,
                    onValueChange = { bankText = it },
                    label = { Text("Banco / Cartão (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    val dueDay = dueDayText.toIntOrNull() ?: 10
                    if (titleText.isNotBlank() && amount > 0) {
                        onSave(titleText.trim(), amount, dueDay, categoryText.trim(), bankText.trim().ifBlank { null })
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkForestGreen)
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
