package com.example.lifequest.ui.theme

import androidx.compose.ui.graphics.Color

//val Purple80 = Color(0xFFD0BCFF)
//val PurpleGrey80 = Color(0xFFCCC2DC)
//val Pink80 = Color(0xFFEFB8C8)
//
//val Purple40 = Color(0xFF6650a4)
//val PurpleGrey40 = Color(0xFF625b71)
//val Pink40 = Color(0xFF7D5260)

// --- Hybrid Water Todo Color System (Base) ---


// --- Tech Noir Spec (LIVENESS) ---

// Background: アプリ基底。漆黒の空間。
val TechNoirBackground = Color(0xFF0A0E14)

// Primary: エレクトリック・シアン。FAB、完了ボタン、アクティブ状態。
val TechNoirPrimary = Color(0xFF40E0FF)
val TechNoirOnPrimary = Color(0xFF0A0E14) // 黒文字
val TechNoirPrimaryContainer = Color(0xFF0F2C35) // 暗いシアンのコンテナ
val TechNoirOnPrimaryContainer = Color(0xFF40E0FF)

// Surface (Glass): カード背景。Blurを想定した半透明色。
// 実際のガラス効果はModifierで行うが、ベース色として定義。
val TechNoirSurface = Color(0xFF1C232D).copy(alpha = 0.7f)
val TechNoirOnSurface = Color(0xFFE9EEF5) // オフホワイト

// Secondary: 補助情報
val TechNoirSecondary = Color(0xFF253041)
val TechNoirOnSecondary = Color(0xFF8A9AB5) // 少し青みがかったグレー
val TechNoirSecondaryContainer = Color(0xFF1A2633)
val TechNoirOnSecondaryContainer = Color(0xFF40E0FF)

// Tertiary: アクセント/システム通知
val TechNoirTertiary = Color(0xFF00F5D4) // ネオングリーンに近いシアン
val TechNoirOnTertiary = Color(0xFF000000)
val TechNoirTertiaryContainer = Color(0xFF003E36)
val TechNoirOnTertiaryContainer = Color(0xFF00F5D4)

// Outline (Neon): 極細の境界線。
val TechNoirOutline = Color(0xFF40E0FF).copy(alpha = 0.5f) // 発光感を出すためPrimary系
val TechNoirOutlineVariant = Color(0xFF2D3B4E)

// Error / Accent: 警告、重要通知
val TechNoirError = Color(0xFFFF3D00)
val TechNoirOnError = Color(0xFF000000)
val TechNoirErrorContainer = Color(0xFF3E1000)
val TechNoirOnErrorContainer = Color(0xFFFF3D00)

// --- Quest Category Colors (Neon Style) ---
val QuestCategoryTask = Color(0xFFFF4081)    // Blue Neon
val QuestCategoryHealth = Color(0xFF00E676)  // Green Neon
val QuestCategoryLearn = Color(0xFFFFC400)   // Amber Neon
val QuestCategoryRelation = Color(0xFF2196F3)   // Pink Neon
val QuestCategorySocial = Color(0xFFE040FB)  // Purple Neon
val QuestCategoryOther = Color(0xFFB0BEC5)   // Grey Neon

// --- Bonus Mission Gradient ---
val BonusMissionGradientStart = Color(0xFFD500F9)
val BonusMissionGradientMiddle = Color(0xFF2979FF)
val BonusMissionGradientEnd = Color(0xFF00E5FF)
val BonusMissionStar = Color(0xFFFFD740)

// --- Daily Quest Colors (Tech Noir / Neon Style) ---
val DailyQuestWakeUp = Color(0xFFFF9100)   // Neon Orange
val DailyQuestBedTime = Color(0xFF536DFE)  // Neon Indigo
val DailyQuestFocus = Color(0xFFFF1744)    // Neon Red
val DailyQuestBalance = Color(0xFF00E5FF)  // Neon Cyan
val DailyQuestBonus = Color(0xFFFFEA00)    // Neon Yellow