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
            val adjustedTransaction = applyCreditCardClosingRule(transaction)
            transactionDao.insertTransaction(adjustedTransaction)
        }
    }

    // Função para excluir uma transação
    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactions(transactions: List<TransactionEntity>) {
        transactionDao.deleteTransactions(transactions)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.updateTransaction(transaction)
    }

    // Exclui todas as parcelas de um grupo caso a compra seja parcelada
    suspend fun deleteTransactionGroup(transaction: TransactionEntity) {
        if (!transaction.installmentGroupId.isNullOrBlank()) {
            transactionDao.deleteTransactionsByGroupId(transaction.installmentGroupId!!)
        } else {
            transactionDao.deleteTransaction(transaction)
        }
    }

    // Regra do Cartão de Crédito: Fechamento no dia 4
    // Se a compra for feita no dia 4 ou depois, vai para o mês seguinte
    private fun applyCreditCardClosingRule(transaction: TransactionEntity): TransactionEntity {
        if (transaction.paymentMethod == "CREDIT") {
            val calendar = Calendar.getInstance().apply { timeInMillis = transaction.date }
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            if (day >= 4) {
                calendar.add(Calendar.MONTH, 1)
                return transaction.copy(date = calendar.timeInMillis)
            }
        }
        return transaction
    }

    // Lógica para gerar as parcelas automaticamente mês a mês dividindo o valor total pelo número de parcelas
    private suspend fun insertInstallmentTransactions(baseTransaction: TransactionEntity) {
        val groupId = UUID.randomUUID().toString()
        val calendar = Calendar.getInstance().apply { timeInMillis = baseTransaction.date }

        // Aplica a regra do dia 4 na primeira parcela se for cartão de crédito
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        if (baseTransaction.paymentMethod == "CREDIT" && day >= 4) {
            calendar.add(Calendar.MONTH, 1)
        }

        val transactions = mutableListOf<TransactionEntity>()

        // Divide o valor total digitado pelo número de parcelas
        val totalInst = baseTransaction.totalInstallments
        val rawInstallmentAmount = baseTransaction.amount / totalInst
        val baseInstallmentAmount = kotlin.math.round(rawInstallmentAmount * 100.0) / 100.0
        val remainder = kotlin.math.round((baseTransaction.amount - (baseInstallmentAmount * totalInst)) * 100.0) / 100.0

        for (i in 1..totalInst) {
            val installmentDate = calendar.timeInMillis
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

            // Avança 1 mês para a próxima parcela
            calendar.add(Calendar.MONTH, 1)
        }

        transactionDao.insertTransactions(transactions)
    }
}
