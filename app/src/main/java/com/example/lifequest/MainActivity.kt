package com.example.lifequest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.room.Room
import com.example.lifequest.ui.GameScreen
import com.example.lifequest.ui.theme.LifeQuestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // DB初期化
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "lifequest-db"
        ).fallbackToDestructiveMigration().build()

        // ViewModel初期化
        val viewModel = GameViewModel(db.userDao())

        // UI表示
        setContent {
            LifeQuestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 処理はすべてGameScreenにお任せ
                    GameScreen(viewModel)
                }
            }
        }
    }
}