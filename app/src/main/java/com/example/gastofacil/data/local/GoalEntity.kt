package com.example.gastofacil.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,              // Ex: "Reserva de Emergência", "Viagem"
    val targetAmount: Double,      // Meta em R$
    val currentAmount: Double = 0.0,// Valor acumulado atual
    val deadlineDate: Long? = null // Timestamp da data limite (opcional)
)
