package com.drummer.speed.data.repository

import com.drummer.speed.data.local.HistoryDao
import com.drummer.speed.data.model.SessionResult
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DrumRepository @Inject constructor(
    private val historyDao: HistoryDao
) {
    fun getAllHistory(): Flow<List<SessionResult>> = historyDao.getAllHistory()

    suspend fun insertResult(result: SessionResult) = historyDao.insertResult(result)

    suspend fun deleteResult(result: SessionResult) = historyDao.deleteResult(result)

    suspend fun deleteResults(ids: List<String>) = historyDao.deleteResults(ids)

    suspend fun clearHistory() = historyDao.clearHistory()
}
