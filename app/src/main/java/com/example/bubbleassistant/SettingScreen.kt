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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextField
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
            .background(Color(0xFFE2F4F3)) // 整體背景
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Grandma Helper",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.Black
        )

        // Bubble Assistant 開關
        SettingItemRow(
            title = "開啟詢問泡泡",
            checked = bubbleOn,
            onCheckedChange = onBubbleToggle
        )

        // 語音開關
        val context = LocalContext.current
        val ttsManager = remember { TextToSpeechManager.getInstance(context) }
        LaunchedEffect(voiceOn) {
            ttsManager.setVoiceEnabled(voiceOn)
        }
        SettingItemRow(
            title = "開啟語音模式",
            checked = voiceOn,
            onCheckedChange = {
                onVoiceToggle(it)
                ttsManager.setVoiceEnabled(it)
            }
        )

        // 常用功能
        SettingNavRow(title = "常用詢問設定", onClick = onNavigateFeatures)

        // 使用教學
        SettingNavRow(title = "App 使用教學", onClick = onNavigateTutorial)

        // 早安圖 - 可展開卡片
        var morningCardExpanded by remember { mutableStateOf(false) }
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
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
                    Text("每日一張早安圖", style = MaterialTheme.typography.titleMedium, color = Color.Black)
                    Icon(
                        imageVector = if (morningCardExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (morningCardExpanded) "收起" else "展開",
                        tint = Color(0xFF42A09D)
                    )
                }

                // 展開內容
                if (morningCardExpanded) {
                    Spacer(modifier = Modifier.height(16.dp))

                    var prompt by rememberSaveable { mutableStateOf("") }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .background(Color(0xFFE2F4F3)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("空白圖片", color = Color.Gray)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = prompt,
                                onValueChange = { prompt = it },
                                placeholder = { Text("想要生成什麼樣的早安圖") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 56.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White,
                                    focusedIndicatorColor = Color(0xFF42A09D),
                                    unfocusedIndicatorColor = Color.LightGray,
                                    cursorColor = Color(0xFF42A09D),
                                    focusedTextColor = Color.Black,
                                    unfocusedTextColor = Color.Black,
                                    focusedPlaceholderColor = Color.Gray,
                                    unfocusedPlaceholderColor = Color.Gray
                                )
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Button(
                                onClick = { /* TODO: 傳送 prompt */ },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF42A09D))
                            ) {
                                Text("傳送", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

// 可重複使用的設定開關項目
@Composable
fun SettingItemRow(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, shape = MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.Black)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF42A09D)
            )
        )
    }
}

// 可重複使用的導覽項目
@Composable
fun SettingNavRow(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(Color.White, shape = MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color.Black)
        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF42A09D))
    }
}
