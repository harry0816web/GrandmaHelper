package com.example.bubbleassistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_NOT_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bubbleassistant.ui.theme.BubbleAssistantTheme

class MainActivity : ComponentActivity() {

    // Compose 開關的共享狀態（讓廣播可更新 UI）
    private var bubbleOnState: MutableState<Boolean>? = null

    // 接收 BubbleService 廣播，把 UI 開關同步 ON/OFF
    private val stateReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            if (intent?.action == BubbleService.ACTION_BUBBLE_STATE) {
                val running = intent.getBooleanExtra(BubbleService.EXTRA_RUNNING, false)
                bubbleOnState?.value = running
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 監聽泡泡狀態
        registerReceiver(
            stateReceiver,
            IntentFilter(BubbleService.ACTION_BUBBLE_STATE),
            RECEIVER_NOT_EXPORTED
        )

        // 有權限就預設啟動泡泡（Switch 初值 = ON）
        val defaultOn = if (Settings.canDrawOverlays(this)) {
            startService(Intent(this, BubbleService::class.java))
            true
        } else false

        setContent {
            BubbleAssistantTheme {
                var bubbleOn by remember { mutableStateOf(defaultOn) }
                // 提供給廣播更新的引用
                bubbleOnState = remember { mutableStateOf(bubbleOn) }
                bubbleOn = bubbleOnState!!.value

                Scaffold(
                    topBar = { TopAppBar(title = { Text("設定") }) }
                ) { innerPadding ->
                    SettingsScreen(
                        modifier = Modifier.padding(innerPadding),
                        bubbleOn = bubbleOn,
                        onToggle = { checked ->
                            if (checked) {
                                // 要開啟：檢查懸浮窗權限
                                if (!Settings.canDrawOverlays(this)) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:$packageName")
                                    )
                                    startActivity(intent) // 回來後在 onResume 再判斷
                                } else {
                                    startService(Intent(this, BubbleService::class.java))
                                    bubbleOnState?.value = true
                                }
                            } else {
                                // 關閉泡泡
                                stopService(Intent(this, BubbleService::class.java))
                                bubbleOnState?.value = false
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 從權限頁返回：若同意且開關目前是 ON，啟動服務；若不同意，把開關打回 OFF
        bubbleOnState?.let { state ->
            if (state.value) {
                if (Settings.canDrawOverlays(this)) {
                    startService(Intent(this, BubbleService::class.java))
                } else {
                    state.value = false
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(stateReceiver) } catch (_: Exception) {}
    }
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    bubbleOn: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Bubble Assistant")
            Switch(checked = bubbleOn, onCheckedChange = onToggle)
        }
    }
}
