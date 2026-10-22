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
import com.example.gastofacil.data.local.GoalEntity
import com.example.gastofacil.data.local.NotificationEntity
import com.example.gastofacil.data.local.RecurringBillEntity
import com.example.gastofacil.data.local.SuggestedTransactionEntity
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

    val allNotifications: Flow<List<NotificationEntity>> = AppDatabase.getDatabase(application)
        .notificationDao()
        .getAllNotifications()

    val unreadNotificationsCount: Flow<Int> = AppDatabase.getDatabase(application)
        .notificationDao()
        .getUnreadCount()

    val allSuggestions: Flow<List<SuggestedTransactionEntity>> = AppDatabase.getDatabase(application)
        .suggestedTransactionDao()
        .getAllSuggestions()

    fun acceptSuggestion(suggestion: SuggestedTransactionEntity) {
        viewModelScope.launch {
            val transaction = TransactionEntity(
                title = suggestion.title,
                amount = suggestion.amount,
                type = suggestion.type,
                paymentMethod = suggestion.paymentMethod,
                bank = suggestion.bank,
                category = suggestion.category,
                date = suggestion.date,
                storeLocation = null
            )
            repository.insertTransaction(transaction)
            AppDatabase.getDatabase(getApplication())
                .suggestedTransactionDao()
                .deleteSuggestion(suggestion)
            checkAllBudgetsAcrossMonths()
        }
    }

    fun dismissSuggestion(suggestion: SuggestedTransactionEntity) {
        viewModelScope.launch {
            AppDatabase.getDatabase(getApplication())
                .suggestedTransactionDao()
                .deleteSuggestion(suggestion)
        }
    }

    fun isNotificationServiceEnabled(context: Context): Boolean {
        return try {
            val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
            enabledPackages.contains(context.packageName)
        } catch (e: Exception) {
            val flat = android.provider.Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            flat?.contains(context.packageName) == true
        }
    }

    fun hasSmsPermission(context: Context): Boolean {
        return androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECEIVE_SMS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    fun rebindNotificationListener(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isNotificationServiceEnabled(context)) {
            try {
                val componentName = android.content.ComponentName(
                    context,
                    com.example.gastofacil.data.service.BankNotificationService::class.java
                )
                android.service.notification.NotificationListenerService.requestRebind(componentName)
                android.util.Log.d("MainViewModel", "Solicitado rebind do NotificationListenerService.")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

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
            checkAllBudgetsAcrossMonths()
            checkRecurringBillsDueSoon()
        }
    }

    fun updateBudgetSettings(limit: Double, applyToAll: Boolean) {
        monthlyBudgetLimit = limit
        applyLimitToAllMonths = applyToAll
        
        val editor = prefs.edit()
        editor.putFloat("monthly_budget_limit", limit.toFloat())
        editor.putBoolean("apply_limit_all_months", applyToAll)
        // Limpa patamares armazenados anteriormente para que a nova configuração de limite possa reavaliar e notificar imediatamente
        prefs.all.keys.filter { it.startsWith("notified_tier_") }.forEach { key ->
            editor.remove(key)
        }
        editor.apply()

        checkAllBudgetsAcrossMonths()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "budget_channel",
                "Alertas de Orçamento",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações de limite de gastos mensais e parcelamentos futuros"
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager = getApplication<Application>().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun checkBudgetAndNotify(year: Int, month: Int, totalExpenses: Double) {
        checkAllBudgetsAcrossMonths()
    }

    // Varre o mês atual e os próximos 12 meses (para capturar parcelamentos futuros)
    fun checkAllBudgetsAcrossMonths() {
        if (monthlyBudgetLimit <= 0.0) return

        viewModelScope.launch {
            val db = AppDatabase.getDatabase(getApplication())
            val allTx = db.transactionDao().getAllTransactionsList()
            val expenseList = allTx.filter { it.type == "EXPENSE" }

            val calendar = Calendar.getInstance()
            val currentYear = calendar.get(Calendar.YEAR)
            val currentMonth = calendar.get(Calendar.MONTH)

            // Analisa do mês atual até 12 meses à frente
            for (i in 0..12) {
                val calIter = Calendar.getInstance().apply {
                    set(currentYear, currentMonth, 1, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.MONTH, i)
                }
                val y = calIter.get(Calendar.YEAR)
                val m = calIter.get(Calendar.MONTH)

                // Se a regra for apenas para o mês atual e não estivermos nele, pula
                if (!applyLimitToAllMonths && i > 0) continue

                // Soma os gastos reais do mês (incluindo parcelas futuras que caem nele)
                val totalExpMonth = expenseList.filter { tx ->
                    val txCal = Calendar.getInstance().apply { timeInMillis = tx.date }
                    txCal.get(Calendar.YEAR) == y && txCal.get(Calendar.MONTH) == m
                }.sumOf { it.amount }

                evaluateAndNotifyThreshold(y, m, totalExpMonth, monthlyBudgetLimit)
            }
        }
    }

    private fun evaluateAndNotifyThreshold(year: Int, month: Int, expenses: Double, limit: Double) {
        if (limit <= 0.0) return

        val percentage = (expenses / limit) * 100.0
        val currentTier = when {
            percentage >= 100.0 -> 100
            percentage >= 90.0 -> 90
            percentage >= 80.0 -> 80
            percentage >= 70.0 -> 70
            else -> 0
        }

        // Chave única por mês/ano para armazenar o último nível notificado
        val prefKey = "notified_tier_${year}_$month"
        val lastTier = prefs.getInt(prefKey, 0)

        if (currentTier > lastTier) {
            // Se o patamar atual subiu em relação ao último notificado, atualiza e dispara o novo alerta
            prefs.edit().putInt(prefKey, currentTier).apply()

            val monthFormat = SimpleDateFormat("MMMM yyyy", Locale("pt", "BR"))
            val cal = Calendar.getInstance().apply {
                set(year, month, 1)
            }
            val monthName = monthFormat.format(cal.time).uppercase(Locale("pt", "BR"))

            sendTierNotification(monthName, expenses, limit, currentTier)
        } else if (currentTier < lastTier) {
            // Se os gastos diminuíram (ex: cancelou compra), reseta o nível
            prefs.edit().putInt(prefKey, currentTier).apply()
        }
    }

    private fun sendTierNotification(monthName: String, expenses: Double, limit: Double, tier: Int) {
        val context = getApplication<Application>()
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val formattedExp = currencyFormat.format(expenses)
        val formattedLimit = currencyFormat.format(limit)

        val title = when (tier) {
            100 -> "🚨 Limite atingido! em $monthName!"
            90 -> "⚠️ 90% do Limite em $monthName!"
            80 -> "⚠️ 80% do Limite em $monthName!"
            else -> "ℹ️ 70% do Limite em $monthName"
        }

        val text = "$monthName: Gastos acumulados de $formattedExp (Limite: $formattedLimit)."

        val builder = NotificationCompat.Builder(context, "budget_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                // ID único combinando o mês e o nível para permitir múltiplas notificações simultâneas
                notify("$monthName-$tier".hashCode(), builder.build())
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        // Salva registro no histórico de notificações do banco local
        viewModelScope.launch {
            val entity = NotificationEntity(
                title = title,
                message = text,
                timestamp = System.currentTimeMillis(),
                isRead = false,
                tier = tier,
                monthName = monthName
            )
            AppDatabase.getDatabase(getApplication())
                .notificationDao()
                .insertNotification(entity)
        }
    }

    fun markNotificationAsRead(id: Long) {
        viewModelScope.launch {
            AppDatabase.getDatabase(getApplication())
                .notificationDao()
                .markAsRead(id)
        }
    }

    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            AppDatabase.getDatabase(getApplication())
                .notificationDao()
                .markAllAsRead()
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            AppDatabase.getDatabase(getApplication())
                .notificationDao()
                .clearAllNotifications()
        }
    }

    fun addTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.insertTransaction(transaction)
            checkAllBudgetsAcrossMonths()
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
            checkAllBudgetsAcrossMonths()
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
            checkAllBudgetsAcrossMonths()
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
            checkAllBudgetsAcrossMonths()
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
                checkAllBudgetsAcrossMonths()
                onComplete(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false)
            }
        }
    }

    // --- METAS & CAIXINHAS ---
    val allGoals: Flow<List<GoalEntity>> = AppDatabase.getDatabase(application)
        .goalDao()
        .getAllGoals()

    fun addGoal(name: String, targetAmount: Double, deadlineDate: Long? = null) {
        if (name.isBlank() || targetAmount <= 0.0) return
        viewModelScope.launch {
            AppDatabase.getDatabase(getApplication())
                .goalDao()
                .insertGoal(GoalEntity(name = name.trim(), targetAmount = targetAmount, deadlineDate = deadlineDate))
        }
    }

    fun updateGoal(goal: GoalEntity) {
        viewModelScope.launch {
            AppDatabase.getDatabase(getApplication())
                .goalDao()
                .updateGoal(goal)
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch {
            AppDatabase.getDatabase(getApplication())
                .goalDao()
                .deleteGoal(goal)
        }
    }

    fun depositToGoal(goal: GoalEntity, amount: Double) {
        if (amount <= 0.0) return
        viewModelScope.launch {
            val updated = goal.copy(currentAmount = goal.currentAmount + amount)
            AppDatabase.getDatabase(getApplication())
                .goalDao()
                .updateGoal(updated)
        }
    }

    fun withdrawFromGoal(goal: GoalEntity, amount: Double) {
        if (amount <= 0.0) return
        viewModelScope.launch {
            val newAmt = (goal.currentAmount - amount).coerceAtLeast(0.0)
            val updated = goal.copy(currentAmount = newAmt)
            AppDatabase.getDatabase(getApplication())
                .goalDao()
                .updateGoal(updated)
        }
    }

    // --- CONTAS FIXAS & VENCIMENTOS ---
    val allRecurringBills: Flow<List<RecurringBillEntity>> = AppDatabase.getDatabase(application)
        .recurringBillDao()
        .getAllRecurringBills()

    fun addRecurringBill(title: String, amount: Double, dueDay: Int, category: String, bank: String? = null) {
        if (title.isBlank() || amount <= 0.0) return
        viewModelScope.launch {
            AppDatabase.getDatabase(getApplication())
                .recurringBillDao()
                .insertRecurringBill(
                    RecurringBillEntity(
                        title = title.trim(),
                        amount = amount,
                        dueDay = dueDay.coerceIn(1, 31),
                        category = category.ifBlank { "Contas de casa" },
                        bank = bank
                    )
                )
            checkRecurringBillsDueSoon()
        }
    }

    fun updateRecurringBill(bill: RecurringBillEntity) {
        viewModelScope.launch {
            AppDatabase.getDatabase(getApplication())
                .recurringBillDao()
                .updateRecurringBill(bill)
            checkRecurringBillsDueSoon()
        }
    }

    fun deleteRecurringBill(bill: RecurringBillEntity) {
        viewModelScope.launch {
            AppDatabase.getDatabase(getApplication())
                .recurringBillDao()
                .deleteRecurringBill(bill)
        }
    }

    fun toggleBillPaidStatus(bill: RecurringBillEntity) {
        viewModelScope.launch {
            val updated = bill.copy(isPaidThisMonth = !bill.isPaidThisMonth)
            AppDatabase.getDatabase(getApplication())
                .recurringBillDao()
                .updateRecurringBill(updated)
            checkRecurringBillsDueSoon()
        }
    }

    fun checkRecurringBillsDueSoon() {
        viewModelScope.launch {
            val database = AppDatabase.getDatabase(getApplication())
            val bills = database.recurringBillDao().getAllRecurringBillsList()

            val calendar = Calendar.getInstance()
            val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)

            bills.filter { !it.isPaidThisMonth }.forEach { bill ->
                val daysUntilDue = bill.dueDay - currentDay

                // Notifica se faltam 3 dias, 1 dia ou se vence hoje
                if (daysUntilDue in 0..3) {
                    val prefKey = "notified_bill_${bill.id}_${currentYear}_${currentMonth}_${bill.dueDay}"
                    val alreadyNotified = prefs.getBoolean(prefKey, false)

                    if (!alreadyNotified) {
                        prefs.edit().putBoolean(prefKey, true).apply()
                        sendBillNotification(bill.title, bill.amount, daysUntilDue, bill.dueDay)
                    }
                }
            }
        }
    }

    private fun sendBillNotification(title: String, amount: Double, daysUntilDue: Int, dueDay: Int) {
        val context = getApplication<Application>()
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("pt", "BR"))
        val formattedAmount = currencyFormat.format(amount)

        val notifTitle = when (daysUntilDue) {
            0 -> "🚨 Vence Hoje: $title!"
            1 -> "⚠️ Vence Amanhã: $title!"
            else -> "⚠️ Conta Próxima do Vencimento: $title"
        }

        val notifText = "A conta no valor de $formattedAmount vence no dia $dueDay."

        val builder = NotificationCompat.Builder(context, "budget_channel")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(notifTitle)
            .setContentText(notifText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notifText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(context)) {
                notify("bill-${title}-${dueDay}".hashCode(), builder.build())
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }

        // Salva na Central de Alertas
        viewModelScope.launch {
            val entity = NotificationEntity(
                title = notifTitle,
                message = notifText,
                timestamp = System.currentTimeMillis(),
                isRead = false,
                tier = if (daysUntilDue == 0) 100 else 80,
                monthName = "VENCIMENTO DIA $dueDay"
            )
            AppDatabase.getDatabase(getApplication())
                .notificationDao()
                .insertNotification(entity)
        }
    }

    // --- PAINEL DE PARCELADOS ---
    val allInstallmentSummaries: Flow<List<InstallmentGroupSummary>> = AppDatabase.getDatabase(getApplication())
        .transactionDao()
        .getAllTransactions()
        .map { list ->
            val installmentTx = list.filter { it.isInstallment || !it.installmentGroupId.isNullOrBlank() }
            val grouped = installmentTx.groupBy {
                if (!it.installmentGroupId.isNullOrBlank()) {
                    it.installmentGroupId
                } else {
                    "${it.title}_${it.amount}_${it.totalInstallments}"
                }
            }

            val nowMs = System.currentTimeMillis()

            grouped.map { (groupId, txList) ->
                val sortedTx = txList.sortedBy { it.currentInstallment }
                val sample = sortedTx.first()
                val totalInst = sample.totalInstallments
                val instAmount = sample.amount
                val totalOriginal = instAmount * totalInst

                val paidTx = sortedTx.filter { it.date <= nowMs }
                val remainingTx = sortedTx.filter { it.date > nowMs }

                val paidCount = paidTx.size.coerceAtMost(totalInst)
                val remainingCount = (totalInst - paidCount).coerceAtLeast(0)

                val paidAmt = paidTx.sumOf { it.amount }
                val remainingAmt = remainingTx.sumOf { it.amount }

                InstallmentGroupSummary(
                    groupId = groupId ?: "",
                    title = sample.title,
                    category = sample.category,
                    bank = sample.bank,
                    paymentMethod = sample.paymentMethod,
                    totalInstallments = totalInst,
                    installmentAmount = instAmount,
                    totalOriginalAmount = totalOriginal,
                    paidAmount = paidAmt,
                    remainingAmount = remainingAmt,
                    paidInstallmentsCount = paidCount,
                    remainingInstallmentsCount = remainingCount,
                    installmentsList = sortedTx
                )
            }.sortedByDescending { it.remainingAmount }
        }
}

data class InstallmentGroupSummary(
    val groupId: String,
    val title: String,
    val category: String,
    val bank: String?,
    val paymentMethod: String,
    val totalInstallments: Int,
    val installmentAmount: Double,
    val totalOriginalAmount: Double,
    val paidAmount: Double,
    val remainingAmount: Double,
    val paidInstallmentsCount: Int,
    val remainingInstallmentsCount: Int,
    val installmentsList: List<TransactionEntity>
)
