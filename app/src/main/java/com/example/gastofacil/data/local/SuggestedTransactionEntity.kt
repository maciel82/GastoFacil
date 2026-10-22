package com.example.gastofacil.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "suggested_transactions")
data class SuggestedTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,                // Ex: "Supermercado X" ou "Salário Recebido"
    val amount: Double,               // Valor extraído
    val type: String,                 // "EXPENSE" (Saída) ou "INCOME" (Entrada)
    val paymentMethod: String,        // "PIX", "DEBIT", "CREDIT"
    val bank: String?,                // "Nubank", "PicPay", "RecargaPay", etc.
    val category: String,             // Categoria sugerida
    val date: Long,                   // Timestamp da captura
    val source: String                // "SMS" ou "Notificação"
)
