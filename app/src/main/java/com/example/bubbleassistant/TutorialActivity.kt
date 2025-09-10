package com.example.bubbleassistant

import android.content.Intent
import androidx.activity.ComponentActivity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class TutorialActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TutorialScreen(
                onBack = { finish() },
                onFinish = {
                    // 回到主頁
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(
    onBack: () -> Unit,
    onFinish: () -> Unit
) {
    val steps = listOf(
        "1. 打開泡泡的開關",
        "2. 點擊泡泡",
        "3. 輸入問題 (也可以點擊上方快速捷徑喔！)",
        "4. 根據步驟操作，完成點擊右上方打勾",
        "5. 問題解決！"
    )

    var currentPage by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("使用教學") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFE2F4F3),
                    titleContentColor = Color(0xFF000000)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 教學卡片
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(650.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = steps[currentPage],
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF000000)
                    )
                }
            }
            Spacer(modifier = Modifier.height(5.dp))
            // 頁碼 + 左右按鈕
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { if (currentPage > 0) currentPage-- },
                    enabled = currentPage > 0
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "上一頁")
                }

                // 頁碼指示器
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    if (index == currentPage) Color(0xFF42A09D) else Color.LightGray,
                                    shape = RoundedCornerShape(50)
                                )
                        )
                    }
                }

                IconButton(
                    onClick = { if (currentPage < steps.lastIndex) currentPage++ },
                    enabled = currentPage < steps.lastIndex
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "下一頁")
                }
            }

            if (currentPage == steps.lastIndex) {
                Button(
                    onClick = onFinish,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF42A09D))
                ) {
                    Text("開始操作")
                }
            } else {
                Spacer(modifier = Modifier.height(48.dp)) // 保持高度一致
            }
        }
    }
}
