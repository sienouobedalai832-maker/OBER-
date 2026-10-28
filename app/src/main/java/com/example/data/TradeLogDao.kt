package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for the trade_logs Room database table.
 */
@Dao
interface TradeLogDao {

    @Query("SELECT * FROM trade_logs ORDER BY date DESC")
    fun getAllTradeLogs(): Flow<List<TradeLog>>

    @Query("SELECT * FROM trade_logs WHERE id = :id")
    suspend fun getTradeLogById(id: Long): TradeLog?

    @Query("SELECT * FROM trade_logs WHERE asset_symbol = :symbol ORDER BY date DESC")
    fun getTradeLogsByAsset(symbol: String): Flow<List<TradeLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTradeLog(tradeLog: TradeLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTradeLogs(tradeLogs: List<TradeLog>): List<Long>

    @Update
    suspend fun updateTradeLog(tradeLog: TradeLog)

    @Delete
    suspend fun deleteTradeLog(tradeLog: TradeLog)

    @Query("DELETE FROM trade_logs WHERE id = :id")
    suspend fun deleteTradeLogById(id: Long): Int

    @Query("DELETE FROM trade_logs")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM trade_logs")
    fun getTotalTradeCount(): Flow<Int>

    @Query("SELECT SUM(profit_loss) FROM trade_logs")
    fun getTotalProfitLoss(): Flow<Double?>

    @Query("SELECT COUNT(*) FROM trade_logs WHERE profit_loss > 0")
    fun getWinCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM trade_logs WHERE profit_loss < 0")
    fun getLossCount(): Flow<Int>

    @Query("SELECT MAX(profit_loss) FROM trade_logs")
    fun getMaxProfit(): Flow<Double?>

    @Query("SELECT MIN(profit_loss) FROM trade_logs")
    fun getMaxLoss(): Flow<Double?>
}
