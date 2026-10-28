package com.example.data

import kotlinx.coroutines.flow.Flow

/**
 * Repository abstracting data access for trading logs from the Room DAO.
 */
class TradeLogRepository(private val tradeLogDao: TradeLogDao) {

    val allTradeLogs: Flow<List<TradeLog>> = tradeLogDao.getAllTradeLogs()
    val totalProfitLoss: Flow<Double?> = tradeLogDao.getTotalProfitLoss()
    val totalTradeCount: Flow<Int> = tradeLogDao.getTotalTradeCount()
    val winCount: Flow<Int> = tradeLogDao.getWinCount()
    val lossCount: Flow<Int> = tradeLogDao.getLossCount()

    fun getTradesByAsset(symbol: String): Flow<List<TradeLog>> {
        return tradeLogDao.getTradeLogsByAsset(symbol)
    }

    suspend fun getTradeById(id: Long): TradeLog? {
        return tradeLogDao.getTradeLogById(id)
    }

    suspend fun insertTrade(tradeLog: TradeLog): Long {
        return tradeLogDao.insertTradeLog(tradeLog)
    }

    suspend fun insertTrades(tradeLogs: List<TradeLog>): List<Long> {
        return tradeLogDao.insertTradeLogs(tradeLogs)
    }

    suspend fun updateTrade(tradeLog: TradeLog) {
        tradeLogDao.updateTradeLog(tradeLog)
    }

    suspend fun deleteTrade(tradeLog: TradeLog) {
        tradeLogDao.deleteTradeLog(tradeLog)
    }

    suspend fun deleteTradeById(id: Long): Boolean {
        return tradeLogDao.deleteTradeLogById(id) > 0
    }

    suspend fun clearAll() {
        tradeLogDao.clearAll()
    }
}
