package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.TradeLog
import com.example.data.TradeLogDao
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TradeLogRoomTest {

  private lateinit var database: AppDatabase
  private lateinit var tradeLogDao: TradeLogDao

  @Before
  fun createDb() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    tradeLogDao = database.tradeLogDao()
  }

  @After
  fun closeDb() {
    database.close()
  }

  @Test
  fun testProfitLossCalculation() {
    // Buy trade (Long): Entry 1.0800, Exit 1.0900 -> +0.0100 * 1000 = +10.0
    val buyPL = TradeLog.calculateProfitLoss(
      direction = "Achat",
      entryPrice = 1.0800,
      exitPrice = 1.0900,
      quantity = 1000.0
    )
    assertEquals(10.0, buyPL, 0.0001)

    // Sell trade (Short): Entry 2350.0, Exit 2340.0 -> (2350 - 2340) * 1 = +10.0
    val sellPL = TradeLog.calculateProfitLoss(
      direction = "Vente",
      entryPrice = 2350.0,
      exitPrice = 2340.0,
      quantity = 1.0
    )
    assertEquals(10.0, sellPL, 0.0001)

    // Losing Buy trade: Entry 100, Exit 95 -> -5.0
    val losingPL = TradeLog.calculateProfitLoss(
      direction = "Achat",
      entryPrice = 100.0,
      exitPrice = 95.0,
      quantity = 1.0
    )
    assertEquals(-5.0, losingPL, 0.0001)
  }

  @Test
  fun testInsertAndRetrieveTradeLog() = runBlocking {
    val trade = TradeLog(
      assetSymbol = "EURUSD",
      direction = "Achat",
      entryPrice = 1.0820,
      exitPrice = 1.0890,
      quantity = 1.0,
      stopLoss = 1.0790,
      takeProfit = 1.0910,
      profitLoss = 70.0,
      strategy = "Order Block SMC",
      notes = "Rebond sur FVG 15M"
    )

    val id = tradeLogDao.insertTradeLog(trade)
    assertTrue(id > 0)

    val allLogs = tradeLogDao.getAllTradeLogs().first()
    assertEquals(1, allLogs.size)

    val saved = allLogs[0]
    assertEquals("EURUSD", saved.assetSymbol)
    assertEquals("Achat", saved.direction)
    assertEquals(70.0, saved.profitLoss, 0.001)
    assertTrue(saved.isWin)

    val byId = tradeLogDao.getTradeLogById(id)
    assertNotNull(byId)
    assertEquals("EURUSD", byId?.assetSymbol)
  }

  @Test
  fun testAggregationQueries() = runBlocking {
    val trade1 = TradeLog(
      assetSymbol = "EURUSD",
      direction = "Achat",
      entryPrice = 1.0800,
      exitPrice = 1.0900,
      profitLoss = 100.0
    )
    val trade2 = TradeLog(
      assetSymbol = "BTCUSD",
      direction = "Vente",
      entryPrice = 64000.0,
      exitPrice = 65000.0,
      profitLoss = -40.0
    )

    tradeLogDao.insertTradeLogs(listOf(trade1, trade2))

    val count = tradeLogDao.getTotalTradeCount().first()
    assertEquals(2, count)

    val totalPL = tradeLogDao.getTotalProfitLoss().first()
    assertEquals(60.0, totalPL ?: 0.0, 0.001)

    val wins = tradeLogDao.getWinCount().first()
    assertEquals(1, wins)

    val losses = tradeLogDao.getLossCount().first()
    assertEquals(1, losses)
  }

  @Test
  fun testDeleteTradeLog() = runBlocking {
    val trade = TradeLog(
      assetSymbol = "XAUUSD",
      direction = "Vente",
      entryPrice = 2345.0,
      exitPrice = 2338.0,
      profitLoss = 45.0
    )
    val id = tradeLogDao.insertTradeLog(trade)

    val deletedRows = tradeLogDao.deleteTradeLogById(id)
    assertEquals(1, deletedRows)

    val remaining = tradeLogDao.getAllTradeLogs().first()
    assertEquals(0, remaining.size)
  }
}
