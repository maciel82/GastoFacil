package com.example.gastofacil.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SuggestedTransactionDao {
    @Query("SELECT * FROM suggested_transactions ORDER BY date DESC")
    fun getAllSuggestions(): Flow<List<SuggestedTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuggestion(suggestion: SuggestedTransactionEntity)

    @Delete
    suspend fun deleteSuggestion(suggestion: SuggestedTransactionEntity)
}
