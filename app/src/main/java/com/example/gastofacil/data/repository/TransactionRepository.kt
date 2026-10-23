package com.example.gastofacil.data.repository

import com.example.gastofacil.data.local.TransactionDao
import com.example.gastofacil.data.local.TransactionEntity
import java.util.Calendar
import java.util.UUID

class TransactionRepository(private val transactionDao: TransactionDao) {

    // Função principal para inserir transação (lida com à vista e parcelados)
    suspend fun insertTransaction(transaction: TransactionEntity) {
        if (transaction.isInstallment && transaction.totalInstallments > 1) {
            insertInstallmentTransactions(transaction)
        } else {
            transactionDao.insertTransaction(transaction)
        }
    }

    // Função para excluir uma transação
    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactions(transactions: List<TransactionEntity>) {
        transactionDao.deleteTransactions(transactions)
    }

    suspend fun updateTransaction(transaction: TransactionEntity, newTotalAmount: Double? = null) {
        if (!transaction.installmentGroupId.isNullOrBlank()) {
            val allTx = transactionDao.getAllTransactionsList()
            val groupTx = allTx.filter { it.installmentGroupId == transaction.installmentGroupId }
            if (groupTx.isNotEmpty()) {
                val totalInst = transaction.totalInstallments
                val totalAmt = newTotalAmount ?: (transaction.amount * totalInst)
                val rawInstallmentAmount = totalAmt / totalInst
                val baseInstallmentAmount = kotlin.math.round(rawInstallmentAmount * 100.0) / 100.0
                val remainder = kotlin.math.round((totalAmt - (baseInstallmentAmount * totalInst)) * 100.0) / 100.0

                groupTx.forEach { item ->
                    val currentInst = item.currentInstallment
                    val currentAmount = if (currentInst == totalInst) baseInstallmentAmount + remainder else baseInstallmentAmount
                    val updatedItem = item.copy(
                        title = transaction.title,
                        storeLocation = transaction.storeLocation,
                        amount = currentAmount,
                        type = transaction.type,
                        paymentMethod = transaction.paymentMethod,
                        bank = transaction.bank,
                        category = transaction.category
                    )
                    transactionDao.updateTransaction(updatedItem)
                }
                return
            }
        }

        val finalAmount = if (transaction.isInstallment && transaction.totalInstallments > 1 && newTotalAmount != null) {
            val rawAmt = newTotalAmount / transaction.totalInstallments
            kotlin.math.round(rawAmt * 100.0) / 100.0
        } else {
            transaction.amount
        }
        transactionDao.updateTransaction(transaction.copy(amount = finalAmount))
    }

    // Exclui todas as parcelas de um grupo caso a compra seja parcelada
    suspend fun deleteTransactionGroup(transaction: TransactionEntity) {
        if (!transaction.installmentGroupId.isNullOrBlank()) {
            transactionDao.deleteTransactionsByGroupId(transaction.installmentGroupId!!)
        } else {
            transactionDao.deleteTransaction(transaction)
        }
    }

    // Lógica para gerar as parcelas automaticamente mês a mês dividindo o valor total pelo número de parcelas
    private suspend fun insertInstallmentTransactions(baseTransaction: TransactionEntity) {
        val groupId = UUID.randomUUID().toString()
        val originalCal = Calendar.getInstance().apply { timeInMillis = baseTransaction.date }
        val originalDay = originalCal.get(Calendar.DAY_OF_MONTH)

        val transactions = mutableListOf<TransactionEntity>()

        // Divide o valor total digitado pelo número de parcelas
        val totalInst = baseTransaction.totalInstallments
        val rawInstallmentAmount = baseTransaction.amount / totalInst
        val baseInstallmentAmount = kotlin.math.round(rawInstallmentAmount * 100.0) / 100.0
        val remainder = kotlin.math.round((baseTransaction.amount - (baseInstallmentAmount * totalInst)) * 100.0) / 100.0

        for (i in 1..totalInst) {
            val instCal = (originalCal.clone() as Calendar).apply {
                add(Calendar.MONTH, i - 1)
                val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
                set(Calendar.DAY_OF_MONTH, originalDay.coerceAtMost(maxDay))
            }
            val installmentDate = instCal.timeInMillis
            // Ajusta o centavo de diferença na última parcela se houver dízima
            val currentAmount = if (i == totalInst) baseInstallmentAmount + remainder else baseInstallmentAmount

            transactions.add(
                baseTransaction.copy(
                    id = 0,
                    amount = currentAmount, // Atribui o valor individual de cada parcela mensal
                    date = installmentDate,
                    currentInstallment = i,
                    totalInstallments = totalInst,
                    installmentGroupId = groupId
                )
            )
        }

        transactionDao.insertTransactions(transactions)
    }
}
