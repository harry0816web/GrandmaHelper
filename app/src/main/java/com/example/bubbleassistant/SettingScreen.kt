package com.example.bubbleassistant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    bubbleOn: Boolean,
    onBubbleToggle: (Boolean) -> Unit,
    voiceOn: Boolean,
    onVoiceToggle: (Boolean) -> Unit,
    onNavigateTutorial: () -> Unit,
    onNavigateFeatures: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Bubble Assistant 開關
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("開啟詢問泡泡")
            Switch(checked = bubbleOn, onCheckedChange = onBubbleToggle)
        }

        // 語音開關
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("開啟語音模式")
            Switch(checked = voiceOn, onCheckedChange = onVoiceToggle)
        }

        // 常用功能
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateFeatures() }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("常用詢問設定")
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
        }

        // 使用教學
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateTutorial() }
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("App 使用教學")
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = null)
        }

        // 早安圖 - 可展開卡片
        var morningCardExpanded by remember { mutableStateOf(false) }
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // 標題 Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { morningCardExpanded = !morningCardExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("每日一張早安圖", style = MaterialTheme.typography.titleMedium)
                    Icon(
                        imageVector = if (morningCardExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (morningCardExpanded) "收起" else "展開"
                    )
                }

                // 展開內容
                if (morningCardExpanded) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // 使用者輸入的早安圖描述
                    var prompt by rememberSaveable { mutableStateOf("") }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(200.dp)
                                .background(Color.LightGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("空白圖片")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 左：輸入框，右：傳送按鈕
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = prompt,
                                onValueChange = { prompt = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 56.dp),
                                placeholder = { Text("想要生成什麼樣的早安圖呢") },
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = {
                                    // TODO: 傳送 prompt 的邏輯（例如呼叫產圖 API）
                                    // e.g., onSendMorningImage(prompt)
                                }
                            ) {
                                Text("傳送")
                            }
                        }
                    }
                }
            }
        }
    }
}
