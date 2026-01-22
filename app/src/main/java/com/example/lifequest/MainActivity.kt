package com.example.lifequest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.lifequest.ui.GameScreen
import com.example.lifequest.ui.theme.LifeQuestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // データベースのインスタンス化
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "lifequest-db"
        )
            .fallbackToDestructiveMigration() // マイグレーションが必要な場合は適切に設定
            .build()

        // リポジトリのインスタンス化
        val repository = GameRepository(db.userDao(), db.breakActivityDao(),db.dailyQuestDao())

        // ViewModelのファクトリ作成
        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(GameViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    return GameViewModel(repository) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }

        // ViewModelの取得
        val viewModel = ViewModelProvider(this, viewModelFactory)[GameViewModel::class.java]

        setContent {
            LifeQuestTheme {
                GameScreen(viewModel = viewModel)
            }
        }
    }
}