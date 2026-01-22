package com.example.lifequest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.lifequest.data.local.AppDatabase
import com.example.lifequest.data.repository.MainRepository
import com.example.lifequest.ui.GameScreen
import com.example.lifequest.ui.theme.LifeQuestTheme
import com.example.lifequest.utils.UsageStatsHelper
import com.example.lifequest.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "lifequest-db"
        )
            .fallbackToDestructiveMigration()
            .build()

        val repository = MainRepository(db.userDao(), db.breakActivityDao(), db.dailyQuestDao())

        val usageStatsHelper = UsageStatsHelper(applicationContext)

        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                    @Suppress("UNCHECKED_CAST")
                    // ★修正: helperを渡す
                    return MainViewModel(repository, usageStatsHelper) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }

        val viewModel = ViewModelProvider(this, viewModelFactory)[MainViewModel::class.java]

        setContent {
            LifeQuestTheme {
                GameScreen(viewModel = viewModel)
            }
        }
    }
}