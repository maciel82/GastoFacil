package com.example.gastofacil.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MonthDetailsScreen(
    viewModel: MainViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToExtrato: () -> Unit,
    onNavigateToMenu: () -> Unit
) {
    // Estado da Visão Atual: null = Visão 1 (Histórico Global); Pair(Ano, Mês) = Visão 2 (Detalhes do Mês)
    var activeMonthView by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    // Calendário selecionado na Visão 1 para o card do resumo
    var summaryCalendar by remember { mutableStateOf(Calendar.getInstance()) }

    // Transações Agrupadas (para a Visão 1 - Histórico Global com total agrupado)
    val groupedTransactions by viewModel.allTransactions.collectAsState(initial = emptyList())

    // Transações Brutas (para a Visão 2 - Detalhamento Mensal Real e para os Gráficos)
    val rawTransactions by viewModel.allRawTransactions.collectAsState(initial = emptyList())

    // Estados de Filtro Avançado
    var searchQuery by remember { mutableStateOf("") }
    var selectedPaymentMethods by remember { mutableStateOf(setOf<String>()) } // Vazio = Todos
    var selectedBanks by remember { mutableStateOf(setOf<String>()) }          // Vazio = Todos
    var selectedCategories by remember { mutableStateOf(setOf<String>()) }     // Vazio = Todas
    var showFilterDialog by remember { mutableStateOf(false) }

    // Estado para controlar a abertura do Painel Lateral do Gráfico
    var showChartSidePanel by remember { mutableStateOf(false) }

    // Estados de Seleção e Ações
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var transactionToConfirmEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    var showEditConfirmDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    var transactionToDelete by remember { mutableStateOf<TransactionEntity?>(null) }
    var showSingleDeleteGroupDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var transactionToViewDetails by remember { mutableStateOf<TransactionEntity?>(null) }

    val isSelecting = selectedIds.isNotEmpty()

    // Categorias e Opções
    val categoriesList by viewModel.categories.collectAsState(initial = emptyList())
    val uniqueCategories = remember(categoriesList) { categoriesList.distinctBy { it.name } }
    val bankOptions = remember { listOf("Nubank", "PicPay", "RecargaPay", "Outro") }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale("pt", "BR")) }
    val monthYearHeaderFormat = remember { SimpleDateFormat("MMMM yyyy", Locale("pt", "BR")) }
    val dateFormat = remember { SimpleDateFormat("dd MMM", Locale("pt", "BR")) }

    // ==========================================
    // LÓGICA DE FILTRAGEM
    // ==========================================
    val isGlobalView = activeMonthView == null

    val baseTransactions = remember(groupedTransactions, rawTransactions, activeMonthView) {
        if (activeMonthView == null) {
            groupedTransactions
        } else {
            val (y, m) = activeMonthView!!
            rawTransactions.filter { tx ->
                val cal = Calendar.getInstance().apply { timeInMillis = tx.date }
                cal.get(Calendar.YEAR) == y && cal.get(Calendar.MONTH) == m
            }
        }
    }

    val filteredTransactions = remember(
        baseTransactions, searchQuery, selectedPaymentMethods, selectedBanks, selectedCategories, isGlobalView
    ) {
        baseTransactions.filter { tx ->
            val matchesSearch = if (isGlobalView) {
                searchQuery.isBlank() ||
                        tx.title.contains(searchQuery, ignoreCase = true) ||
                        (tx.storeLocation?.contains(searchQuery, ignoreCase = true) == true)
            } else true

            val matchesMethod = selectedPaymentMethods.isEmpty() || selectedPaymentMethods.contains(tx.paymentMethod)
            val matchesBank = selectedBanks.isEmpty() || (tx.bank != null && selectedBanks.contains(tx.bank))
            val matchesCategory = selectedCategories.isEmpty() || selectedCategories.contains(tx.category)

            matchesSearch && matchesMethod && matchesBank && matchesCategory
        }
    }

    // Agrupamento por Mês para o Histórico Global
    val groupedGlobalTransactions = remember(filteredTransactions) {
        filteredTransactions.groupBy { tx ->
            monthYearHeaderFormat.format(Date(tx.date)).uppercase(Locale("pt", "BR"))
        }
    }

    val hasActiveFilters = searchQuery.isNotBlank() || selectedPaymentMethods.isNotEmpty() || selectedBanks.isNotEmpty() || selectedCategories.isNotEmpty()

    Box(modifier = Modifier.fillMaxSize()) {
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
                            text = if (isSelecting) "${selectedIds.size} selecionados"
                            else if (isGlobalView) "Histórico financeiro"
                            else monthFormat.format(Calendar.getInstance().apply { set(activeMonthView!!.first, activeMonthView!!.second, 1) }.time).uppercase(Locale("pt", "BR")),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        if (!isGlobalView) {
                            IconButton(onClick = { activeMonthView = null }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                            }
                        }
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
                NavigationBar(containerColor = SageCardBackground) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Início") },
                        label = { Text("Início") },
                        selected = false,
                        colors = NavigationBarItemDefaults.colors(unselectedIconColor = DarkForestGreen, unselectedTextColor = DarkForestGreen),
                        onClick = onNavigateToHome
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Histórico") },
                        label = { Text("Histórico") },
                        selected = true,
                        colors = NavigationBarItemDefaults.colors(indicatorColor = WarmGoldFab, selectedIconColor = DarkForestGreen, selectedTextColor = DarkForestGreen, unselectedIconColor = DarkForestGreen, unselectedTextColor = DarkForestGreen),
                        onClick = { activeMonthView = null }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "Extrato") },
                        label = { Text("Extrato") },
                        selected = false,
                        colors = NavigationBarItemDefaults.colors(unselectedIconColor = DarkForestGreen, unselectedTextColor = DarkForestGreen),
                        onClick = onNavigateToExtrato
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Menu, contentDescription = "Menu") },
                        label = { Text("Menu") },
                        selected = false,
                        colors = NavigationBarItemDefaults.colors(unselectedIconColor = DarkForestGreen, unselectedTextColor = DarkForestGreen),
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // =========================================================================
                // VISÃO 1: HISTÓRICO GLOBAL
                // =========================================================================
                if (isGlobalView) {
                    // 1. Barra de Pesquisa
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

                    // 2. Card de Resumo do Mês (Com gestos de deslizar e clique para detalhar)
                    val selYear = summaryCalendar[Calendar.YEAR]
                    val selMonth = summaryCalendar[Calendar.MONTH]
                    val monthExpenses = remember(rawTransactions, selYear, selMonth) {
                        rawTransactions.filter { tx ->
                            val c = Calendar.getInstance().apply { timeInMillis = tx.date }
                            c.get(Calendar.YEAR) == selYear && c.get(Calendar.MONTH) == selMonth && tx.type == "EXPENSE"
                        }.sumOf { it.amount }
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkForestGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .pointerInput(Unit) {
                                var dragAmount = 0f
                                detectHorizontalDragGestures(
                                    onDragEnd = {
                                        if (dragAmount > 50f) {
                                            summaryCalendar = (summaryCalendar.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                                        } else if (dragAmount < -50f) {
                                            summaryCalendar = (summaryCalendar.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                                        }
                                        dragAmount = 0f
                                    },
                                    onHorizontalDrag = { _, delta -> dragAmount += delta }
                                )
                            }
                            .clickable {
                                activeMonthView = Pair(selYear, selMonth)
                            }
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
                                    summaryCalendar = (summaryCalendar.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                                }) {
                                    Icon(Icons.Default.ChevronLeft, contentDescription = "Mês Anterior", tint = Color.White)
                                }

                                Text(
                                    text = monthFormat.format(summaryCalendar.time).uppercase(Locale("pt", "BR")),
                                    fontSize = 18.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )

                                IconButton(onClick = {
                                    summaryCalendar = (summaryCalendar.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                                }) {
                                    Icon(Icons.Default.ChevronRight, contentDescription = "Próximo Mês", tint = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = currencyFormat.format(monthExpenses),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF9A9A)
                            )
                            Text(
                                text = "Gastos do Mês (Toque para detalhar)",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // 3. Barra com Botão "Filtros" (Esquerda) e Gráfico de Barras (Direita)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showFilterDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (hasActiveFilters) WarmGoldFab.copy(alpha = 0.2f) else Color.White,
                                contentColor = DarkForestGreen
                            )
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (hasActiveFilters) "Filtros (Ativos)" else "Filtros",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }

                        // Ícone do Gráfico que abre o Painel Lateral
                        IconButton(
                            onClick = { showChartSidePanel = true },
                            modifier = Modifier
                                .background(Color.White, shape = RoundedCornerShape(10.dp))
                                .size(40.dp)
                        ) {
                            Icon(Icons.Default.BarChart, contentDescription = "Abrir Gráfico", tint = DarkForestGreen)
                        }
                    }

                    // 4. Lista do Histórico Contínuo Global Agrupada por Mês
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        groupedGlobalTransactions.forEach { (monthName, txGroup) ->
                            item {
                                Text(
                                    text = monthName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkForestGreen,
                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                )
                            }

                            items(txGroup, key = { it.id }) { transaction ->
                                TransactionListItem(
                                    transaction = transaction,
                                    isGlobalView = true,
                                    isSelected = selectedIds.contains(transaction.id),
                                    isSelecting = isSelecting,
                                    dateFormat = dateFormat,
                                    currencyFormat = currencyFormat,
                                    onItemClick = {
                                        if (isSelecting) {
                                            selectedIds = if (selectedIds.contains(transaction.id)) selectedIds - transaction.id else selectedIds + transaction.id
                                        } else {
                                            transactionToViewDetails = transaction
                                        }
                                    },
                                    onItemLongClick = {
                                        selectedIds = if (selectedIds.contains(transaction.id)) selectedIds - transaction.id else selectedIds + transaction.id
                                    },
                                    onSwipeEdit = {
                                        transactionToConfirmEdit = transaction
                                        showEditConfirmDialog = true
                                    },
                                    onSwipeDelete = {
                                        transactionToDelete = transaction
                                        showSingleDeleteGroupDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
                // =========================================================================
                // VISÃO 2: DETALHAMENTO DO MÊS ESPECÍFICO
                // =========================================================================
                else {
                    val monthIncomes = baseTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
                    val monthExpenses = baseTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                    val monthCashExpenses = baseTransactions.filter { it.type == "EXPENSE" && it.paymentMethod != "CREDIT" }.sumOf { it.amount }
                    val monthBalance = monthIncomes - monthCashExpenses

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
                            Text(
                                text = currencyFormat.format(monthExpenses),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFEF9A9A)
                            )
                            Text(
                                text = "Gastos Acumulados no Mês",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.8f)
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = Color.White.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = currencyFormat.format(monthIncomes),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(text = "Receita do Mês", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = currencyFormat.format(monthBalance),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (monthBalance >= 0) Color.White else Color(0xFFEF9A9A)
                                    )
                                    Text(text = "Saldo do Mês", fontSize = 10.sp, color = Color.White.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { showFilterDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (hasActiveFilters) WarmGoldFab.copy(alpha = 0.2f) else Color.White,
                                contentColor = DarkForestGreen
                            )
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (hasActiveFilters) "Filtros (Ativos)" else "Filtros",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(filteredTransactions, key = { it.id }) { transaction ->
                            TransactionListItem(
                                transaction = transaction,
                                isGlobalView = false,
                                isSelected = selectedIds.contains(transaction.id),
                                isSelecting = isSelecting,
                                dateFormat = dateFormat,
                                currencyFormat = currencyFormat,
                                onItemClick = {
                                    if (isSelecting) {
                                        selectedIds = if (selectedIds.contains(transaction.id)) selectedIds - transaction.id else selectedIds + transaction.id
                                    } else {
                                        transactionToViewDetails = transaction
                                    }
                                },
                                onItemLongClick = {
                                    selectedIds = if (selectedIds.contains(transaction.id)) selectedIds - transaction.id else selectedIds + transaction.id
                                },
                                onSwipeEdit = {
                                    transactionToConfirmEdit = transaction
                                    showEditConfirmDialog = true
                                },
                                onSwipeDelete = {
                                    transactionToDelete = transaction
                                    showSingleDeleteGroupDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // PAINEL LATERAL (SIDE SHEET) DO GRÁFICO
        // =========================================================================
        AnimatedVisibility(
            visible = showChartSidePanel,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showChartSidePanel = false },
                contentAlignment = Alignment.CenterEnd
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(320.dp)
                        .clickable(enabled = false) {}, // Evita fechar ao clicar dentro do painel
                    color = SageBackground,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Gráfico Mensal",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = DarkForestGreen
                            )
                            IconButton(onClick = { showChartSidePanel = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Fechar")
                            }
                        }

                        HorizontalDivider(color = DarkForestGreen.copy(alpha = 0.2f))

                        Text(
                            text = "Toque em uma barra para abrir o detalhamento do mês correspondente:",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )

                        // Gráfico de Barras Vertical Ampliado no Painel Lateral
                        ExpandedVerticalBarChart(
                            rawTransactions = rawTransactions,
                            onMonthSelected = { y, m ->
                                activeMonthView = Pair(y, m)
                                showChartSidePanel = false
                            }
                        )
                    }
                }
            }
        }

        // ==========================================
        // DIÁLOGO DE FILTROS AVANÇADOS UNIFICADO
        // ==========================================
        if (showFilterDialog) {
            AlertDialog(
                onDismissRequest = { showFilterDialog = false },
                title = { Text("Filtros Avançados", fontWeight = FontWeight.Bold) },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 420.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Forma de Pagamento:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DarkForestGreen)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            FilterChip(
                                selected = selectedPaymentMethods.isEmpty(),
                                onClick = { selectedPaymentMethods = emptySet() },
                                label = { Text("Todos") }
                            )
                            listOf(
                                "CREDIT" to "Crédito",
                                "PIX" to "PIX",
                                "DEBIT" to "Débito",
                                "MONEY" to "Dinheiro"
                            ).forEach { (code, label) ->
                                val isSelected = selectedPaymentMethods.contains(code)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedPaymentMethods = if (isSelected) selectedPaymentMethods - code else selectedPaymentMethods + code
                                    },
                                    label = { Text(label) }
                                )
                            }
                        }

                        HorizontalDivider(color = SageBackground)

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
                                        selectedBanks = if (isSelected) selectedBanks - bank else selectedBanks + bank
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
                                        selectedCategories = if (isSelected) selectedCategories - cat.name else selectedCategories + cat.name
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
                        selectedPaymentMethods = emptySet()
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

        // Outros diálogos auxiliares
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

// =========================================================================
// COMPONENTE: ITEM DA LISTA COM SWIPE E SELEÇÃO
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun TransactionListItem(
    transaction: TransactionEntity,
    isGlobalView: Boolean,
    isSelected: Boolean,
    isSelecting: Boolean,
    dateFormat: SimpleDateFormat,
    currencyFormat: NumberFormat,
    onItemClick: () -> Unit,
    onItemLongClick: () -> Unit,
    onSwipeEdit: () -> Unit,
    onSwipeDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            when (dismissValue) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onSwipeDelete()
                    false
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onSwipeEdit()
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
                        onClick = onItemClick,
                        onLongClick = onItemLongClick
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
                                onCheckedChange = { onItemClick() }
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
                        val displayAmount = if (isGlobalView && transaction.isInstallment && transaction.totalInstallments > 1) {
                            transaction.amount * transaction.totalInstallments
                        } else {
                            transaction.amount
                        }

                        Text(
                            text = (if (transaction.type == "INCOME") "+ " else "- ") + currencyFormat.format(displayAmount),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = if (transaction.type == "INCOME") IncomeGreenText else ExpenseRedText
                        )

                        if (transaction.isInstallment) {
                            val installmentLabel = if (isGlobalView && transaction.totalInstallments > 1) {
                                "em ${transaction.totalInstallments}x"
                            } else {
                                "Parcela ${transaction.currentInstallment}/${transaction.totalInstallments}"
                            }

                            Text(
                                text = installmentLabel,
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

// =========================================================================
// COMPONENTE: GRÁFICO DE BARRAS VERTICAL AMPLIADO PARA O PAINEL LATERAL
// =========================================================================
@Composable
private fun ExpandedVerticalBarChart(
    rawTransactions: List<TransactionEntity>,
    onMonthSelected: (year: Int, month: Int) -> Unit
) {
    val monthlyTotals = remember(rawTransactions) {
        val expenseTx = rawTransactions.filter { it.type == "EXPENSE" }
        if (expenseTx.isEmpty()) {
            val cal = Calendar.getInstance()
            listOf(Triple(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), 0.0))
        } else {
            val map = mutableMapOf<Pair<Int, Int>, Double>()
            
            val currentCal = Calendar.getInstance()
            val currentKey = Pair(currentCal.get(Calendar.YEAR), currentCal.get(Calendar.MONTH))
            map[currentKey] = 0.0

            expenseTx.forEach { tx ->
                val cal = Calendar.getInstance().apply { timeInMillis = tx.date }
                val key = Pair(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH))
                map[key] = (map[key] ?: 0.0) + tx.amount
            }

            map.entries
                .map { (key, total) -> Triple(key.first, key.second, total) }
                .sortedWith(compareBy<Triple<Int, Int, Double>> { it.first }.thenBy { it.second })
        }
    }

    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }
    val maxVal = remember(monthlyTotals) { (monthlyTotals.maxOfOrNull { it.third } ?: 1.0).coerceAtLeast(1.0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        monthlyTotals.forEach { (y, m, amount) ->
            val ratio = (amount / maxVal).coerceIn(0.05, 1.0).toFloat()
            val monthLabel = SimpleDateFormat("MMMM yyyy", Locale("pt", "BR")).format(Calendar.getInstance().apply { set(y, m, 1) }.time).uppercase()

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMonthSelected(y, m) },
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = monthLabel, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = DarkForestGreen)
                        Text(text = currencyFormat.format(amount), fontSize = 12.sp, color = ExpenseRedText)
                    }

                    Box(
                        modifier = Modifier
                            .width(60.dp)
                            .height(14.dp)
                            .background(SageBackground, shape = RoundedCornerShape(7.dp)),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(ratio)
                                .background(DarkForestGreen, shape = RoundedCornerShape(7.dp))
                        )
                    }
                }
            }
        }
    }
}
