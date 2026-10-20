package com.example.gastofacil.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gastofacil.data.local.TransactionEntity
import com.example.gastofacil.ui.components.AddTransactionDialog
import com.example.gastofacil.ui.components.NotificationsDialog
import com.example.gastofacil.ui.components.TransactionDetailsDialog
import com.example.gastofacil.ui.theme.*
import com.example.gastofacil.ui.util.CategoryIconBox
import com.example.gastofacil.ui.viewmodel.MainViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onNavigateToExtrato: () -> Unit,
    onNavigateToLancamentos: () -> Unit,
    onNavigateToDetails: () -> Unit,
    onOpenAddDialog: () -> Unit,
    onNavigateToMenu: () -> Unit
) {
    var selectedCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    val selectedYear = selectedCalendar[Calendar.YEAR]
    val selectedMonth = selectedCalendar[Calendar.MONTH]

    var showAddDialog by remember { mutableStateOf(false) }

    val transactionsFlow = remember(viewModel, selectedYear, selectedMonth) {
        viewModel.getTransactionsForMonth(selectedYear, selectedMonth)
    }
    val transactions by transactionsFlow.collectAsState(initial = emptyList())

    val totalAllTimeIncomes by viewModel.totalAllTimeIncomes.collectAsState(initial = 0.0)

    val projectionsFlow = remember(viewModel) { viewModel.getUpcomingProjections() }
    val projections by projectionsFlow.collectAsState(initial = emptyList())

    val totalExpenses = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    val totalIncomes = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }

    // Notificação de limite de gastos mensais
    LaunchedEffect(totalExpenses, selectedYear, selectedMonth) {
        viewModel.checkAllBudgetsAcrossMonths()
    }

    // Gastos com pagamento imediato (PIX, DÉBITO, DINHEIRO) que descontam do saldo do mês
    val totalCashExpenses = transactions.filter { it.type == "EXPENSE" && it.paymentMethod != "CREDIT" }.sumOf { it.amount }

    val allTransactionsFlow = remember(viewModel) { viewModel.allTransactions }
    val allTransactions by allTransactionsFlow.collectAsState(initial = emptyList())

    val lastExpenses = remember(allTransactions) {
        allTransactions.filter { it.type == "EXPENSE" }.take(3)
    }

    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    var transactionToConfirmEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    var showEditConfirmDialog by remember { mutableStateOf(false) }

    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var showDeleteGroupDialog by remember { mutableStateOf(false) }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
    val monthYearFormat = remember { SimpleDateFormat("MMMM yyyy", Locale("pt", "BR")) }
    val monthYearText = remember(selectedCalendar) {
        monthYearFormat.format(selectedCalendar.time).uppercase(Locale("pt", "BR"))
    }

    val totalAllTimeBalance by viewModel.totalAllTimeBalance.collectAsState(initial = 0.0)

    // Saldo do Mês (Sobra da receita daquele mês descontando apenas PIX, DÉBITO e DINHEIRO)
    val monthBalance = totalIncomes - totalCashExpenses

    var transactionToViewDetails by remember { mutableStateOf<TransactionEntity?>(null) }

    val unreadNotificationsCount by viewModel.unreadNotificationsCount.collectAsState(initial = 0)
    var showNotificationsDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = SageBackground,
        topBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TopBarHeaderGreen)
                    .statusBarsPadding()
                    .height(64.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = 0.dp,
                        bottomEnd = 0.dp
                    ),
                    color = SageBackground,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = SpanStyle(color = BrandGastoGreen, fontWeight = FontWeight.Bold)) {
                                    append("Gasto")
                                }
                                withStyle(style = SpanStyle(color = BrandFacilGold, fontWeight = FontWeight.Bold)) {
                                    append("Fácil")
                                }
                            },
                            fontSize = 21.sp
                        )

                        IconButton(
                            onClick = { showNotificationsDialog = true },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadNotificationsCount > 0) {
                                        Badge(
                                            containerColor = ExpenseRedText,
                                            contentColor = Color.White
                                        ) {
                                            Text(if (unreadNotificationsCount > 99) "99+" else unreadNotificationsCount.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notificações",
                                    tint = DarkForestGreen
                                )
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = SageCardBackground
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Início") },
                    label = { Text("Início") },
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
                    selected = false,
                    colors = NavigationBarItemDefaults.colors(
                        unselectedIconColor = DarkForestGreen,
                        unselectedTextColor = DarkForestGreen
                    ),
                    onClick = onNavigateToMenu
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = WarmGoldFab,
                contentColor = WarmGoldOnFab
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Lançamento")
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SageCardBackground),
                    modifier = Modifier.size(width = 190.dp, height = 180.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.Start
                    ) {
                        Column {
                            Text(
                                text = "RECEITA TOTAL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreenText
                            )
                            Text(
                                text = currencyFormat.format(totalAllTimeIncomes),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray
                            )
                        }

                        HorizontalDivider(color = DarkForestGreen.copy(alpha = 0.15f))

                        Column {
                            Text(
                                text = "SALDO TOTAL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = IncomeGreenText
                            )
                            Text(
                                text = currencyFormat.format(totalAllTimeBalance),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (totalAllTimeBalance >= 0) Color.DarkGray else ExpenseRedText
                            )
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = SageCardBackground),
                    modifier = Modifier.size(width = 270.dp, height = 180.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val newCal = (selectedCalendar.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                                    selectedCalendar = newCal
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Mês Anterior", tint = DarkForestGreen)
                            }

                            Text(
                                text = monthYearText,
                                fontSize = 12.sp,
                                color = IncomeGreenText,
                                fontWeight = FontWeight.Bold
                            )

                            IconButton(
                                onClick = {
                                    val newCal = (selectedCalendar.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                                    selectedCalendar = newCal
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Próximo Mês", tint = DarkForestGreen)
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToDetails() },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = currencyFormat.format(totalExpenses),
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRedText
                            )
                            Text(text = "Gastos Acumulados no Mês", fontSize = 11.sp, color = Color.Gray)

                            Spacer(modifier = Modifier.height(6.dp))
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp), color = DarkForestGreen.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = currencyFormat.format(totalIncomes),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.DarkGray
                                    )
                                    Text(text = "Receita do Mês", fontSize = 10.sp, color = Color.Gray)
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = currencyFormat.format(monthBalance),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (monthBalance >= 0) Color.DarkGray else ExpenseRedText
                                    )
                                    Text(text = "Saldo do Mês", fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }

            Text(text = "PROJEÇÃO DE PARCELAS", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (projections.isEmpty()) {
                    ProjectionMonthCard(
                        monthName = "SEM PROJEÇÃO",
                        amount = "R$ 0.00",
                        color = SageCardBackground
                    )
                } else {
                    projections.forEach { (monthName, amount) ->
                        ProjectionMonthCard(
                            monthName = monthName,
                            amount = currencyFormat.format(amount),
                            color = SageCardBackground
                        )
                    }
                }
            }

            if (lastExpenses.isNotEmpty()) {
                Text(text = "ÚLTIMOS GASTOS", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(lastExpenses, key = { it.id }) { transaction ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                when (dismissValue) {
                                    SwipeToDismissBoxValue.EndToStart -> {
                                        transactionToDelete = transaction
                                        showDeleteGroupDialog = true
                                        false
                                    }
                                    SwipeToDismissBoxValue.StartToEnd -> {
                                        transactionToConfirmEdit = transaction
                                        showEditConfirmDialog = true
                                        false
                                    }
                                    else -> false
                                }
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                val color = when (dismissState.dismissDirection) {
                                    SwipeToDismissBoxValue.EndToStart -> ExpenseRedText
                                    SwipeToDismissBoxValue.StartToEnd -> BrandGastoGreen
                                    else -> Color.Transparent
                                }

                                val alignment = when (dismissState.dismissDirection) {
                                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                                    SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                    else -> Alignment.Center
                                }

                                val icon = when (dismissState.dismissDirection) {
                                    SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
                                    SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Edit
                                    else -> null
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color, shape = RoundedCornerShape(8.dp))
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = alignment
                                ) {
                                    if (icon != null) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                    }
                                }
                            },
                            content = {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            transactionToViewDetails = transaction
                                        },
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

                                            val fullDateFormat = SimpleDateFormat("dd MMM", Locale("pt", "BR"))
                                            val dateStr = fullDateFormat.format(Date(transaction.date))
                                            val paymentLabel = when (transaction.paymentMethod) {
                                                "CREDIT" -> "Crédito"
                                                "DEBIT" -> "Débito"
                                                "PIX" -> "PIX"
                                                "MONEY" -> "Dinheiro"
                                                else -> transaction.paymentMethod
                                            }

                                            Text(
                                                text = "$dateStr | $paymentLabel",
                                                fontSize = 12.sp,
                                                color = Color.Gray,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(8.dp))

                                        Column(
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            val displayFullAmount = if (transaction.isInstallment && transaction.totalInstallments > 1) {
                                                transaction.amount * transaction.totalInstallments
                                            } else {
                                                transaction.amount
                                            }

                                            Text(
                                                text = currencyFormat.format(displayFullAmount),
                                                fontWeight = FontWeight.Bold,
                                                color = ExpenseRedText
                                            )
                                            if (transaction.isInstallment && transaction.totalInstallments > 1) {
                                                Text(
                                                    text = "em ${transaction.totalInstallments}x",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }

            if (showEditConfirmDialog && transactionToConfirmEdit != null) {
                AlertDialog(
                    onDismissRequest = {
                        showEditConfirmDialog = false
                        transactionToConfirmEdit = null
                    },
                    title = { Text("Confirmar Edição") },
                    text = { Text("Deseja realmente editar o lançamento \"${transactionToConfirmEdit?.title}\"?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                transactionToEdit = transactionToConfirmEdit
                                showEditConfirmDialog = false
                                transactionToConfirmEdit = null
                            }
                        ) {
                            Text("Sim, Editar")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showEditConfirmDialog = false
                            transactionToConfirmEdit = null
                        }) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            if (showDeleteGroupDialog && transactionToDelete != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteGroupDialog = false },
                    title = { Text("Confirmar Exclusão") },
                    text = { Text("Deseja realmente excluir este lançamento e todas as suas parcelas associadas?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.deleteTransactionGroup(transactionToDelete!!)
                                showDeleteGroupDialog = false
                                transactionToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Excluir")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteGroupDialog = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            if (transactionToEdit != null) {
                AddTransactionDialog(
                    viewModel = viewModel,
                    transactionToEdit = transactionToEdit,
                    onDismiss = { transactionToEdit = null }
                )
            }

            if (showAddDialog) {
                AddTransactionDialog(
                    viewModel = viewModel,
                    onDismiss = { showAddDialog = false }
                )
            }
            if (transactionToViewDetails != null) {
                TransactionDetailsDialog(
                    transaction = transactionToViewDetails!!,
                    onDismiss = { transactionToViewDetails = null }
                )
            }

            if (showNotificationsDialog) {
                NotificationsDialog(
                    viewModel = viewModel,
                    onDismiss = { showNotificationsDialog = false }
                )
            }
        }
    }
}

@Composable
fun ProjectionMonthCard(monthName: String, amount: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color,
        modifier = Modifier.size(width = 130.dp, height = 75.dp)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = monthName, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IncomeGreenText)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = amount, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.DarkGray)
        }
    }
}
