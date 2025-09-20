// MainActivity.kt
package com.example.bubbleassistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

@Composable
fun BubbleAssistantTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}

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

        // ✅ 改掉 let，避免 it 解析問題
        val decorView = window?.decorView
        if (decorView != null) {
            decorView.systemUiVisibility =
                decorView.systemUiVisibility or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        }

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

        // === 建立早安圖 ViewModel，啟動時自動生成一次 ===
        val morningVm = MorningImageViewModel(applicationContext)

        setContent {
            BubbleAssistantTheme {
                // App 一進來自動生成一次（只跑一次）
                LaunchedEffect(Unit) { morningVm.generateOnce() }

                var bubbleOn by remember { mutableStateOf(defaultOn) }
                bubbleOnState = remember { mutableStateOf(bubbleOn) }
                bubbleOn = bubbleOnState!!.value

                var voiceOn by remember { mutableStateOf(false) } // 若未使用可之後移除

                val showAccessibilityGuide = remember { mutableStateOf(false) }
                accessibilityGuideState = showAccessibilityGuide

                LaunchedEffect(Unit) {
                    if (Settings.canDrawOverlays(this@MainActivity) && !isAccessibilityServiceEnabled()) {
                        showAccessibilityGuide.value = true
                    }
                }

                var selectedTab by remember { mutableStateOf(0) }

                Scaffold(
                    containerColor = Color(0xFFE2F4F3),
                    bottomBar = {
                        NavigationBar(containerColor = Color(0xFF42A09D)) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                label = { Text("設定") },
                                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = Color(0xFFE2F4F3),
                                    selectedIconColor = Color(0xFF42A09D),
                                    unselectedIconColor = Color.White,
                                    selectedTextColor = Color.White,
                                    unselectedTextColor = Color.White
                                )
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                label = { Text("早安圖") },
                                icon = { Icon(Icons.Default.Image, contentDescription = null) },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = Color(0xFFE2F4F3),
                                    selectedIconColor = Color(0xFF42A09D),
                                    unselectedIconColor = Color.White,
                                    selectedTextColor = Color.White,
                                    unselectedTextColor = Color.White
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    when (selectedTab) {
                        0 -> SettingsScreen( // ⚠️ 這個來自你的 SettingScreen.kt
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
                            },
                            voiceOn = voiceOn,
                            onVoiceToggle = { voiceOn = it },
                            onNavigateTutorial = {
                                startActivity(Intent(this@MainActivity, TutorialActivity::class.java))
                            },
                            onNavigateFeatures = {
                                startActivity(Intent(this@MainActivity, FeaturesActivity::class.java))
                            }
                        )
                        1 -> GreetingImageScreen( // 使用新的畫面（僅讀 VM 狀態）
                            vm = morningVm,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }

                val dialogTitle = "需要開啟協助工具服務"
                val dialogText =
                    "請在接下來的畫面中點選『已下載的應用程式』，並找到『BubbleAssistant』將其啟用，以允許螢幕內容分析"
                var hasSpokenAccessibilityGuide by remember { mutableStateOf(false) }

                LaunchedEffect(showAccessibilityGuide.value) {
                    if (showAccessibilityGuide.value && !hasSpokenAccessibilityGuide) {
                        ttsManager.speak("$dialogTitle. $dialogText")
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
                        title = { Text(text = dialogTitle, color = Color(0xFF000000)) },
                        text = { Text(text = dialogText, color = Color(0xFF000000)) },
                        containerColor = Color(0xFFE2F4F3),
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    ttsManager.stop()
                                    showAccessibilityGuide.value = false
                                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                    startActivity(intent)
                                }
                            ) { Text("前往設定", color = Color(0xFF42A09D)) }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    ttsManager.stop()
                                    showAccessibilityGuide.value = false
                                }
                            ) { Text("稍後", color = Color(0xFF42A09D)) }
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
