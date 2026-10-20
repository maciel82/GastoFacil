package com.example.gastofacil.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gastofacil.ui.theme.*
import com.example.gastofacil.ui.viewmodel.MainViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// Cores para as fatias do gráfico de pizza/rosca
val ChartPalette = listOf(
    Color(0xFFEF5350), // Vermelho
    Color(0xFF42A5F5), // Azul
    Color(0xFF66BB6A), // Verde
    Color(0xFFA1887F), // Castanho
    Color(0xFFAB47BC), // Roxo
    Color(0xFFFFA726), // Laranja
    Color(0xFF26A69A), // Ciano/Verde Água
    Color(0xFFEC407A)  // Rosa
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    var selectedCalendar by remember { mutableStateOf(Calendar.getInstance()) }
    val selectedYear = selectedCalendar[Calendar.YEAR]
    val selectedMonth = selectedCalendar[Calendar.MONTH]

    val transactionsFlow = remember(viewModel, selectedYear, selectedMonth) {
        viewModel.getTransactionsForMonth(selectedYear, selectedMonth)
    }
    val transactions by transactionsFlow.collectAsState(initial = emptyList())

    val monthFormat = remember { SimpleDateFormat("MMMM yyyy", Locale("pt", "BR")) }
    val monthTitleText = remember(selectedCalendar) {
        monthFormat.format(selectedCalendar.time).uppercase(Locale("pt", "BR"))
    }
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("pt", "BR")) }

    // Gastos filtrados do mês
    val expenses = remember(transactions) { transactions.filter { it.type == "EXPENSE" } }
    val totalExpenses = remember(expenses) { expenses.sumOf { it.amount } }

    // Agrupamento por Categoria
    val categoryTotals = remember(expenses) {
        expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    // Agrupamento por Banco
    val bankTotals = remember(expenses) {
        expenses.groupBy { it.bank ?: "Outro" }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }
    }

    Scaffold(
        containerColor = SageBackground,
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = TopBarHeaderGreen,
                    titleContentColor = Color.White
                ),
                title = { Text("Analytics & Gráficos", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = Color.White
                        )
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Seletor de Mês
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SageCardBackground),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        val newCal = (selectedCalendar.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                        selectedCalendar = newCal
                    }) {
                        Icon(Icons.Default.ChevronLeft, contentDescription = "Mês Anterior", tint = DarkForestGreen)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = monthTitleText,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkForestGreen
                        )
                        Text(
                            text = "Total: ${currencyFormat.format(totalExpenses)}",
                            fontSize = 12.sp,
                            color = ExpenseRedText,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    IconButton(onClick = {
                        val newCal = (selectedCalendar.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                        selectedCalendar = newCal
                    }) {
                        Icon(Icons.Default.ChevronRight, contentDescription = "Próximo Mês", tint = DarkForestGreen)
                    }
                }
            }

            if (expenses.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .padding(24.dp)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Sem lançamentos de gastos para este mês.", color = Color.Gray, fontSize = 14.sp)
                    }
                }
            } else {
                // 📊 Gráfico 1: Gastos por Categoria (Rosca)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Gastos por Categoria",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = DarkForestGreen
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        DoughnutChart(
                            data = categoryTotals,
                            totalAmount = totalExpenses,
                            currencyFormat = currencyFormat
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Legenda por Categoria
                        categoryTotals.forEachIndexed { index, (category, amount) ->
                            val color = ChartPalette[index % ChartPalette.size]
                            val percentage = if (totalExpenses > 0) (amount / totalExpenses) * 100 else 0.0

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .background(color, shape = RoundedCornerShape(3.dp))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = category, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                }
                                Text(
                                    text = "${currencyFormat.format(amount)} (${String.format(Locale.getDefault(), "%.1f", percentage)}%)",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }

                // 🏦 Gráfico 2: Gastos por Banco / Conta (Barras Horizontais)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Gastos por Banco / Conta",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = DarkForestGreen
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        bankTotals.forEachIndexed { index, (bank, amount) ->
                            val color = ChartPalette[(index + 2) % ChartPalette.size]
                            val ratio = if (totalExpenses > 0) (amount / totalExpenses).toFloat() else 0f

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = bank, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(text = currencyFormat.format(amount), fontSize = 13.sp, color = DarkForestGreen)
                                }
                                Spacer(modifier = Modifier.height(4.dp))

                                // Barra de Progresso Customizada
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .background(SageBackground, shape = RoundedCornerShape(5.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxHeight()
                                            .fillMaxWidth(fraction = ratio)
                                            .background(color, shape = RoundedCornerShape(5.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DoughnutChart(
    data: List<Pair<String, Double>>,
    totalAmount: Double,
    currencyFormat: NumberFormat
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        progress.snapTo(0f)
        progress.animateTo(1f, animationSpec = tween(800))
    }

    Box(
        modifier = Modifier.size(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            var startAngle = -90f
            val strokeWidth = 32.dp.toPx()

            data.forEachIndexed { index, (_, amount) ->
                val sweepAngle = ((amount / totalAmount) * 360f).toFloat() * progress.value
                val color = ChartPalette[index % ChartPalette.size]

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = strokeWidth)
                )

                startAngle += sweepAngle
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Total", fontSize = 11.sp, color = Color.Gray)
            Text(
                text = currencyFormat.format(totalAmount),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = DarkForestGreen
            )
        }
    }
}
