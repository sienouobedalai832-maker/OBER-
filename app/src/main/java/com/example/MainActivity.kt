package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import com.example.data.AppDatabase
import com.example.data.TradeLog
import com.example.data.TradeLogRepository
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.TradeNavyBg
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class MainActivity : ComponentActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    val database = AppDatabase.getDatabase(applicationContext)
    val repository = TradeLogRepository(database.tradeLogDao())

    setContent {
      MyApplicationTheme {
        TradeJournalApp(
          repository = repository,
          onFinishActivity = { finish() }
        )
      }
    }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun TradeJournalApp(
  repository: TradeLogRepository,
  onFinishActivity: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var webViewInstance by remember { mutableStateOf<WebView?>(null) }

  // Handle Android back button
  BackHandler {
    val wv = webViewInstance
    if (wv != null) {
      wv.evaluateJavascript(
        "(function(){ return (typeof window.handleAndroidBack === 'function') ? window.handleAndroidBack() : false; })();",
        ValueCallback<String> { result ->
          val handled = result != null && (result.equals("true", ignoreCase = true) || result.equals("\"true\"", ignoreCase = true))
          if (!handled) {
            onFinishActivity()
          }
        }
      )
    } else {
      onFinishActivity()
    }
  }

  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(TradeNavyBg)
      .testTag("trade_journal_scaffold"),
    contentWindowInsets = WindowInsets.safeDrawing,
    containerColor = TradeNavyBg
  ) { innerPadding ->
    AndroidView(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .testTag("trade_journal_webview"),
      factory = { ctx ->
        WebView(ctx).apply {
          layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
          )
          setBackgroundColor(android.graphics.Color.parseColor("#071126"))

          settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
          }

          webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
              if (url != null && !url.startsWith("file://")) {
                try {
                  val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                  }
                  ctx.startActivity(intent)
                  return true
                } catch (e: Exception) {
                  e.printStackTrace()
                }
              }
              return false
            }
          }

          webChromeClient = WebChromeClient()

          addJavascriptInterface(
            TradeJournalBridge(ctx, repository),
            "AndroidBridge"
          )

          loadUrl("file:///android_asset/tradejournal.html")
          webViewInstance = this
        }
      },
      update = {
        webViewInstance = it
      }
    )
  }

  DisposableEffect(Unit) {
    onDispose {
      webViewInstance?.destroy()
      webViewInstance = null
    }
  }
}

/**
 * JavaScript Interface bridge to provide native Android features:
 * - Room Database synchronization for trading logs
 * - Native CSV sharing via Intent.ACTION_SEND / FileProvider
 * - Native Android Toasts
 * - Safe external link launching
 */
class TradeJournalBridge(
  private val context: Context,
  private val repository: TradeLogRepository
) {

  private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private val mainHandler = Handler(Looper.getMainLooper())

  /**
   * Persists a trade log into Room Database.
   */
  @JavascriptInterface
  fun syncTradeToRoom(
    assetSymbol: String,
    direction: String,
    entryPrice: Double,
    exitPrice: Double,
    quantity: Double,
    stopLoss: Double,
    takeProfit: Double,
    profitLoss: Double,
    formattedDate: String,
    strategy: String,
    notes: String,
    chartUrl: String
  ) {
    coroutineScope.launch {
      try {
        val qty = if (quantity > 0.0) quantity else 1.0
        val pl = if (profitLoss != 0.0) {
          profitLoss
        } else {
          TradeLog.calculateProfitLoss(direction, entryPrice, exitPrice, qty)
        }

        val log = TradeLog(
          assetSymbol = assetSymbol,
          direction = direction,
          entryPrice = entryPrice,
          exitPrice = exitPrice,
          quantity = qty,
          stopLoss = if (stopLoss > 0.0) stopLoss else null,
          takeProfit = if (takeProfit > 0.0) takeProfit else null,
          profitLoss = pl,
          formattedDate = formattedDate,
          strategy = strategy,
          notes = notes,
          chartUrl = chartUrl
        )
        repository.insertTrade(log)
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  /**
   * Clears all trade logs from Room Database.
   */
  @JavascriptInterface
  fun clearRoomDatabase() {
    coroutineScope.launch {
      try {
        repository.clearAll()
      } catch (e: Exception) {
        e.printStackTrace()
      }
    }
  }

  @JavascriptInterface
  fun exportCsv(csvContent: String) {
    mainHandler.post {
      try {
        val cachePath = File(context.cacheDir, "exports")
        if (!cachePath.exists()) {
          cachePath.mkdirs()
        }
        val file = File(cachePath, "tradejournal_export_${System.currentTimeMillis()}.csv")
        FileOutputStream(file).use { out ->
          out.write(csvContent.toByteArray(Charsets.UTF_8))
        }

        val contentUri: Uri = FileProvider.getUriForFile(
          context,
          "${context.packageName}.fileprovider",
          file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
          type = "text/csv"
          putExtra(Intent.EXTRA_STREAM, contentUri)
          putExtra(Intent.EXTRA_SUBJECT, "TradeJournal Pro - Export des trades")
          putExtra(Intent.EXTRA_TEXT, "Voici l'export complet de votre journal de trading TradeJournal Pro.")
          addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Exporter les trades (CSV)").apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
      } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(
          context,
          "Erreur lors du partage du fichier CSV : ${e.localizedMessage}",
          Toast.LENGTH_LONG
        ).show()
      }
    }
  }

  @JavascriptInterface
  fun showToast(message: String) {
    mainHandler.post {
      Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
  }

  @JavascriptInterface
  fun openExternalUrl(url: String) {
    mainHandler.post {
      try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
          addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
      } catch (e: Exception) {
        Toast.makeText(context, "Impossible d'ouvrir le lien : $url", Toast.LENGTH_SHORT).show()
      }
    }
  }
}
