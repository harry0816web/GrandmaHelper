package com.example.bubbleassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack

class FeaturesActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FeaturesScreen(onBack = { finish() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturesScreen(
    onBack: () -> Unit
) {
    // 三個可編輯文字狀態
    var shortcut1Text by remember { mutableStateOf("Shortcut 1") }
    var shortcut2Text by remember { mutableStateOf("Shortcut 2") }
    var shortcut3Text by remember { mutableStateOf("Shortcut 3") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("常用功能") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Editable fields
            OutlinedTextField(
                value = shortcut1Text,
                onValueChange = { shortcut1Text = it },
                label = { Text("快捷鍵 1") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = shortcut2Text,
                onValueChange = { shortcut2Text = it },
                label = { Text("快捷鍵 2") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = shortcut3Text,
                onValueChange = { shortcut3Text = it },
                label = { Text("快捷鍵 3") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 儲存按鈕
            Button(
                onClick = {
                    // 這裡可以呼叫方法更新 dialog_chat.xml 的按鈕文字
                    // 或存入 SharedPreferences 再在 dialog_chat 中讀取
                    saveShortcuts(shortcut1Text, shortcut2Text, shortcut3Text)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("儲存")
            }
        }
    }
}

fun saveShortcuts(s1: String, s2: String, s3: String) {
    //TODO
}

