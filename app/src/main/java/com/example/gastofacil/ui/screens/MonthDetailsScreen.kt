package com.example.gastofacil.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
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
    var transactionToViewDetails by remember { mutableStateOf<TransactionEntity?>(null) }

    // Estados para Filtros Avançados (Múltipla Seleção)
    var searchQuery by remember { mutableStateOf("") }
    var selectedBanks by remember { mutableStateOf(setOf<String>()) }       // Vazio = Todos
    var selectedCategories by remember { mutableStateOf(setOf<String>()) } // Vazio = Todas
    var showFilterDialog by remember { mutableStateOf(false) }

    val isSelecting = selectedIds.isNotEmpty()

    // Categorias disponíveis limpas de duplicatas
    val categoriesList by viewModel.categories.collectAsState(initial = emptyList())
    val uniqueCategories = remember(categoriesList) { categoriesList.distinctBy { it.name } }
    val bankOptions = remember { listOf("Nubank", "PicPay", "RecargaPay", "Outro") }

    // Filtragem combinada atualizada para Múltipla Seleção
    val filteredTransactions = transactions.filter { tx ->
        val matchesTab = when (selectedTab) {
            0 -> tx.paymentMethod == "CREDIT"
            1 -> tx.paymentMethod == "DEBIT" || tx.paymentMethod == "PIX" || tx.paymentMethod == "DEBIT_PIX"
            else -> tx.paymentMethod == "MONEY"
        }

        val matchesSearch = searchQuery.isBlank() ||
                tx.title.contains(searchQuery, ignoreCase = true) ||
                (tx.storeLocation?.contains(searchQuery, ignoreCase = true) == true)

        val matchesBank = selectedBanks.isEmpty() || (tx.bank != null && selectedBanks.contains(tx.bank))
        val matchesCategory = selectedCategories.isEmpty() || selectedCategories.contains(tx.category)

        matchesTab && matchesSearch && matchesBank && matchesCategory
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

    val hasActiveFilters = searchQuery.isNotBlank() || selectedBanks.isNotEmpty() || selectedCategories.isNotEmpty()

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
                    } else {
                        IconButton(onClick = { showFilterDialog = true }) {
                            BadgedBox(
                                badge = {
                                    if (hasActiveFilters) {
                                        Badge(containerColor = WarmGoldFab)
                                    }
                                }
                            ) {
                                Icon(Icons.Default.FilterList, contentDescription = "Filtros Avançados", tint = Color.White)
                            }
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = SageCardBackground) {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Início") },
                    label = { Text("Início") },
                    selected = false,
                    colors = NavigationBarItemDefaults.colors(unselectedIconColor = DarkForestGreen, unselectedTextColor = DarkForestGreen),
                    onClick = onNavigateToHome
                )
                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Lançamentos") },
                    label = { Text("Lançamentos") },
                    selected = true,
                    colors = NavigationBarItemDefaults.colors(indicatorColor = WarmGoldFab, selectedIconColor = DarkForestGreen, selectedTextColor = DarkForestGreen, unselectedIconColor = DarkForestGreen, unselectedTextColor = DarkForestGreen),
                    onClick = {}
                )
                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "Extrato") },
                    label = { Text("Extrato") },
                    selected = false,
                    colors = NavigationBarItemDefaults.colors(unselectedIconColor = DarkForestGreen, unselectedTextColor = DarkForestGreen),
                    onClick = onNavigateToExtrato
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Ajustes") },
                    label = { Text("Ajustes") },
                    selected = false,
                    colors = NavigationBarItemDefaults.colors(unselectedIconColor = DarkForestGreen, unselectedTextColor = DarkForestGreen),
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Pesquisar Lançamento") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = DarkForestGreen) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpar busca", tint = Color.Gray)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedBorderColor = DarkForestGreen,
                    unfocusedBorderColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )

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

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = currencyFormat.format(totalExpensesInTab),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF9A9A)
                    )
                    Text(
                        text = if (hasActiveFilters) "Gastos Filtrados" else "Gastos Acumulados",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
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
                    text = { Text("CRÉDITO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkForestGreen) }
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
                                    Icon(imageVector = icon, contentDescription = null, tint = Color.White)
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

                                        Column(modifier = Modifier.weight(1f)) {
                                            val displayLocation = transaction.storeLocation?.ifBlank { null } ?: transaction.title
                                            Text(
                                                text = displayLocation,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 16.sp,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )

                                            val dateStr = dateFormat.format(Date(transaction.date))
                                            val paymentLabel = when (transaction.paymentMethod) {
                                                "CREDIT" -> "Crédito"
                                                "DEBIT" -> "Débito"
                                                "PIX" -> "PIX"
                                                "MONEY" -> "Dinheiro"
                                                else -> transaction.paymentMethod
                                            }
                                            val bankLabel = transaction.bank?.let { " • $it" } ?: ""

                                            Text(
                                                text = "$dateStr | $paymentLabel",
                                                fontSize = 12.sp,
                                                color = Color.Gray,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(8.dp))

                                    Column(horizontalAlignment = Alignment.End) {
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

        // Diálogo de Filtros Avançados (Com rolagem vertical e multi-seleção)
        if (showFilterDialog) {
            AlertDialog(
                onDismissRequest = { showFilterDialog = false },
                title = { Text("Filtros Avançados", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 400.dp) // Altura máxima para ativar rolagem
                            .verticalScroll(rememberScrollState()), // Permite rolar a tela do diálogo
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Filtrar por Banco:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkForestGreen)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilterChip(
                                selected = selectedBanks.isEmpty(),
                                onClick = { selectedBanks = emptySet() },
                                label = { Text("Todos") }
                            )
                            bankOptions.forEach { bank ->
                                val isSelected = selectedBanks.contains(bank)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedBanks = if (isSelected) {
                                            selectedBanks - bank
                                        } else {
                                            selectedBanks + bank
                                        }
                                    },
                                    label = { Text(bank) }
                                )
                            }
                        }

                        HorizontalDivider(color = SageBackground)

                        Text("Filtrar por Categoria:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkForestGreen)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilterChip(
                                selected = selectedCategories.isEmpty(),
                                onClick = { selectedCategories = emptySet() },
                                label = { Text("Todas") }
                            )
                            uniqueCategories.forEach { cat ->
                                val isSelected = selectedCategories.contains(cat.name)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedCategories = if (isSelected) {
                                            selectedCategories - cat.name
                                        } else {
                                            selectedCategories + cat.name
                                        }
                                    },
                                    label = { Text(cat.name) }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showFilterDialog = false }) {
                        Text("Aplicar Filtros")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        selectedBanks = emptySet()
                        selectedCategories = emptySet()
                        searchQuery = ""
                        showFilterDialog = false
                    }) {
                        Text("Limpar Tudo")
                    }
                }
            )
        }

        // Diálogos auxiliares (Edição, Exclusão, Detalhes)
        if (showEditConfirmDialog && transactionToConfirmEdit != null) {
            AlertDialog(
                onDismissRequest = { showEditConfirmDialog = false; transactionToConfirmEdit = null },
                title = { Text("Confirmar Edição") },
                text = { Text("Deseja realmente editar o lançamento \"${transactionToConfirmEdit?.title}\"?") },
                confirmButton = {
                    Button(onClick = { transactionToEdit = transactionToConfirmEdit; showEditConfirmDialog = false; transactionToConfirmEdit = null }) {
                        Text("Sim, Editar")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEditConfirmDialog = false; transactionToConfirmEdit = null }) { Text("Cancelar") }
                }
            )
        }

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
                    TextButton(onClick = { showSingleDeleteGroupDialog = false }) { Text("Cancelar") }
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
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
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