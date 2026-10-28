package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity representing an individual trade log record.
 * Contains core trading data: asset symbol, direction, entry price,
 * exit price, date, and profit/loss calculation.
 */
@Entity(tableName = "trade_logs")
data class TradeLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "asset_symbol")
    val assetSymbol: String,

    @ColumnInfo(name = "direction")
    val direction: String = "Achat", // "Achat" (BUY) or "Vente" (SELL)

    @ColumnInfo(name = "entry_price")
    val entryPrice: Double,

    @ColumnInfo(name = "exit_price")
    val exitPrice: Double,

    @ColumnInfo(name = "quantity")
    val quantity: Double = 1.0,

    @ColumnInfo(name = "stop_loss")
    val stopLoss: Double? = null,

    @ColumnInfo(name = "take_profit")
    val takeProfit: Double? = null,

    @ColumnInfo(name = "profit_loss")
    val profitLoss: Double,

    @ColumnInfo(name = "date")
    val date: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "formatted_date")
    val formattedDate: String = "",

    @ColumnInfo(name = "strategy")
    val strategy: String = "",

    @ColumnInfo(name = "chart_url")
    val chartUrl: String = "",

    @ColumnInfo(name = "notes")
    val notes: String = ""
) {
    /**
     * Checks if the trade was profitable.
     */
    val isWin: Boolean
        get() = profitLoss > 0.0

    /**
     * Checks if the trade ended in a loss.
     */
    val isLoss: Boolean
        get() = profitLoss < 0.0

    /**
     * Checks if the trade was break-even.
     */
    val isBreakEven: Boolean
        get() = profitLoss == 0.0

    companion object {
        /**
         * Calculates profit or loss based on entry, exit, direction, and position size.
         * For Long (Achat): (Exit - Entry) * Quantity
         * For Short (Vente): (Entry - Exit) * Quantity
         */
        fun calculateProfitLoss(
            direction: String,
            entryPrice: Double,
            exitPrice: Double,
            quantity: Double = 1.0
        ): Double {
            val isShort = direction.equals("Vente", ignoreCase = true) ||
                    direction.equals("SELL", ignoreCase = true) ||
                    direction.equals("Short", ignoreCase = true)
            return if (isShort) {
                (entryPrice - exitPrice) * quantity
            } else {
                (exitPrice - entryPrice) * quantity
            }
        }
    }
}
