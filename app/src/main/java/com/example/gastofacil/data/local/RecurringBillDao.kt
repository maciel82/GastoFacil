package com.example.gastofacil.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringBillDao {
    @Query("SELECT * FROM recurring_bills ORDER BY dueDay ASC")
    fun getAllRecurringBills(): Flow<List<RecurringBillEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringBill(bill: RecurringBillEntity)

    @Update
    suspend fun updateRecurringBill(bill: RecurringBillEntity)

    @Delete
    suspend fun deleteRecurringBill(bill: RecurringBillEntity)
}
