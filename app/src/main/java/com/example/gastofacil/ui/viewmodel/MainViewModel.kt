package com.example.gastofacil.ui.viewmodel

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gastofacil.data.local.AppDatabase
import com.example.gastofacil.data.local.CategoryEntity
import com.example.gastofacil.data.local.TransactionEntity
import com.example.gastofacil.data.local.util.BackupManager
import com.example.gastofacil.data.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository

    val categories: Flow<List<CategoryEntity>> = AppDatabase.getDatabase(application)
        .categoryDao()
        .getAllCategories()

    private val prefs = application.getSharedPreferences("gastofacil_prefs", Context.MODE_PRIVATE)

    var monthlyBudgetLimit by mutableStateOf(prefs.getFloat("monthly_budget_limit", 0f).toDouble())
        private set

    var applyLimitToAllMonths by mutableStateOf(prefs.getBoolean("apply_limit_all_months", true))
        private set

    init {
        val database = AppDatabase.getDatabase(application)
        val transactionDao = database.transactionDao()
        repository = TransactionRepository(transactionDao)

        createNotificationChannel()

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

    fun updateBudgetSettings(limit: Double, applyToAll: Boolean) {
        monthlyBudgetLimit = limit
        applyLimitToAllMonths = applyToAll
        prefs.edit().apply {
            putFloat("monthly_budget_limit", limit.toFloat())
            putBoolean("apply_limit_all_months", applyToAll)
            apply()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "budget_channel",
                "Alertas de Orçamento",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações de limite de gastos mensais atingido"
            }
            val notificationManager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun checkBudgetAndNotify(year: Int, month: Int, totalExpenses: Double) {
        if (monthlyBudgetLimit <= 0.0) return

        val shouldCheck = if (applyLimitToAllMonths) {
            true
        } else {
            val currentCal = Calendar.getInstance()
            currentCal.get(Calendar.YEAR) == year && currentCal.get(Calendar.MONTH) == month
        }

        if (shouldCheck && totalExpenses >= monthlyBudgetLimit) {
            sendBudgetNotification(totalExpenses, monthlyBudgetLimit)
        }
    }

    private fun sendBudgetNotification(currentExpense: Double, limit: Double) {
        val context = getApplication<Application>()
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val formattedExpense = currencyFormat.format(currentExpense)
        val formattedLimit = currencyFormat.format(limit)

        val builder = NotificationCompat.Builder(context, "budget_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("⚠️ Limite de Gastos Atingido!")
            .setContentText("Seus gastos ($formattedExpense) ultrapassaram o limite de $formattedLimit.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify(1001, builder.build())
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun addTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.insertTransaction(transaction)
        }
    }

    fun addCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            AppDatabase.getDatabase(getApplication())
                .categoryDao()
                .insertCategory(CategoryEntity(name = name.trim()))
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            AppDatabase.getDatabase(getApplication())
                .categoryDao()
                .deleteCategory(category)
        }
    }

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

    val allTransactions: Flow<List<TransactionEntity>> = AppDatabase.getDatabase(getApplication())
        .transactionDao()
        .getAllTransactions()
        .map { list ->
            val groupedMap = mutableMapOf<String, TransactionEntity>()
            val singleList = mutableListOf<TransactionEntity>()

            list.forEach { tx ->
                val groupKey = if (!tx.installmentGroupId.isNullOrBlank()) {
                    tx.installmentGroupId
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

    // Exportação dos dados brutos completos para formato JSON
    suspend fun exportAllDataJson(): String {
        val db = AppDatabase.getDatabase(getApplication())
        val txList = db.transactionDao().getAllTransactionsList()
        val catList = db.categoryDao().getAllCategoriesList()
        return BackupManager.exportToJson(txList, catList)
    }

    // Importação do arquivo de backup JSON com desduplicação de categorias
    fun importBackupData(jsonString: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val data = BackupManager.parseJson(jsonString)
                val db = AppDatabase.getDatabase(getApplication())

                if (data.transactions.isNotEmpty()) {
                    db.transactionDao().insertTransactions(data.transactions)
                }
                if (data.categories.isNotEmpty()) {
                    val existingCatNames = db.categoryDao().getAllCategoriesList()
                        .map { it.name.trim().lowercase() }.toSet()

                    val uniqueNewCategories = data.categories
                        .distinctBy { it.name.trim().lowercase() }
                        .filter { it.name.trim().lowercase() !in existingCatNames }

                    if (uniqueNewCategories.isNotEmpty()) {
                        db.categoryDao().insertCategories(uniqueNewCategories)
                    }
                }
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }
}
