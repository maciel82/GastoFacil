package com.example.gastofacil.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_bills")
data class RecurringBillEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,          // Ex: "Aluguel", "Internet", "Luz"
    val amount: Double,         // Valor mensal estimado
    val dueDay: Int,            // Dia do vencimento (1 a 31)
    val category: String = "Contas de casa",
    val bank: String? = null,
    val isPaidThisMonth: Boolean = false
)
