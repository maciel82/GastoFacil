package com.example.gastofacil.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Insert
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Insert
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    // Retorna todas as transações de um determinado período (mês/ano) ordenadas por data
    @Query("SELECT * FROM transactions WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getTransactionsByDateRange(startDate: Long, endDate: Long): Flow<List<TransactionEntity>>

    // Retorna o somatório de gastos agrupados por forma de pagamento em um período
    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE' AND paymentMethod = :method AND date BETWEEN :startDate AND :endDate")
    fun getTotalByPaymentMethod(method: String, startDate: Long, endDate: Long): Flow<Double?>

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    // // Exclui todas as transações pertencentes a um mesmo grupo de parcelas
    @Query("DELETE FROM transactions WHERE installmentGroupId = :groupId")
    suspend fun deleteTransactionsByGroupId(groupId: String)

    @androidx.room.Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @androidx.room.Delete
    suspend fun deleteTransactions(transactions: List<TransactionEntity>)

    @androidx.room.Update
    suspend fun updateTransaction(transaction: TransactionEntity)
}