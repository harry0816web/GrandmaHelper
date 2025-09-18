package com.example.bubbleassistant

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource

class TutorialActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TutorialScreen(onBack = { finish() })
        }
    }
}

data class TutorialPage(
    val title: String,
    val subtitle: String,
    val imageRes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity   // 取得目前的 Activity

    val pages = listOf(
        TutorialPage(
            "1.開啟泡泡",
            "點擊右上角的開關，啟動泡泡功能",
            R.drawable.tutorial_step1 // ← 放你的教學圖片
        ),
        TutorialPage(
            "2.點選泡泡",
            "輕觸畫面上的泡泡，開啟互動視窗",
            R.drawable.tutorial_step2
        ),
        TutorialPage(
            "3.輸入或選擇問題",
            "在輸入框輸入問題，或直接點擊上方的快速捷徑",
            R.drawable.tutorial_step3
        ),
        TutorialPage(
            "4.跟著教學做",
            "完成步驟就打勾，問題輕鬆解決！",
            R.drawable.tutorial_step4
        ),
        TutorialPage(
            "隱藏功能-每日早安圖！",
            "隨心輸入關鍵字，每天接收不重複",
            R.drawable.tutorial_step5
        )
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
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFE2F4F3))
            )
        },
        containerColor = Color(0xFFE2F4F3)
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 卡片顯示教學內容
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(6.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                ) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = pages[currentPage].title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.Black,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(start = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = pages[currentPage].subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(start = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(30.dp))
                    Image(
                        painter = painterResource(id = pages[currentPage].imageRes),
                        contentDescription = pages[currentPage].title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f),
                        contentScale = ContentScale.Fit
                    )
                    if (currentPage == 2) {
                        Spacer(modifier = Modifier.height(30.dp))
                        Text(
                            text = "*捷徑可以透過主頁修改",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier
                                .align(Alignment.Start)
                                .padding(start = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 底部控制區
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 左箭頭
                IconButton(
                    onClick = { if (currentPage > 0) currentPage-- },
                    enabled = currentPage > 0
                ) {
                    Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "上一頁")
                }

                // 頁碼指示器
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    pages.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .size(if (index == currentPage) 12.dp else 8.dp)
                                .background(
                                    if (index == currentPage) Color(0xFF42A09D) else Color.Gray,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                // 右箭頭
                IconButton(
                    onClick = { if (currentPage < pages.size - 1) currentPage++ },
                    enabled = currentPage < pages.size - 1
                ) {
                    Icon(Icons.Default.KeyboardArrowRight, contentDescription = "下一頁")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 如果是最後一頁，就顯示「開始操作」按鈕
            if (currentPage == pages.size - 1) {
                Button(
                    onClick = { activity?.finish() }, // 跟返回鍵一樣，直接關閉 TutorialActivity
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF42A09D)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("開始操作", color = Color.White)
                }
            }
        }
    }
}
