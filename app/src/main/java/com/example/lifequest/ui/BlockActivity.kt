package com.example.lifequest.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

class BlockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black), // 没入感を阻害しない黒背景
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "集中モード中！",
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "このアプリは許可されていません。",
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(onClick = {
                        // ホームに戻る（緊急回避）
                        finish()
                        // moveTaskToBack(true) でも可
                    }) {
                        Text("集中に戻る")
                    }
                }
            }
        }
    }

    // 戻るボタン無効化
    override fun onBackPressed() {
        // Super call removed to disable back press
    }
}