// MainActivity.kt
package com.example.bubbleassistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.bubbleassistant.ui.theme.BubbleAssistantTheme
import kotlinx.coroutines.flow.distinctUntilChanged

class MainActivity : ComponentActivity() {

    private var bubbleOnState: MutableState<Boolean>? = null
    private var accessibilityGuideState: MutableState<Boolean>? = null
    private lateinit var ttsManager: TextToSpeechManager

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
        ttsManager = TextToSpeechManager.getInstance(this)

        // 檢查懸浮窗權限
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "請允許本 App 能顯示在最上層", Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        // 註冊泡泡狀態廣播
        ContextCompat.registerReceiver(
            this,
            stateReceiver,
            IntentFilter(BubbleService.ACTION_BUBBLE_STATE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        val defaultOn = if (Settings.canDrawOverlays(this)) {
            startService(Intent(this, BubbleService::class.java))
            true
        } else false

        setContent {
            BubbleAssistantTheme {
                var bubbleOn by remember { mutableStateOf(defaultOn) }
                bubbleOnState = remember { mutableStateOf(bubbleOn) }
                bubbleOn = bubbleOnState!!.value

                val showAccessibilityGuide = remember { mutableStateOf(false) }
                accessibilityGuideState = showAccessibilityGuide

                LaunchedEffect(Unit) {
                    if (Settings.canDrawOverlays(this@MainActivity) && !isAccessibilityServiceEnabled()) {
                        showAccessibilityGuide.value = true
                    }
                }

                Scaffold(
                    topBar = { TopAppBar(title = { Text("設定") }) }
                ) { innerPadding ->
                    SettingsScreen(
                        modifier = Modifier.padding(innerPadding),
                        bubbleOn = bubbleOn,
                        onBubbleToggle = { checked ->
                            if (checked) {
                                if (!Settings.canDrawOverlays(this@MainActivity)) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "請先允許懸浮窗權限",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:$packageName")
                                    )
                                    startActivity(intent)
                                } else {
                                    startService(Intent(this@MainActivity, BubbleService::class.java))
                                    bubbleOnState?.value = true
                                    if (!isAccessibilityServiceEnabled()) {
                                        showAccessibilityGuide.value = true
                                    }
                                }
                            } else {
                                stopService(Intent(this@MainActivity, BubbleService::class.java))
                                bubbleOnState?.value = false
                            }
                        }
                    )
                }

                val dialogTitle = "需要開啟協助工具服務"
                val dialogText = "請在接下來的畫面中點選『已下載的應用程式』，並找到『BubbleAssistant』將其啟用，以允許螢幕內容分析"
                var hasSpokenAccessibilityGuide by remember { mutableStateOf(false) }

                // 改用 snapshotFlow 收集 state，避免只播一次
                LaunchedEffect(showAccessibilityGuide.value) {
                    if (showAccessibilityGuide.value && !hasSpokenAccessibilityGuide) {
                        if (!dialogTitle.contains("開啟協助工具服務")) {
                            ttsManager.speak("$dialogTitle. $dialogText")
                        }
                        hasSpokenAccessibilityGuide = true
                    } else if (!showAccessibilityGuide.value) {
                        ttsManager.stop()
                        hasSpokenAccessibilityGuide = false
                    }
                }

                if (showAccessibilityGuide.value) {
                    AlertDialog(
                        onDismissRequest = {
                            ttsManager.stop()
                            showAccessibilityGuide.value = false
                        },
                        title = { Text(dialogTitle) },
                        text = { Text(dialogText) },
                        confirmButton = {
                            TextButton(onClick = {
                                ttsManager.stop()
                                showAccessibilityGuide.value = false
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                startActivity(intent)
                            }) { Text("前往設定") }
                        },
                        dismissButton = {
                            TextButton(onClick = {
                                ttsManager.stop()
                                showAccessibilityGuide.value = false
                            }) { Text("稍後") }
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        bubbleOnState?.let { state ->
            if (state.value) {
                if (Settings.canDrawOverlays(this)) {
                    startService(Intent(this, BubbleService::class.java))
                } else {
                    state.value = false
                }
            }
        }
        if (Settings.canDrawOverlays(this) && !isAccessibilityServiceEnabled()) {
            accessibilityGuideState?.value = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(stateReceiver) } catch (_: Exception) {}
        stopService(Intent(this, BubbleService::class.java))
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityEnabled = try {
            Settings.Secure.getInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)
        } catch (_: Settings.SettingNotFoundException) {
            0
        }
        if (accessibilityEnabled == 1) {
            val service = "${packageName}/${ScreenMonitor::class.java.name}"
            val enabled = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            return enabled?.split(':')?.any { it.equals(service, ignoreCase = true) } == true
        }
        return false
    }
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    bubbleOn: Boolean,
    onBubbleToggle: (Boolean) -> Unit
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
            Switch(checked = bubbleOn, onCheckedChange = onBubbleToggle)
        }
    }
}
