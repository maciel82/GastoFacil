package com.example.gastofacil.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gastofacil.data.local.TransactionEntity
import com.example.gastofacil.ui.components.AddTransactionDialog
import com.example.gastofacil.ui.components.TransactionDetailsDialog
import com.example.gastofacil.ui.theme.*
import com.example.gastofacil.ui.util.CategoryIconBox
import com.example.gastofacil.ui.viewmodel.MainViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun MonthDetailsScreen(
    viewModel: MainViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToExtrato: () -> Unit,
    onNavigateToAjustes: () -> Unit
) {
    var selectedCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    val selectedYear = selectedCalendar[Calendar.YEAR]
    val selectedMonth = selectedCalendar[Calendar.MONTH]

    val transactionsFlow = remember(viewModel, selectedYear, selectedMonth) {
        viewModel.getTransactionsForMonth(selectedYear, selectedMonth)
    }
    val transactions by transactionsFlow.collectAsState(initial = emptyList())

    var selectedTab by remember { mutableStateOf(0) }

    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var transactionToConfirmEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    var showEditConfirmDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }

    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var showSingleDeleteGroupDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Estado para exibir os detalhes do lançamento ao clicar no card
    var transactionToViewDetails by remember { mutableStateOf<TransactionEntity?>(null) }

    val isSelecting = selectedIds.isNotEmpty()

    val filteredTransactions = when (selectedTab) {
        0 -> transactions.filter { it.paymentMethod == "CREDIT" }
        1 -> transactions.filter { it.paymentMethod == "DEBIT_PIX" || it.paymentMethod == "PIX" || it.paymentMethod == "DEBIT" }
        else -> transactions.filter { it.paymentMethod == "MONEY" }
    }

    val totalExpensesInTab = filteredTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }

    val totalIncomesInMonth = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    val totalCashExpensesInMonth = transactions.filter { it.type == "EXPENSE" && it.paymentMethod != "CREDIT" }.sumOf { it.amount }
    val monthBalance = totalIncomesInMonth - totalCashExpensesInMonth

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
                title = {
                    Text(
                        text = if (isSelecting) "${selectedIds.size} selecionados" else "Lançamentos",
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    if (isSelecting) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir Selecionados", tint = Color.White)
                        }
                    }
                }
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
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Lançamentos") },
                    label = { Text("Lançamentos") },
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
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkForestGreen),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
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
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Mês Anterior", tint = Color.White)
                        }

                        Text(
                            text = monthTitleText,
                            fontSize = 18.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )

                        IconButton(onClick = {
                            val newCal = (selectedCalendar.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                            selectedCalendar = newCal
                        }) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Próximo Mês", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = currencyFormat.format(totalExpensesInTab),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF9A9A)
                    )
                    Text(
                        text = "Gastos Acumulados",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.3f), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = currencyFormat.format(totalIncomesInMonth),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF81C784)
                            )
                            Text(
                                text = "Receita do Mês",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = currencyFormat.format(monthBalance),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (monthBalance >= 0) Color.White else Color(0xFFEF9A9A)
                            )
                            Text(
                                text = "Saldo do Mês",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = DarkForestGreen,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            height = 3.dp,
                            color = DarkForestGreen
                        )
                    }
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("CARTÃO DE CRÉDITO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkForestGreen) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("PIX/DÉBITO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkForestGreen) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("DINHEIRO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkForestGreen) }
                )
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(filteredTransactions, key = { it.id }) { transaction ->
                    val isSelected = selectedIds.contains(transaction.id)

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            when (dismissValue) {
                                SwipeToDismissBoxValue.EndToStart -> {
                                    transactionToDelete = transaction
                                    showSingleDeleteGroupDialog = true
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
                                    .background(color, shape = RoundedCornerShape(12.dp))
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
                                    .combinedClickable(
                                        onClick = {
                                            if (isSelecting) {
                                                selectedIds = if (isSelected) selectedIds - transaction.id else selectedIds + transaction.id
                                            } else {
                                                // Exibe os detalhes da transação ao clicar
                                                transactionToViewDetails = transaction
                                            }
                                        },
                                        onLongClick = {
                                            selectedIds = if (isSelected) selectedIds - transaction.id else selectedIds + transaction.id
                                        }
                                    ),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) SageCardBackground else Color.White
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (isSelecting) {
                                            Checkbox(
                                                checked = isSelected,
                                                onCheckedChange = { checked ->
                                                    selectedIds = if (checked) selectedIds + transaction.id else selectedIds - transaction.id
                                                }
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                        }

                                        CategoryIconBox(
                                            category = transaction.category,
                                            type = transaction.type
                                        )

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = transaction.title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp
                                            )

                                            val dateStr = dateFormat.format(Date(transaction.date))
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
                                                color = Color.Gray
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = (if (transaction.type == "INCOME") "+ " else "- ") + currencyFormat.format(transaction.amount),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = if (transaction.type == "INCOME") IncomeGreenText else ExpenseRedText
                                        )

                                        if (transaction.isInstallment) {
                                            Text(
                                                text = "Parcela ${transaction.currentInstallment}/${transaction.totalInstallments}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
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

        // Diálogo de confirmação para editar (ativado pelo Swipe para a direita)
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

        // Diálogo de visualização de detalhes ao clicar no card
        if (transactionToViewDetails != null) {
            TransactionDetailsDialog(
                transaction = transactionToViewDetails!!,
                onDismiss = { transactionToViewDetails = null }
            )
        }

        if (showSingleDeleteGroupDialog && transactionToDelete != null) {
            AlertDialog(
                onDismissRequest = { showSingleDeleteGroupDialog = false },
                title = { Text("Confirmar Exclusão") },
                text = { Text("Deseja realmente excluir este lançamento e todas as suas parcelas associadas?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteTransactionGroup(transactionToDelete!!)
                            showSingleDeleteGroupDialog = false
                            transactionToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Excluir Tudo")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSingleDeleteGroupDialog = false }) {
                        Text("Cancelar")
                    }
                }
            )
        }

        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Confirmar Exclusão em Lote") },
                text = { Text("Deseja excluir as ${selectedIds.size} transações selecionadas?") },
                confirmButton = {
                    Button(
                        onClick = {
                            val selectedList = filteredTransactions.filter { it.id in selectedIds }
                            viewModel.deleteTransactions(selectedList)
                            selectedIds = emptySet()
                            showDeleteDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Excluir Selecionados")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
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
    }
}