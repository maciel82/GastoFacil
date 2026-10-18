package com.example.gastofacil.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,                // Ex: "Supermercado", "Salário", "Notebook"
    val storeLocation: String?,       // Ex: "Shopping Center Norte" (opcional para saídas)
    val amount: Double,               // Valor da transação (ou da parcela)
    val date: Long,                   // Timestamp da data da transação
    val type: String,                 // "EXPENSE" (Gasto) ou "INCOME" (Receita)
    val paymentMethod: String,        // "CREDIT", "DEBIT", "PIX", "MONEY"
    val bank: String? = null,         // "Nubank", "PicPay", "RecargaPay", "Outro"
    val category: String = "Geral",   // "Lazer", "Mercado", "Alimentação", etc.
    val categoryId: Long = 0L,        // ID da categoria relacionada
    val isInstallment: Boolean = false,// Se é uma compra parcelada
    val currentInstallment: Int = 1,  // Ex: 3 (se for a 3ª parcela)
    val totalInstallments: Int = 1,   // Ex: 10 (se for em 10x)
    val installmentGroupId: String? = null // Identificador único para o grupo de parcelas
)
