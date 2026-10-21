package com.example.gastofacil.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gastofacil.data.local.GoalEntity
import com.example.gastofacil.ui.theme.*
import com.example.gastofacil.ui.viewmodel.MainViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val goals by viewModel.allGoals.collectAsState(initial = emptyList())
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }

    var showAddDialog by remember { mutableStateOf(false) }
    var goalToEdit by remember { mutableStateOf<GoalEntity?>(null) }
    var goalToDeposit by remember { mutableStateOf<GoalEntity?>(null) }
    var goalToWithdraw by remember { mutableStateOf<GoalEntity?>(null) }

    val totalSaved = goals.sumOf { it.currentAmount }
    val totalTarget = goals.sumOf { it.targetAmount }
    val overallRatio = if (totalTarget > 0) (totalSaved / totalTarget).coerceIn(0.0, 1.0).toFloat() else 0f

    Scaffold(
        containerColor = SageBackground,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = TopBarHeaderGreen,
                    titleContentColor = Color.White
                ),
                title = { Text("Metas & Caixinhas", fontWeight = FontWeight.Bold) },
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
                Icon(Icons.Default.Add, contentDescription = "Nova Caixinha")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Cartão de Resumo de Economias
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
                            Icon(Icons.Default.Savings, contentDescription = null, tint = WarmGoldFab, modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Total Guardado", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Text("${(overallRatio * 100).toInt()}% da Meta", color = WarmGoldFab, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(currencyFormat.format(totalSaved), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Meta total: ${currencyFormat.format(totalTarget)}", color = Color.LightGray, fontSize = 12.sp)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Barra de Progresso Geral
                    LinearProgressIndicator(
                        progress = { overallRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = WarmGoldFab,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (goals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Nenhuma caixinha criada. Toque no '+' para criar a primeira!", color = Color.Gray, fontSize = 14.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(goals, key = { it.id }) { goal ->
                        GoalCard(
                            goal = goal,
                            currencyFormat = currencyFormat,
                            onDepositClick = { goalToDeposit = goal },
                            onWithdrawClick = { goalToWithdraw = goal },
                            onEditClick = { goalToEdit = goal },
                            onDeleteClick = { viewModel.deleteGoal(goal) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        GoalFormDialog(
            onDismiss = { showAddDialog = false },
            onSave = { name, target ->
                viewModel.addGoal(name, target)
                showAddDialog = false
            }
        )
    }

    if (goalToEdit != null) {
        GoalFormDialog(
            goalToEdit = goalToEdit,
            onDismiss = { goalToEdit = null },
            onSave = { name, target ->
                viewModel.updateGoal(goalToEdit!!.copy(name = name, targetAmount = target))
                goalToEdit = null
            }
        )
    }

    if (goalToDeposit != null) {
        AmountInputDialog(
            title = "Guardar Dinheiro em ${goalToDeposit!!.name}",
            actionLabel = "Guardar",
            onDismiss = { goalToDeposit = null },
            onConfirm = { amount ->
                viewModel.depositToGoal(goalToDeposit!!, amount)
                goalToDeposit = null
            }
        )
    }

    if (goalToWithdraw != null) {
        AmountInputDialog(
            title = "Resgatar Dinheiro de ${goalToWithdraw!!.name}",
            actionLabel = "Resgatar",
            onDismiss = { goalToWithdraw = null },
            onConfirm = { amount ->
                viewModel.withdrawFromGoal(goalToWithdraw!!, amount)
                goalToWithdraw = null
            }
        )
    }
}

@Composable
private fun GoalCard(
    goal: GoalEntity,
    currencyFormat: NumberFormat,
    onDepositClick: () -> Unit,
    onWithdrawClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val ratio = if (goal.targetAmount > 0) (goal.currentAmount / goal.targetAmount).coerceIn(0.0, 1.0).toFloat() else 0f

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
                Text(goal.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = DarkForestGreen)

                Row {
                    IconButton(onClick = onEditClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color.Gray, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDeleteClick, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Excluir", tint = ExpenseRedText, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Guardado: ${currencyFormat.format(goal.currentAmount)}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = IncomeGreenText)
                Text("Meta: ${currencyFormat.format(goal.targetAmount)}", fontSize = 13.sp, color = Color.Gray)
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

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onWithdrawClick) {
                    Text("- Resgatar", color = ExpenseRedText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onDepositClick,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkForestGreen)
                ) {
                    Text("+ Guardar", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun GoalFormDialog(
    goalToEdit: GoalEntity? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, targetAmount: Double) -> Unit
) {
    var nameText by remember { mutableStateOf(goalToEdit?.name ?: "") }
    var targetText by remember { mutableStateOf(goalToEdit?.targetAmount?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SageBackground,
        title = { Text(if (goalToEdit != null) "Editar Caixinha" else "Nova Caixinha / Meta", fontWeight = FontWeight.Bold, color = DarkForestGreen) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Nome da Meta (ex: Reserva de Emergência)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it },
                    label = { Text("Valor Objetivo (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val target = targetText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (nameText.isNotBlank() && target > 0) {
                        onSave(nameText.trim(), target)
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

@Composable
private fun AmountInputDialog(
    title: String,
    actionLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SageBackground,
        title = { Text(title, fontWeight = FontWeight.Bold, color = DarkForestGreen, fontSize = 16.sp) },
        text = {
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                label = { Text("Valor (R$)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val amt = amountText.replace(",", ".").toDoubleOrNull() ?: 0.0
                    if (amt > 0) {
                        onConfirm(amt)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkForestGreen)
            ) {
                Text(actionLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
