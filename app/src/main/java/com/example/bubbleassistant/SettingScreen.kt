package com.example.bubbleassistant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Tune
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextField
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.Image
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import android.provider.MediaStore
import android.content.ContentValues
import android.os.Build
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

        // 早安圖 - 可展開卡片（已移除使用者上傳圖片，只顯示生成結果 + 可選 prompt）
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
                        .clickable { morningCardExpanded = !morningCardExpanded }
                ) {
                    Icon(
                        imageVector = Icons.Default.Image,
                        contentDescription = "早安圖 Icon",
                        tint = Color(0xFF42A09D),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "每日一張早安圖",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.Black,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (morningCardExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (morningCardExpanded) "收起" else "展開",
                        tint = Color(0xFF42A09D)
                    )
                }

                if (morningCardExpanded) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // 僅保留「結果顯示 + 可選 prompt + 生成 + 另存」
                    var prompt by rememberSaveable { mutableStateOf("") }
                    var resultBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
                    var isLoading by remember { mutableStateOf(false) }
                    var errorMsg by remember { mutableStateOf<String?>(null) }
                    val scope = rememberCoroutineScope()

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 結果顯示區（無上傳、不可點）
                        Box(
                            modifier = Modifier
                                .height(200.dp)
                                .fillMaxWidth()
                                .background(Color(0xFFE2F4F3)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (resultBitmap != null) {
                                Image(
                                    bitmap = resultBitmap!!.asImageBitmap(),
                                    contentDescription = "生成結果"
                                )
                            } else {
                                Text("尚未生成圖片", color = Color.Gray)
                            }

                            // 清除結果按鈕（若需要快速清空）
                            if (resultBitmap != null) {
                                IconButton(
                                    onClick = {
                                        resultBitmap = null
                                        errorMsg = null
                                    },
                                    modifier = Modifier.align(Alignment.TopStart)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "清除結果",
                                        tint = Color(0xFF333333)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = prompt,
                                onValueChange = { prompt = it },
                                placeholder = { Text("想要生成怎樣的早安圖") },
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
                                enabled = !isLoading,
                                onClick = {
                                    errorMsg = null
                                    scope.launch {
                                        isLoading = true
                                        try {
                                            val generated = generateMorningImage(context, prompt)
                                            resultBitmap = generated
                                        } catch (t: Throwable) {
                                            errorMsg = t.message ?: "生成失敗"
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF42A09D))
                            ) {
                                Text(if (isLoading) "生成中..." else "生成", color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 另存圖片
                        Button(
                            enabled = resultBitmap != null && !isLoading,
                            onClick = {
                                val bmp = resultBitmap ?: return@Button
                                scope.launch {
                                    val ok = saveBitmapToGallery(
                                        context = context,
                                        bitmap = bmp,
                                        displayName = "morning_image_${System.currentTimeMillis()}.png"
                                    )
                                    Toast.makeText(
                                        context,
                                        if (ok) "已另存到相簿/Pictures/GrandmaHelper" else "儲存失敗",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF42A09D))
                        ) {
                            Text("另存圖片", color = Color.White)
                        }

                        if (!errorMsg.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(errorMsg!!, color = Color.Red)
                        }
                    }
                }
            }
        }
    }
}
private suspend fun generateMorningImage(
    context: android.content.Context,
    prompt: String
): android.graphics.Bitmap = withContext(Dispatchers.IO) {
    val client = OkHttpClient.Builder().build()
    val multipart = MultipartBody.Builder().setType(MultipartBody.FORM)
        .addFormDataPart("prompt", if (prompt.isNotBlank()) prompt else "回傳一張圖片")
        .build()

    val request = Request.Builder()
        .url(GEMINI_IMAGE_API_URL)
        .post(multipart)
        .build()

    val bytes = client.newCall(request).execute().use { resp ->
        if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
        resp.body?.bytes() ?: throw IllegalStateException("空回應")
    }

    val outFile = File(context.cacheDir, "morning_${System.currentTimeMillis()}.png")
    FileOutputStream(outFile).use { it.write(bytes) }

    BitmapFactory.decodeFile(outFile.absolutePath) ?: throw IllegalStateException("無法解析回傳圖片")
}

private suspend fun saveBitmapToGallery(
    context: android.content.Context,
    bitmap: android.graphics.Bitmap,
    displayName: String
): Boolean = withContext(Dispatchers.IO) {
    try {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GrandmaHelper")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(collection, values) ?: return@withContext false
        resolver.openOutputStream(uri)?.use { out ->
            val ok = bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, out)
            if (!ok) return@withContext false
        } ?: return@withContext false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        true
    } catch (_: Throwable) {
        false
    }
}

private const val GEMINI_IMAGE_API_URL: String = "https://morning-image-api-855188038216.asia-east1.run.app/generate"

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
            modifier = Modifier.weight(1f) // 讓文字佔滿中間空間
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
            modifier = Modifier.weight(1f) // 填滿空間，讓箭頭靠右
        )

        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = Color(0xFF42A09D)
        )
    }
}
