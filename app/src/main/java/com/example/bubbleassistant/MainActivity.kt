package com.example.bubbleassistant

import android.content.Intent
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
import com.example.bubbleassistant.ui.theme.BubbleAssistantTheme
import androidx.compose.ui.unit.dp
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text


class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 檢查是否有懸浮窗權限，若無則請求
        if (!Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, 1001)
        } else {
            // 啟動懸浮泡泡服務
            startService(Intent(this, BubbleService::class.java))
        }

        setContent {
            BubbleAssistantTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(title = { Text("Bubble Assistant") })
                    }
                ) { innerPadding ->
                    MainScreen(Modifier.padding(innerPadding))
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001) {
            if (Settings.canDrawOverlays(this)) {
                // 使用者已授權，啟動服務
                startService(Intent(this, BubbleService::class.java))
            }
        }
    }
}
@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "歡迎使用 Bubble Assistant!")
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "泡泡會自動顯示在畫面上。")
    }
}
