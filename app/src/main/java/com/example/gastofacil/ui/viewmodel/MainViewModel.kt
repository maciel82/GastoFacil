package com.example.gastofacil.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gastofacil.data.local.AppDatabase
import com.example.gastofacil.data.local.CategoryEntity
import com.example.gastofacil.data.local.TransactionEntity
import com.example.gastofacil.data.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository

    // Declaração do fluxo de categorias logo no topo para evitar erros de ordem
    val categories: Flow<List<CategoryEntity>> = AppDatabase.getDatabase(application)
        .categoryDao()
        .getAllCategories()

    init {
        val database = AppDatabase.getDatabase(application)
        val transactionDao = database.transactionDao()
        repository = TransactionRepository(transactionDao)

        // Insere categorias padrão automaticamente se a tabela estiver vazia
        viewModelScope.launch {
            val categoryDao = database.categoryDao()
            if (categoryDao.getCategoryCount() == 0) {
                val defaultCategories = listOf(
                    "Lazer", "Mercado", "Alimentação", "Compras Online",
                    "Transporte", "Educação", "Contas de casa",
                    "Trabalho", "Saúde Capilar", "Contas de outros"
                ).map { CategoryEntity(name = it, isDefault = true) }
                categoryDao.insertCategories(defaultCategories)
            }
        }
    }

    // Insere uma nova transação
    fun addTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.insertTransaction(transaction)
        }
    }

    // Adiciona nova categoria personalizada
    fun addCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            AppDatabase.getDatabase(getApplication())
                .categoryDao()
                .insertCategory(CategoryEntity(name = name.trim()))
        }
    }

    // Exclui categoria personalizada
    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            AppDatabase.getDatabase(getApplication())
                .categoryDao()
                .deleteCategory(category)
        }
    }

    // Busca transações de um mês e ano específicos
    fun getTransactionsForMonth(year: Int, month: Int): Flow<List<TransactionEntity>> {
        val calendar = Calendar.getInstance().apply {
            set(year, month, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startDate = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        val endDate = calendar.timeInMillis - 1

        val dao = AppDatabase.getDatabase(getApplication()).transactionDao()
        return dao.getTransactionsByDateRange(startDate, endDate)
    }

    // Histórico geral de transações agrupando parcelas
    val allTransactions: Flow<List<TransactionEntity>> = AppDatabase.getDatabase(getApplication())
        .transactionDao()
        .getAllTransactions()
        .map { list ->
            val groupedMap = mutableMapOf<String, TransactionEntity>()
            val singleList = mutableListOf<TransactionEntity>()

            list.forEach { tx ->
                val groupKey = if (!tx.installmentGroupId.isNullOrBlank()) {
                    tx.installmentGroupId!!
                } else if (tx.isInstallment) {
                    "${tx.title}_${tx.amount}_${tx.totalInstallments}"
                } else {
                    null
                }

                if (groupKey != null) {
                    if (!groupedMap.containsKey(groupKey)) {
                        groupedMap[groupKey] = tx
                    }
                } else {
                    singleList.add(tx)
                }
            }
            (groupedMap.values + singleList).sortedByDescending { it.date }
        }

    fun deleteTransactions(transactions: List<TransactionEntity>) {
        viewModelScope.launch {
            repository.deleteTransactions(transactions)
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    // Projeções futuras de parcelas
    fun getUpcomingProjections(): Flow<List<Pair<String, Double>>> {
        return AppDatabase.getDatabase(getApplication()).transactionDao().getAllTransactions().map { list ->
            val expenseList = list.filter { it.type == "EXPENSE" }
            val calendar = Calendar.getInstance()
            val projectionList = mutableListOf<Pair<String, Double>>()

            for (i in 1..6) {
                calendar.add(Calendar.MONTH, 1)
                val year = calendar.get(Calendar.YEAR)
                val month = calendar.get(Calendar.MONTH)

                val monthName = SimpleDateFormat("MMM yyyy", Locale("pt", "BR")).format(calendar.time).uppercase()

                val totalForMonth = expenseList.filter { tx ->
                    val txCal = Calendar.getInstance().apply { timeInMillis = tx.date }
                    txCal.get(Calendar.YEAR) == year && txCal.get(Calendar.MONTH) == month
                }.sumOf { it.amount }

                if (totalForMonth > 0.0) {
                    projectionList.add(Pair(monthName, totalForMonth))
                }
            }
            projectionList
        }
    }

    val totalAllTimeIncomes: Flow<Double> = AppDatabase.getDatabase(getApplication())
        .transactionDao()
        .getAllTransactions()
        .map { list ->
            list.filter { it.type == "INCOME" }.sumOf { it.amount }
        }

    val totalAllTimeBalance: Flow<Double> = AppDatabase.getDatabase(getApplication())
        .transactionDao()
        .getAllTransactions()
        .map { list ->
            val totalIncomes = list.filter { it.type == "INCOME" }.sumOf { it.amount }
            val totalCashExpenses = list.filter { it.type == "EXPENSE" && it.paymentMethod != "CREDIT" }.sumOf { it.amount }
            totalIncomes - totalCashExpenses
        }

    fun deleteTransactionGroup(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransactionGroup(transaction)
        }
    }
}