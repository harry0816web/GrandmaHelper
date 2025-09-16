package com.example.bubbleassistant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

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
            text = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF2C8C82), Color(0xFF6BC8C5))
                        ),
                        fontWeight = FontWeight.Bold,
                        shadow = Shadow(
                            color = Color(0x55000000),
                            offset = Offset(2f, 2f),
                            blurRadius = 4f
                        ),
                        fontSize = MaterialTheme.typography.headlineLarge.fontSize
                    )
                ) {
                    append("Grandma Helper")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp), // 與下方內容保持距離
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center
        )




        // Bubble Assistant 開關
        SettingItemRow(
            icon = Icons.Default.ChatBubble,
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
            icon = Icons.Default.KeyboardVoice,
            title = "開啟語音模式",
            checked = voiceOn,
            onCheckedChange = {
                onVoiceToggle(it)
                ttsManager.setVoiceEnabled(it)
            }
        )

        // 常用功能
        SettingNavRow(icon = Icons.Default.Tune, title = "常用詢問設定", onClick = onNavigateFeatures)

        // 使用教學
        SettingNavRow(icon = Icons.Default.Info, title = "App 使用教學", onClick = onNavigateTutorial)

        MorningCard()
    }
}

/** 可重複使用的設定開關項目 */
@Composable
fun SettingItemRow(
    icon: ImageVector,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, shape = MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF42A09D),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            color = Color.Black,
            modifier = Modifier.weight(1f)
        )

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

/** 可點擊導覽的設定列 */
@Composable
fun SettingNavRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(Color.White, shape = MaterialTheme.shapes.medium)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF42A09D),
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            color = Color.Black,
            modifier = Modifier.weight(1f)
        )

        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF42A09D)
        )
    }
}
