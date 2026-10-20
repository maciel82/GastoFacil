package com.example.gastofacil.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val tier: Int,          // 70, 80, 90 ou 100
    val monthName: String   // Ex: "NOVEMBRO 2026"
)
