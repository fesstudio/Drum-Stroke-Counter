package com.drummer.speed.domain.repository

import com.drummer.speed.data.model.SessionResult
import kotlinx.coroutines.flow.Flow

interface IHistoryRepository {
    fun getAllHistory(): Flow<List<SessionResult>>
    suspend fun insertResult(result: SessionResult)
    suspend fun deleteResult(result: SessionResult)
    suspend fun deleteResults(ids: List<String>)
    suspend fun clearHistory()
}
