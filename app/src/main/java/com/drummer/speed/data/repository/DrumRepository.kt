package com.drummer.speed.data.repository

import com.drummer.speed.data.local.HistoryDao
import com.drummer.speed.data.model.SessionResult
import com.drummer.speed.domain.repository.IHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DrumRepository @Inject constructor(
    private val historyDao: HistoryDao
) : IHistoryRepository {
    override fun getAllHistory(): Flow<List<SessionResult>> = historyDao.getAllHistory()

    override suspend fun insertResult(result: SessionResult) = historyDao.insertResult(result)

    override suspend fun deleteResult(result: SessionResult) = historyDao.deleteResult(result)

    override suspend fun deleteResults(ids: List<String>) = historyDao.deleteResults(ids)

    override suspend fun clearHistory() = historyDao.clearHistory()
}
