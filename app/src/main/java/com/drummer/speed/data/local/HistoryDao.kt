package com.drummer.speed.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.drummer.speed.data.model.SessionResult
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM practice_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<SessionResult>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: SessionResult)

    @Delete
    suspend fun deleteResult(result: SessionResult)

    @Query("DELETE FROM practice_history WHERE id IN (:ids)")
    suspend fun deleteResults(ids: List<String>)

    @Query("DELETE FROM practice_history")
    suspend fun clearHistory()
}
