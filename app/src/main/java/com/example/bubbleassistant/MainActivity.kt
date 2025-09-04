package com.example.bubbleassistant

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_NOT_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.provider.Settings.Secure
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.bubbleassistant.ui.theme.BubbleAssistantTheme
import android.widget.Toast

class MainActivity : ComponentActivity() {

    // Compose 開關的共享狀態（讓廣播可更新 UI）
    private var bubbleOnState: MutableState<Boolean>? = null
    private var accessibilityOnState: MutableState<Boolean>? = null

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
        // 自動導引：若尚未授權懸浮窗，直接提示並跳設定頁
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(
                this,
                "請在下一頁允許本 App 顯示在其他應用程式上 (懸浮窗)",
                Toast.LENGTH_LONG
            ).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
        }

        // 監聽泡泡狀態
        ContextCompat.registerReceiver(
            this,
            stateReceiver,
            IntentFilter(BubbleService.ACTION_BUBBLE_STATE),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )

        // 檢查無障礙服務是否啟用
        val isAccessibilityEnabled = isAccessibilityServiceEnabled()
        var onboardingInProgress = false
        var requestedOverlay = false
        var requestedAccessibility = false
        
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

                // 導覽提示對話框狀態
                var showOverlayGuide by remember { mutableStateOf(false) }
                var showAccessibilityGuide by remember { mutableStateOf(false) }

                // 首次進入或缺權限時觸發導引
                LaunchedEffect(Unit) {
                    if (!Settings.canDrawOverlays(this@MainActivity)) {
                        showOverlayGuide = true
                        onboardingInProgress = true
                    } else if (!isAccessibilityServiceEnabled()) {
                        showAccessibilityGuide = true
                        onboardingInProgress = true
                    }
                }

                Scaffold(
                    topBar = { TopAppBar(title = { Text("設定") }) }
                ) { innerPadding ->
                    SettingsScreen(
                        modifier = Modifier.padding(innerPadding),
                        bubbleOn = bubbleOn,
                        accessibilityOn = isAccessibilityEnabled,
                        onBubbleToggle = { checked ->
                            if (checked) {
                                // 要開啟：檢查懸浮窗權限
                                if (!Settings.canDrawOverlays(this)) {
                                    showOverlayGuide = true
                                } else {
                                    startService(Intent(this, BubbleService::class.java))
                                    bubbleOnState?.value = true
                                }
                            } else {
                                // 關閉泡泡
                                stopService(Intent(this, BubbleService::class.java))
                                bubbleOnState?.value = false
                            }
                        },
                        onAccessibilityToggle = { checked ->
                            if (checked) {
                                showAccessibilityGuide = true
                            }
                        }
                    )
                }

                if (showOverlayGuide) {
                    AlertDialog(
                        onDismissRequest = { showOverlayGuide = false },
                        title = { Text("需要開啟泡泡視窗權限") },
                        text = {
                            Text("請在接下來的畫面中允許本 app 顯示於其他應用程式上 (顯示在上層/懸浮窗)。")
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showOverlayGuide = false
                                requestedOverlay = true
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:$packageName")
                                )
                                startActivity(intent)
                            }) { Text("前往設定") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showOverlayGuide = false }) { Text("稍後") }
                        }
                    )
                }

                if (showAccessibilityGuide) {
                    AlertDialog(
                        onDismissRequest = { showAccessibilityGuide = false },
                        title = { Text("需要開啟協助工具服務") },
                        text = {
                            Text("請在接下來的畫面中找到『BubbleAssistant』並將其啟用，以允許螢幕內容分析。")
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showAccessibilityGuide = false
                                requestedAccessibility = true
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                startActivity(intent)
                            }) { Text("前往設定") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showAccessibilityGuide = false }) { Text("稍後") }
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
        
        // 更新無障礙服務狀態
        accessibilityOnState?.value = isAccessibilityServiceEnabled()

        // Onboarding 流程：依序引導開啟兩個權限
        if (Settings.canDrawOverlays(this) && !isAccessibilityServiceEnabled()) {
            Toast.makeText(this, "請在下一頁開啟『BubbleAssistant』的協助工具服務", Toast.LENGTH_LONG).show()
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(stateReceiver) } catch (_: Exception) {}
    }
    
    /**
     * 檢查無障礙服務是否已啟用
     */
    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityEnabled = try {
            Settings.Secure.getInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)
        } catch (e: Settings.SettingNotFoundException) {
            0
        }
        
        if (accessibilityEnabled == 1) {
            val service = "${packageName}/${ScreenMonitor::class.java.name}"
            val settingValue = try {
                Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            } catch (e: Exception) {
                null
            }
            return settingValue?.contains(service) == true
        }
        return false
    }
}

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    bubbleOn: Boolean,
    accessibilityOn: Boolean,
    onBubbleToggle: (Boolean) -> Unit,
    onAccessibilityToggle: (Boolean) -> Unit
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
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("螢幕監控服務")
            Switch(checked = accessibilityOn, onCheckedChange = onAccessibilityToggle)
        }
    }
}
